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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.json.util.DespacedRendering;
import org.junit.Before;
import org.junit.Test;

/** Test the String formatting functionality of JSONObject */
public class JSONWriterTest {
    private JSONWriter w;
    private StringWriter output;
    
    @Before
    public void setup() {
        output = new StringWriter();
        w = new JSONWriter(output);
    }
    
    private DespacedRendering write() throws JSONException {
        w.object();
        w.key("foo").value("bar");
        w.key("array");
        w.array().value(1).value("two").value(3.0).value(false).endArray();
        w.key("last").value("one");
        w.endObject();
        return new DespacedRendering(output.toString());
    }

    private DespacedRendering writeObject() throws JSONException {
        JSONArray arr = new JSONArray();
        arr.put(1).put("two").put(3.0).put(false);

        w.writeObject(
            new JSONObject()
                .put("foo", "bar")
                .put("array", arr)
        );

        return new DespacedRendering(output.toString());
    }
    
    @Test
    public void testSetTidy() {
        assertFalse(w.isTidy());
        w.setTidy(true);
        assertTrue(w.isTidy());
    }
    
    @Test
    public void testStandardWrite() throws JSONException {
        final DespacedRendering r = write();
        r.expect(
                "_foo_:_bar_", 
                "_array_:[1,_two_,3,false]");
    }

    @Test
    public void testStandardObjectWrite() throws JSONException {
        final DespacedRendering r = writeObject();
        r.expect(
                "_foo_:_bar_", 
                "_array_:[1,_two_,3,false]");
    }
    
    @Test
    public void testTidyWrite() throws JSONException {
        w.setTidy(true);
        final DespacedRendering r = write();
        r.expect(
                "-nl-_foo_:_bar_", 
                "-nl-_array_:[-nl-1,-nl-_two_,-nl-3,-nl-false-nl-]");
    }

    @Test
    public void testTidyObjectWrite() throws JSONException {
        w.setTidy(true);
        final DespacedRendering r = writeObject();
        r.expect(
                "-nl-_foo_:_bar_", 
                "-nl-_array_:[-nl-1,-nl-_two_,-nl-3,-nl-false-nl-]");
    }
    
    @Test
    public void testEmpty() throws JSONException {
        assertTrue(output.toString().length() == 0);
    }
    
    @Test(expected=JSONException.class)
    public void testMisplacedArray() throws JSONException {
        w.object().array();
    }
    
    @Test(expected=JSONException.class)
    public void testMisplacedKey() throws JSONException {
        w.array().key("foo");
    }
    
    @Test(expected=JSONException.class)
    public void testMisplacedEndObjectA() throws JSONException {
        w.object().endObject().endObject();
    }
    
    @Test(expected=JSONException.class)
    public void testMisplacedEndObjectB() throws JSONException {
        w.endObject();
    }
}