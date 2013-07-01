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

            public void setLocale(Locale loc) {}

            public void setContentType(String type) {}

            public void setContentLength(int len) {}

            public void setCharacterEncoding(String charset) {}

            public void setBufferSize(int size) {}

            public void resetBuffer() {}

            public void reset() {}

            public boolean isCommitted() { 
                return false;
            }

            public PrintWriter getWriter() throws IOException {
                return null;
            }

            public ServletOutputStream getOutputStream() throws IOException {
                return null;
            }

            public Locale getLocale() { 
                return null;
            }

            public String getContentType() { 
                return null;
            }

            public String getCharacterEncoding() { 
                return null;
            }

            public int getBufferSize() {
                return 0;
            }

            public void flushBuffer() throws IOException {}

            public void setStatus(int sc, String sm) {}

            public void setStatus(int sc) {}

            public void setIntHeader(String name, int value) {}

            public void setHeader(String name, String value) {}

            public void setDateHeader(String name, long date) {}

            public void sendRedirect(String location) throws IOException {}

            public void sendError(int sc, String msg) throws IOException {}

            public void sendError(int sc) throws IOException {}

            public String encodeUrl(String url) {
                return null;
            }

            public String encodeURL(String url) {
                return null;
            }

            public String encodeRedirectUrl(String url) {
                return null;
            }

            public String encodeRedirectURL(String url) {
                return null;
            }

            public boolean containsHeader(String name) {
                return false;
            }

            public void addIntHeader(String name, int value) {}

            public void addHeader(String name, String value) {}

            public void addDateHeader(String name, long date) {}

            public void addCookie(Cookie cookie) {}
        };

    }


}
