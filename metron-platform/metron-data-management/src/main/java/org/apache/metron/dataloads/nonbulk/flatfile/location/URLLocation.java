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
package org.apache.metron.dataloads.nonbulk.flatfile.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class URLLocation implements RawLocation {

  @Override
  public Optional<List<String>> list(String loc) throws IOException {
    return Optional.of(Collections.emptyList());
  }

  @Override
  public boolean exists(String loc) throws IOException {
    return true;
  }

  @Override
  public boolean isDirectory(String loc) throws IOException {
    return false;
  }

  @Override
  public BufferedReader openReader(String loc) throws IOException {
    InputStream is = new URL(loc).openStream();
    if(loc.endsWith(".zip")) {
      is = new ZipInputStream(is);
    }
    else if(loc.endsWith(".gz")) {
      is = new GZIPInputStream(is);
    }
    return new BufferedReader(new InputStreamReader(is));
  }

  @Override
  public boolean match(String loc) {
    try {
      new URL(loc);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }
}
