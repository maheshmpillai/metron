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
package org.apache.metron.common.typosquat;

import java.util.HashSet;
import java.util.Set;

/**
 * Typos based on random hyphenation (e.g. am-azon.com vs amazon.com)
 */
public class HyphenationStrategy implements TyposquattingStrategy {
  @Override
  public Set<String> generateCandidates(String originalString) {
    Set<String> ret = new HashSet<>();
    for(int i = 1;i < originalString.length();++i) {
      ret.add(originalString.substring(0, i) + "-" + originalString.substring(i));
    }
    return ret;
  }

  @Override
  public String name() {
    return "Hyphenation";
  }

}
