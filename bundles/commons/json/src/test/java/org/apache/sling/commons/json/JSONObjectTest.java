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
package org.apache.sling.commons.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * @since Apr 17, 2009 6:04:00 PM
 */
public class JSONObjectTest {

    private static final String KEY = "key";

    /**
     * See <a href="https://issues.apache.org/jira/browse/SLING-929">SLING-929</a>
     */
    @Test public void testAppend() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.append(KEY, "value1");
        obj.append(KEY, "value2");
        Object result = obj.get(KEY);
        assertTrue("Did not create an array", result instanceof JSONArray);
    }

    /**
     * See <a href="https://issues.apache.org/jira/browse/SLING-929">SLING-929</a>
     */
    @Test public void testFailAppend() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(KEY, "value1");
        try {
            obj.append(KEY, "value2");
            TestCase.fail("Accepted append() to a non-array property");
        } catch (JSONException ignore) {
            // this is expected
        }
    }

    @Test public void testNull() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(KEY, JSONObject.NULL);

        TestCase.assertTrue(obj.has(KEY));
        TestCase.assertTrue(obj.get(KEY).equals(null));
        TestCase.assertEquals("{\"" + KEY + "\":null}", obj.toString());
    }

    @Test public void testParseLong() throws JSONException {
        String jsonStr = "{\"longvalue\":\"13857270119014401\"}";
        JSONObject obj = new JSONObject(jsonStr);
        TestCase.assertEquals(13857270119014401L, obj.getLong("longvalue"));
    }

    @Test public void testSample() throws JSONException {
          Map<String,Object> map=new HashMap<String,Object>();
          map.put("abc", "123456");
          List<String> list = new ArrayList<String>();
          list.add("Admin");
          list.add("password");
          map.put("groups", list);
          JSONObject response=new JSONObject();
          response.put("key", map);
          assertNotNull(response.get("key"));
          assertEquals("123456", response.getJSONObject("key").get("abc"));
          assertEquals(list, response.getJSONObject("key").get("groups"));
    }
}
