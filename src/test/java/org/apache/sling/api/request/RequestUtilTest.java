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
package org.apache.sling.api.request;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

import junit.framework.TestCase;

public class RequestUtilTest extends TestCase {


    public void testHandleIfModifiedSince(){
        assertTrue(RequestUtil.handleIfModifiedSince(getMockRequest(1309268989938L,1309269042730L),getMockResponse()));

        assertFalse(RequestUtil.handleIfModifiedSince(getMockRequest(1309269042730L,1309268989938L),getMockResponse()));
        assertFalse(RequestUtil.handleIfModifiedSince(getMockRequest(-1,1309268989938L),getMockResponse()));
    }

    protected SlingHttpServletRequest getMockRequest(final long modificationTime, final long ifModifiedSince) {
        final String resourcePath = "foo";
        final MockSlingHttpServletRequest r = new MockSlingHttpServletRequest(resourcePath, null, null, null, null) {
            @Override
            public long getDateHeader(String name) {
                return ifModifiedSince;
            }

        };
        final String path = "/foo/node";
        final MockResource mr = new MockResource(null, path, null) {};
        mr.getResourceMetadata().setModificationTime(modificationTime);
        r.setResource(mr);
        return r;
    }

    public void testParserAcceptHeader(){
        assertEquals(RequestUtil.parserAcceptHeader("compress;q=0.5, gzip;q=1.0").get("compress"), 0.5);
        assertEquals(RequestUtil.parserAcceptHeader("compress,gzip").get("compress"),1.0);
        assertEquals(RequestUtil.parserAcceptHeader("compress").get("compress"),1.0);
        assertEquals(RequestUtil.parserAcceptHeader("compress;q=string,gzip;q=1.0").get("compress"), 1.0);

        assertNull(RequestUtil.parserAcceptHeader("compress;q=0.5, gzip;q=1.0").get("compres"));
    }


    protected HttpServletResponse getMockResponse() {

        return new HttpServletResponse() {

            @Override
            public void setLocale(Locale loc) {}

            @Override
            public void setContentType(String type) {}

            @Override
            public void setContentLength(int len) {}

            @Override
            public void setCharacterEncoding(String charset) {}

            @Override
            public void setBufferSize(int size) {}

            @Override
            public void resetBuffer() {}

            @Override
            public void reset() {}

            @Override
            public boolean isCommitted() {
                return false;
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                return null;
            }

            @Override
            public ServletOutputStream getOutputStream() throws IOException {
                return null;
            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public int getBufferSize() {
                return 0;
            }

            @Override
            public void flushBuffer() throws IOException {}

            @Override
            @SuppressWarnings("deprecation")
            public void setStatus(int sc, String sm) {}

            @Override
            public void setStatus(int sc) {}

            @Override
            public void setIntHeader(String name, int value) {}

            @Override
            public void setHeader(String name, String value) {}

            @Override
            public void setDateHeader(String name, long date) {}

            @Override
            public void sendRedirect(String location) throws IOException {}

            @Override
            public void sendError(int sc, String msg) throws IOException {}

            @Override
            public void sendError(int sc) throws IOException {}

            @Override
            @SuppressWarnings("deprecation")
            public String encodeUrl(String url) {
                return null;
            }

            @Override
            public String encodeURL(String url) {
                return null;
            }

            @Override
            @SuppressWarnings("deprecation")
            public String encodeRedirectUrl(String url) {
                return null;
            }

            @Override
            public String encodeRedirectURL(String url) {
                return null;
            }

            @Override
            public boolean containsHeader(String name) {
                return false;
            }

            @Override
            public void addIntHeader(String name, int value) {}

            @Override
            public void addHeader(String name, String value) {}

            @Override
            public void addDateHeader(String name, long date) {}

            @Override
            public void addCookie(Cookie cookie) {}

            @Override
            public void setContentLengthLong(long len) {
                // TODO Auto-generated method stub

            }

            @Override
            public int getStatus() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getHeader(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Collection<String> getHeaders(String name) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Collection<String> getHeaderNames() {
                // TODO Auto-generated method stub
                return null;
            }
        };

    }


}
