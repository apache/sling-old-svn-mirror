/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.fscontentparser.impl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public final class TestUtils {
    
    private TestUtils() {
        // static methods only
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getDeep(Map<String, Object> map, String path) {
      String name = StringUtils.substringBefore(path, "/");
      Object object = map.get(name);
      if (object == null || !(object instanceof Map)) {
        return null;
      }
      String remainingPath = StringUtils.substringAfter(path, "/");
      Map<String, Object> childMap = (Map<String, Object>)object;
      if (StringUtils.isEmpty(remainingPath)) {
        return childMap;
      }
      else {
        return getDeep(childMap, remainingPath);
      }
    }
    
}
