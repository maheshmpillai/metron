/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.bolt;

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class BulkMessageWriterBoltTest extends BaseEnrichmentBoltTest {

  protected class MessageListMatcher extends ArgumentMatcher<List<JSONObject>> {

    private List<JSONObject> expectedMessageList;

    public MessageListMatcher(List<JSONObject> expectedMessageList) {
      this.expectedMessageList = expectedMessageList;
    }

    @Override
    public boolean matches(Object o) {
      List<JSONObject> actualMessageList = (List<JSONObject>) o;
      for(JSONObject message: actualMessageList) removeTimingFields(message);
      return expectedMessageList.equals(actualMessageList);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format("[%s]", expectedMessageList));
    }

  }

  /**
   * {
   * "field": "value",
   * "source.type": "test"
   * }
   */
  @Multiline
  private String sampleMessageString;

  private JSONObject sampleMessage;
  private List<String> messageList;
  private List<JSONObject> expectedMessageList;
  private List<Tuple> tupleList;

  @Before
  public void parseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    sampleMessage = (JSONObject) parser.parse(sampleMessageString);
    messageList = new ArrayList<>();
    expectedMessageList = new ArrayList<>();
    sampleMessage.put("field", "value1");
    messageList.add(sampleMessage.toJSONString());
    expectedMessageList.add((JSONObject)sampleMessage.clone());
    sampleMessage.put("field", "value2");
    messageList.add(sampleMessage.toJSONString());
    expectedMessageList.add((JSONObject)sampleMessage.clone());
    sampleMessage.put("field", "value3");
    messageList.add(sampleMessage.toJSONString());
    expectedMessageList.add((JSONObject)sampleMessage.clone());
    sampleMessage.put("field", "value4");
    messageList.add((sampleMessage.toJSONString()));
    expectedMessageList.add((JSONObject)sampleMessage.clone());
    sampleMessage.put("field", "value5");
    messageList.add(sampleMessage.toJSONString());
    expectedMessageList.add((JSONObject)sampleMessage.clone());
  }

  @Mock
  private BulkMessageWriter<JSONObject> bulkMessageWriter;

  @Test
  public void test() throws Exception {
    BulkMessageWriterBolt bulkMessageWriterBolt = new BulkMessageWriterBolt("zookeeperUrl").withBulkMessageWriter(bulkMessageWriter);
    bulkMessageWriterBolt.setCuratorFramework(client);
    bulkMessageWriterBolt.setTreeCache(cache);
    bulkMessageWriterBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(sampleSensorEnrichmentConfigPath));
    bulkMessageWriterBolt.declareOutputFields(declarer);
    verify(declarer, times(1)).declareStream(eq("error"), argThat(new FieldsMatcher("message")));
    Map stormConf = new HashMap();
    doThrow(new Exception()).when(bulkMessageWriter).init(eq(stormConf), any(WriterConfiguration.class));
    try {
      bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector);
      fail("A runtime exception should be thrown when bulkMessageWriter.init throws an exception");
    } catch(RuntimeException e) {}
    reset(bulkMessageWriter);
    bulkMessageWriterBolt.prepare(stormConf, topologyContext, outputCollector);
    verify(bulkMessageWriter, times(1)).init(eq(stormConf), any(WriterConfiguration.class));
    tupleList = new ArrayList<>();
    for(int i = 0; i < 4; i++) {
      when(tuple.getValueByField("message")).thenReturn(messageList.get(i));
      tupleList.add(tuple);
      bulkMessageWriterBolt.execute(tuple);
      verify(bulkMessageWriter, times(0)).write(eq(sensorType), any(WriterConfiguration.class), eq(tupleList), eq(expectedMessageList));
    }
    when(tuple.getValueByField("message")).thenReturn(messageList.get(4));
    tupleList.add(tuple);
    bulkMessageWriterBolt.execute(tuple);
    verify(bulkMessageWriter, times(1)).write(eq(sensorType), any(WriterConfiguration.class), eq(tupleList), argThat(new MessageListMatcher(expectedMessageList)));
    verify(outputCollector, times(5)).ack(tuple);
    reset(outputCollector);
    doThrow(new Exception()).when(bulkMessageWriter).write(eq(sensorType), any(WriterConfiguration.class), Matchers.anyListOf(Tuple.class), Matchers.anyListOf(JSONObject.class));
    when(tuple.getValueByField("message")).thenReturn(messageList.get(0));
    for(int i = 0; i < 5; i++) {
      bulkMessageWriterBolt.execute(tuple);
    }
    verify(outputCollector, times(5)).ack(tuple);
    verify(outputCollector, times(1)).emit(eq(Constants.ERROR_STREAM), any(Values.class));
    verify(outputCollector, times(1)).reportError(any(Throwable.class));
  }
}
