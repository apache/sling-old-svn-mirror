/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.json.util;

import java.math.BigInteger;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;

/** JSONObject with typical test data, used
 *  for tests.
 */
public class TestJSONObject extends JSONObject {
    
    public TestJSONObject() throws JSONException {
        put("string", "this string");
        put("int", 12);
        put("long", 42L);
        put("boolean", true);
        put("float", 12.34f);
        put("double", 45.67d);
        
        final JSONString js = new JSONString() {
            public String toJSONString() {
                return "json.string here";
            }
            
        };
        put("JSONString", js);
        
        for(int i=0; i<2; i++) {
            final JSONObject k = new JSONObject();
            final String name = "k" + i;
            k.put("name", name);
            k.put("this is", name);
            put(name,  k);
        }
        
        final JSONArray a = new JSONArray();
        a.put(true).put("hello").put(52.0).put(new BigInteger("212"));
        put("array", a);
    }
}
