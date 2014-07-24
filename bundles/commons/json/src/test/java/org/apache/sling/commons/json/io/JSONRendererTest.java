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
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.util.DespacedRendering;
import org.apache.sling.commons.json.util.TestJSONObject;
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
    
    @Test
    public void testDefaultArrayOutput() throws JSONException {
        final JSONObject jo = new TestJSONObject();
        final String pp = renderer.prettyPrint(jo.getJSONArray("array"), renderer.options()); 
        final DespacedRendering r = new DespacedRendering("{array:" + pp + "}"); 
        r.expect("[true,_hello_,52,212]");
    }
    
    @Test
    public void testArraysPrettyPrint() throws JSONException {
        final JSONObject jo = new TestJSONObject();
        
        // Verify that we get an array for children, by re-parsing the output
        final String json = renderer.prettyPrint(jo, renderer.options().withArraysForChildren(true));
        final JSONObject copy = new JSONObject(json);
        final JSONArray a = copy.getJSONArray(JSONRenderer.Options.DEFAULT_CHILDREN_KEY);
        final String str = renderer.toString(a);
        final String expected = "[{\"__name__\":\"k0\",\"name\":\"k0\",\"this is\":\"k0\"},{\"__name__\":\"k1\",\"name\":\"k1\",\"this is\":\"k1\"}]";
        assertEquals(expected, str);
    }
    
    @Test
    public void testCustomNamesArraysPrettyPrint() throws JSONException {
        final JSONObject jo = new TestJSONObject();
        final JSONRenderer.Options opt = renderer.options();
        opt.withArraysForChildren(true).withIndent(2).withChildrenKey("KIDS").withChildNameKey("KID.NAME");
        final DespacedRendering r = new DespacedRendering(renderer.prettyPrint(jo, opt));
        r.expect(
                "-nl-_string_:_thisstring_,-nl-_int_:12,-nl-_long_:42,-nl-_boolean_:true,",
                "_array_:[-nl-true,-nl-_hello_,-nl-52,-nl-212-nl-]-nl-,",
                "-nl-_KIDS_:[-nl-{-nl-_KID.NAME_:_k0_,-nl-_name_:_k0_",
                "-nl-_thisis_:_k0_-nl-},-nl-{-nl-_KID.NAME_:_k1_,-nl-_name_:_k1_,-nl-_thisis_:_k1_-nl-}-nl-]"
                );
    }    
    
    @Test
    public void testIndentedArraysPrettyPrint() throws JSONException {
        final JSONObject jo = new TestJSONObject();
        final JSONRenderer.Options opt = renderer.options().withArraysForChildren(true).withIndent(2);
        final DespacedRendering r = new DespacedRendering(renderer.prettyPrint(jo, opt));
        r.expect(
                "-nl-_string_:_thisstring_,-nl-_int_:12,-nl-_long_:42,-nl-_boolean_:true,",
                "_array_:[-nl-true,-nl-_hello_,-nl-52,-nl-212-nl-]-nl-,",
                "-nl-___children___:[-nl-{-nl-___name___:_k0_,-nl-_name_:_k0_",
                "-nl-_thisis_:_k0_-nl-},-nl-{-nl-___name___:_k1_,-nl-_name_:_k1_,-nl-_thisis_:_k1_-nl-}-nl-]"
                );
    }    
}