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

package org.apache.sling.servlets.post;

import junit.framework.TestCase;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.apache.sling.servlets.post.JSONResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

public class JsonResponseTest extends TestCase {
    protected JSONResponse res;

    public void setUp() throws Exception {
        res = new JSONResponse();
        super.setUp();
    }

    public void testOnChange() throws Exception {
        res.onChange("modified", "argument1", "argument2");
        Object prop = res.getProperty("changes");
        JSONArray changes = assertInstanceOf(prop, JSONArray.class);
        assertEquals(1, changes.length());
        Object obj = changes.get(0);
        JSONObject change = assertInstanceOf(obj, JSONObject.class);
        assertEquals("modified", assertProperty(change, JSONResponse.PROP_TYPE, String.class));
        JSONArray arguments = assertProperty(change, JSONResponse.PROP_ARGUMENT, JSONArray.class);
        assertEquals(2, arguments.length());
    }

    public void testSetProperty() throws Exception {
        res.setProperty("prop", "value");
        assertProperty(res.getJson(), "prop", String.class);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testSetError() throws IOException, JSONException {
        String errMsg = "Dummy error";
        res.setError(new Error(errMsg));
        MockSlingHttpServletResponse resp = new MockSlingHttpServletResponse();
        res.send(resp, true);
        JSONObject json = res.getJson();
        JSONObject error = assertProperty(json, "error", JSONObject.class);
        assertProperty(error, "class", Error.class.getName());
        assertProperty(error, "message", errMsg);
    }

    public void testSend() throws Exception {
        res.onChange("modified", "argument1");
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        res.send(response, true);
        JSONObject result = new JSONObject(response.getOutput().toString());
        assertProperty(result, HtmlResponse.PN_STATUS_CODE, HttpServletResponse.SC_OK);
        assertEquals(JSONResponse.RESPONSE_CONTENT_TYPE, response.getContentType());
        assertEquals(JSONResponse.RESPONSE_CHARSET, response.getCharacterEncoding());
    }

    private static <T> T assertProperty(JSONObject obj, String key, Class<T> clazz) throws JSONException {
        assertTrue("JSON object does not have property " + key, obj.has(key));
        return assertInstanceOf(obj.get(key), clazz);
    }

    @SuppressWarnings({"unchecked"})
    private static <T> T assertProperty(JSONObject obj, String key, T expected) throws JSONException {
        T res = (T) assertProperty(obj, key, expected.getClass());
        assertEquals(expected, res);
        return res;
    }

    @SuppressWarnings({"unchecked"})
    private static <T> T assertInstanceOf(Object obj, Class<T> clazz) {
        try {
            return (T) obj;
        } catch (ClassCastException e) {
            TestCase.fail("Object is of unexpected type. Expected: " + clazz.getName() + ", actual: " + obj.getClass().getName());
            return null;
        }
    }

}
