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
package org.apache.sling.commons.json.io;

import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.junit.Test;


/** Most of the JSONRenderer code is tested indirectly
 *  via existing JSONObject and JSONArray tests - this
 *  tests what's left */
public class JSONRendererTest {
    private final JSONRenderer renderer = new JSONRenderer();
    
    @Test
    public void testA() {
        assertEquals("12.34", renderer.doubleToString(12.34));
    }
    
    @Test
    public void testB() {
        assertEquals("123", renderer.doubleToString(123));
    }
    
    @Test
    public void testC() {
        assertEquals("null", renderer.doubleToString(Double.POSITIVE_INFINITY));
    }
    
    @Test
    public void testD() {
        assertEquals("null", renderer.doubleToString(Double.NEGATIVE_INFINITY));
    }
    
    @Test
    public void testE() {
        assertEquals("null", renderer.doubleToString(Double.NaN));
    }
    
    @Test(expected=JSONException.class)
    public void testF() throws JSONException {
        renderer.numberToString(null);
    }
    
    @Test
    public void testEmptyJSONArray() throws JSONException {
        assertEquals("[]", renderer.prettyPrint(new JSONArray(), renderer.options()));
    }
    
    @Test
    public void testSingleJSONArray() throws JSONException {
        final JSONArray ja = new JSONArray();
        ja.put(42);
        assertEquals("[42]", renderer.prettyPrint(ja, renderer.options()));
    }
    
}