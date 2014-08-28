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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.sling.commons.json.util.DespacedRendering;
import org.apache.sling.commons.json.util.TestJSONObject;
import org.junit.Before;
import org.junit.Test;

/** Test the String formatting functionality of JSONObject */
public class JSONObjectToStringTest {
    private JSONObject j;
    
    static class FailingWriter extends Writer {
        public void write(char[] cbuf, int off, int len) throws IOException {
            throw new IOException("write");
        }

        public void flush() throws IOException {
            throw new IOException("flush");
        }

        public void close() throws IOException {
            throw new IOException("close");
        }
    }
    
    @Before
    public void setup() throws JSONException {
        j = new TestJSONObject();
    }

    @Test
    public void testToString() throws JSONException {
        final DespacedRendering r = new DespacedRendering(j.toString());
        r.expect(
                "_long_:42", 
                "_string_:_thisstring_",
                "_int_:12",
                "_k0_:{",
                "_name_:_k0_",
                "_thisis_:_k0_",
                "_k1_:{",
                "_name_:_k1_",
                "_thisis_:_k1_",
                "_boolean_:true",
                "_JSONString_:json.stringhere",
                "_array_:[true,_hello_,52,212]",
                "_double_:45.67",
                "_float_:12.34"
                );
    }
    
    @Test
    public void testToStringWithIndents() throws JSONException {
        final DespacedRendering r = new DespacedRendering(j.toString(2));
        r.expect(
                "_long_:42,-nl-", 
                "_string_:_thisstring_,-nl-",
                "_int_:12,-nl-",
                "_k0_:{",
                "_name_:_k0_",
                "_thisis_:_k0_",
                "_k1_:{",
                "_name_:_k1_",
                "_thisis_:_k1_",
                "_boolean_:true",
                "_JSONString_:json.stringhere",
                "_array_:[-nl-true",
                "_double_:45.67",
                "_float_:12.34"
                );
    }
    
    @Test
    public void testToStringWithInitialIndent() throws JSONException {
        final DespacedRendering r = new DespacedRendering(j.toString(2, 3), "S_");
        r.expect(
                "{-nl-S_S_S_S_S_",
                "_long_:42,-nl-", 
                "_string_:_thisstring_,-nl-",
                "_int_:12,-nl-",
                "_k0_:{",
                "_name_:_k0_",
                "_thisis_:_k0_",
                "_k1_:{",
                "_name_:_k1_",
                "_thisis_:_k1_",
                "_array_:[-nl-S_S_S_S_S_S_S_true"
                );
    }
    
    @Test
    public void testToStringEmpty() throws JSONException {
        final DespacedRendering r = new DespacedRendering(new JSONObject().toString(2));
        r.assertExactMatch("{}");
    }
    
    @Test
    public void testToStringSingle() throws JSONException {
        j = new JSONObject();
        j.put("foo", "bar");
        final DespacedRendering r = new DespacedRendering(j.toString(2));
        r.assertExactMatch("{_foo_:_bar_}");
    }
    
    @Test
    public void testJSONStringException() throws JSONException {
        final JSONString js = new JSONString() {
            @Override
            public String toString() {
                return toJSONString();
            }
            public String toJSONString() {
                throw new UnsupportedOperationException("toJSONString");
            }
            
        };
        j.put("should.fail", js);
        
        // if any exception, toString returns null. 
        // Not sure why but that's what it is  
        assertNull("Expecting null output for toString()", j.toString());
        
        // but toString(s) rethrows...
        try {
            assertNull("Expecting null output for toString(2)", j.toString(2));
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException uso) {
            // as expected
        }
    }
    
    @Test
    public void testQuote() {
        final String inp = "<tag/></thing>\\\\0\"A/B\bT\tN\nF\fT\r\u0082";
        final String out = "\"<tag/><\\/thing>\\\\\\\\0\\\"A/B\\bT\\tN\\nF\\fT\\r\\u0082\"";
        assertEquals(out, JSONObject.quote(inp));
    }
    
    @Test
    public void testQuoteNull() {
        assertEquals("\"\"", JSONObject.quote(null));
        assertEquals("\"\"", JSONObject.quote(""));
    }
    
    @Test
    public void testToJsonArray() throws JSONException {
        final DespacedRendering r = new DespacedRendering("{array:" + j.toJSONArray(j.names()).toString() + "}");
        r.expect("_thisstring_","12","42","true","json.stringhere");
    }
    
    @Test
    public void testWriteJsonArray() throws JSONException {
        final JSONArray a = j.toJSONArray(j.names());
        final StringWriter w = new StringWriter();
        a.write(w);
        final DespacedRendering r = new DespacedRendering("{array:" + w.toString() + "}");
        r.expect("_thisstring_","12","42","true","json.stringhere");
    }
    
    @Test(expected=JSONException.class)
    public void testWriteJsonArrayException() throws JSONException {
        final JSONArray a = j.toJSONArray(j.names());
        a.write(new FailingWriter());
    }
    
    @Test
    public void testWrite() throws JSONException {
        final StringWriter w = new StringWriter();
        j.write(w);
        final DespacedRendering r = new DespacedRendering(w.toString());
        r.expect(
                "_long_:42", 
                "_string_:_thisstring_",
                "_int_:12",
                "_k0_:{",
                "_name_:_k0_",
                "_thisis_:_k0_",
                "_k1_:{",
                "_name_:_k1_",
                "_thisis_:_k1_",
                "_boolean_:true",
                "_JSONString_:json.stringhere",
                "_array_:[true,_hello_,52,212]",
                "_double_:45.67",
                "_float_:12.34"
                );
    }
    
    @Test(expected=JSONException.class)
    public void testWriteException() throws JSONException {
        j.write(new FailingWriter());
    }
}