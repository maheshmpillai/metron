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
package org.apache.metron.common.utils;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Values;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.Constants;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

public class ErrorUtils {
  private final static Logger LOGGER = LoggerFactory.getLogger(ErrorUtils.class);

  public enum RuntimeErrors {
    ILLEGAL_ARG(t -> new IllegalArgumentException(formatReason(t), t.getRight().orElse(null))),
    ILLEGAL_STATE(t -> new IllegalStateException(formatReason(t), t.getRight().orElse(null)));

    Function<Pair<String, Optional<Throwable>>, RuntimeException> func;

    RuntimeErrors(Function<Pair<String, Optional<Throwable>>, RuntimeException> func) {
      this.func = func;
    }

    /**
     * Throw runtime exception with "reason".
     *
     * @param reason Message to include in exception
     */
    public void throwRuntime(String reason) {
      throwRuntime(reason, Optional.empty());
    }

    /**
     * Throw runtime exception with format "reason + cause message + cause Throwable"
     *
     * @param reason Message to include in exception
     * @param t Wrapped exception
     */
    public void throwRuntime(String reason, Throwable t) {
      throwRuntime(reason, Optional.of(t));
    }

    /**
     * Throw runtime exception with format "reason + cause message + cause Throwable".
     * If the optional Throwable is empty/null, the exception will only include "reason".
     *
     * @param reason Message to include in exception
     * @param t Optional wrapped exception
     */
    public void throwRuntime(String reason, Optional<Throwable> t) {
      throw func.apply(Pair.of(reason, t));
    }

    private static String formatReason(Pair<String, Optional<Throwable>> p) {
      return formatReason(p.getLeft(), p.getRight());
    }

    private static String formatReason(String reason, Optional<Throwable> t) {
      if (t.isPresent()) {
        return format("%s - reason:%s", reason, t.get());
      } else {
        return format("%s", reason);
      }
    }
  }

  @SuppressWarnings("unchecked") // JSONObject extends HashMap w/o type parameters
  public static JSONObject generateErrorMessage(String message, Throwable t) {
    JSONObject error_message = new JSONObject();

		/*
     * Save full stack trace in object.
		 */
    String stackTrace = ExceptionUtils.getStackTrace(t);

    String exception = t.toString();

    error_message.put("time", System.currentTimeMillis());
    try {
      error_message.put("hostname", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException ex) {
      LOGGER.info("Unable to resolve hostname while generating error message", ex);
    }

    error_message.put("message", message);
    error_message.put(Constants.SENSOR_TYPE, "error");
    error_message.put("exception", exception);
    error_message.put("stack", stackTrace);

    return error_message;
  }

  public static void handleError(OutputCollector collector, Throwable t, String errorStream) {
    JSONObject error = ErrorUtils.generateErrorMessage(t.getMessage(), t);
    collector.emit(errorStream, new Values(error));
    collector.reportError(t);
  }
}
