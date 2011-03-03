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
        MockHttpServletResponse resp = new MockHttpServletResponse();
        res.send(resp, true);
        JSONObject json = res.getJson();
        JSONObject error = assertProperty(json, "error", JSONObject.class);
        assertProperty(error, "class", Error.class.getName());
        assertProperty(error, "message", errMsg);
    }

    public void testSend() throws Exception {
        res.onChange("modified", "argument1");
        MockHttpServletResponse response = new MockHttpServletResponse();
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

    private static class MockHttpServletResponse implements HttpServletResponse {

        private StringBuffer output = new StringBuffer();
        private String contentType;
        private String encoding;
        private int status = SC_OK;

        public StringBuffer getOutput() {
            return output;
        }

        public void addCookie(Cookie cookie) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addCookie");
        }

        public boolean containsHeader(String s) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".containsHeader");
        }

        public String encodeURL(String s) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeURL");
        }

        public String encodeRedirectURL(String s) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeRedirectURL");
        }

        public String encodeUrl(String s) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeUrl");
        }

        public String encodeRedirectUrl(String s) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".encodeRedirectUrl");
        }

        public void sendError(int i, String s) throws IOException {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendError");
        }

        public void sendError(int i) throws IOException {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendError");
        }

        public void sendRedirect(String s) throws IOException {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".sendRedirect");
        }

        public void setDateHeader(String s, long l) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setDateHeader");
        }

        public void addDateHeader(String s, long l) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addDateHeader");
        }

        public void setHeader(String s, String s1) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setHeader");
        }

        public void addHeader(String s, String s1) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addHeader");
        }

        public void setIntHeader(String s, int i) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setIntHeader");
        }

        public void addIntHeader(String s, int i) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".addIntHeader");
        }

        public void setStatus(int i) {
            this.status = i;
        }

        public void setStatus(int i, String s) {
            this.status = i;
        }

        public String getCharacterEncoding() {
            return encoding;
        }

        public String getContentType() {
            return contentType;
        }

        public ServletOutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getOutputStream");
        }

        public PrintWriter getWriter() throws IOException {
            MockHttpServletResponse.MockWriter writer = new MockWriter(output);
            return new PrintWriter(writer);
        }

        public void setCharacterEncoding(String encoding) {
            this.encoding = encoding;
        }

        public void setContentLength(int i) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setContentLength");
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public void setBufferSize(int i) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setBufferSize");
        }

        public int getBufferSize() {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getBufferSize");
        }

        public void flushBuffer() throws IOException {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".flushBuffer");
        }

        public void resetBuffer() {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".resetBuffer");
        }

        public boolean isCommitted() {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".isCommitted");
        }

        public void reset() {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".reset");
        }

        public void setLocale(Locale locale) {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".setLocale");
        }

        public Locale getLocale() {
            throw new UnsupportedOperationException("Not implemented: " + getClass().getName() + ".getLocale");
        }

        private class MockWriter extends Writer {
            private StringBuffer buf;

            public MockWriter(StringBuffer output) {
                buf = output;
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                buf.setLength(0);
            }

            @Override
            public void close() throws IOException {
                buf = null;
            }
        }
    }

}
