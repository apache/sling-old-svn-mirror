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
package org.apache.sling.servlets.post.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockComponentContext;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.servlets.post.HtmlResponse;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.MediaRangeList;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Request;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Response;
import org.osgi.framework.Constants;

public class SlingPostServletTest extends TestCase {
    
    private SlingPostServlet servlet;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        servlet = new SlingPostServlet();
        MockBundle bundle = new MockBundle(1) {
            @Override
            public Dictionary<String, String> getHeaders() {
                Hashtable<String, String> headers = new Hashtable<String, String>();
                headers.put(Constants.BUNDLE_VENDOR, "test");
                return headers;
            }

            @Override
            public Dictionary<String, String> getHeaders(String locale) {
                return getHeaders();
            }

            @Override
            public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
                return null;
            }

            @Override
            public Enumeration<String> getEntryPaths(String path) {
                return null;
            }

            @Override
            public Enumeration<URL> getResources(String name) {
                return null;
            }
        };
        MockComponentContext componentContext = new MockComponentContext(bundle);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("servlet.post.dateFormats", new String[] { "EEE MMM dd yyyy HH:mm:ss 'GMT'Z" });
        servlet.activate(componentContext, props);
    }

    public void testIsSetStatus() {
        StatusParamSlingHttpServletRequest req = new StatusParamSlingHttpServletRequest();

        // 1. null parameter, expect true
        req.setStatusParam(null);
        assertTrue("Standard status expected for null param",
            servlet.isSetStatus(req));

        // 2. "standard" parameter, expect true
        req.setStatusParam(SlingPostConstants.STATUS_VALUE_STANDARD);
        assertTrue("Standard status expected for '"
            + SlingPostConstants.STATUS_VALUE_STANDARD + "' param",
            servlet.isSetStatus(req));

        // 3. "browser" parameter, expect false
        req.setStatusParam(SlingPostConstants.STATUS_VALUE_BROWSER);
        assertFalse("Browser status expected for '"
            + SlingPostConstants.STATUS_VALUE_BROWSER + "' param",
            servlet.isSetStatus(req));

        // 4. any parameter, expect true
        String param = "knocking on heaven's door";
        req.setStatusParam(param);
        assertTrue("Standard status expected for '" + param + "' param",
            servlet.isSetStatus(req));
    }

    public void testGetJsonResponse() {
        MockSlingHttpServletRequest req = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getHeader(String name) {
                return name.equals(MediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };
        PostResponse result = servlet.createPostResponse(req);
        assertTrue(result instanceof JSONResponse);
    }
    
    public void testRedirection() throws Exception {
        String utf8Path = "\u0414\u0440\u0443\u0433\u0430";
        String encodedUtf8 = "%D0%94%D1%80%D1%83%D0%B3%D0%B0";
        testRedirection("/", "/fred", "*.html", "/fred.html");
        testRedirection("/xyz/", "/xyz/"+utf8Path, "*", "/xyz/"+encodedUtf8);
        testRedirection("/", "/fred/"+utf8Path, "/xyz/*", "/xyz/"+encodedUtf8);
        testRedirection("/", "/fred/"+utf8Path, null, null);
        // test redirect with host information
        testRedirection("/", "/fred/abc", "http://forced", null);
        testRedirection("/", "/fred/abc", "//forced.com/test", null);
        testRedirection("/", "/fred/abc", "https://forced.com/test", null);
        // invalid URI
        testRedirection("/", "/fred/abc", "file://c:\\Users\\workspace\\test.java", null);
    }

    public void testNonExistingOperation() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getParameter(String name) {
                if (name.equals(SlingPostConstants.RP_OPERATION)) {
                    return "doesntexist";
                }
                return null;
            }
        };
        MockSlingHttpServlet3Response response = new MockSlingHttpServlet3Response();
        servlet.doPost(request, response);
        assertEquals(500, response.getStatus());
    }

    public void testNonExistingPostProcessor() throws Exception {
        servlet.bindPostOperation(new PostOperation() {
            @Override
            public void run(SlingHttpServletRequest request, PostResponse response, SlingPostProcessor[] processors) {
                // noop
            }
        }, Collections.<String, Object>singletonMap(SlingPostOperation.PROP_OPERATION_NAME, "noop"));

        MockSlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getParameter(String name) {
                if (name.equals(SlingPostConstants.RP_OPERATION)) {
                    return "noop";
                } else if (name.equals(":requiredPostProcessors")) {
                    return "doesntexist";
                }
                return null;
            }
        };
        MockSlingHttpServlet3Response response = new MockSlingHttpServlet3Response();
        servlet.doPost(request, response);
        assertEquals(501, response.getStatus());
    }



    public void testNonExistingPostProcessorWithMultipleRequired() throws Exception {
        servlet.bindPostOperation(new PostOperation() {
            @Override
            public void run(SlingHttpServletRequest request, PostResponse response, SlingPostProcessor[] processors) {
                // noop
            }
        }, Collections.<String, Object>singletonMap(SlingPostOperation.PROP_OPERATION_NAME, "noop"));

        servlet.bindPostProcessor(new SlingPostProcessor() {
            @Override
            public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
                // noop
            }
        }, Collections.<String, Object>singletonMap("postProcessor.name", "noop"));

        MockSlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getParameter(String name) {
                if (name.equals(SlingPostConstants.RP_OPERATION)) {
                    return "noop";
                } else if (name.equals(":requiredPostProcessors")) {
                    return "noop,doesntexist";
                }
                return null;
            }
        };
        MockSlingHttpServlet3Response response = new MockSlingHttpServlet3Response();
        servlet.doPost(request, response);
        assertEquals(501, response.getStatus());
    }

    public void testRequiredPostProcessor() throws Exception {
        servlet.bindPostOperation(new PostOperation() {
            @Override
            public void run(SlingHttpServletRequest request, PostResponse response, SlingPostProcessor[] processors) {
                // noop
            }
        }, Collections.<String, Object>singletonMap(SlingPostOperation.PROP_OPERATION_NAME, "noop"));

        servlet.bindPostProcessor(new SlingPostProcessor() {
            @Override
            public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
                // noop
            }
        }, Collections.<String, Object>singletonMap("postProcessor.name", "noop"));

        MockSlingHttpServletRequest request = new MockSlingHttpServlet3Request(null, null, null, null, null) {
            @Override
            public String getParameter(String name) {
                if (name.equals(SlingPostConstants.RP_OPERATION)) {
                    return "noop";
                } else if (name.equals(":requiredPostProcessors")) {
                    return "noop";
                }
                return null;
            }
        };
        MockSlingHttpServlet3Response response = new MockSlingHttpServlet3Response();
        servlet.doPost(request, response);
        assertEquals(200, response.getStatus());
    }

    private void testRedirection(String requestPath, String resourcePath, String redirect, String expected) 
            throws Exception {
        RedirectServletResponse resp = new RedirectServletResponse();
        SlingHttpServletRequest request = new RedirectServletRequest(redirect, requestPath);
        PostResponse htmlResponse = new HtmlResponse();
        htmlResponse.setPath(resourcePath);
        assertEquals(expected != null, servlet.redirectIfNeeded(request, htmlResponse, resp));
        assertEquals(expected, resp.redirectLocation);
    }

    /**
     *
     */
    private final class RedirectServletRequest extends MockSlingHttpServlet3Request {

        private String requestPath;
        private String redirect;

        private RedirectServletRequest(String redirect, String requestPath) {
            super(null, null, null, null, null);
            this.requestPath = requestPath;
            this.redirect = redirect;
        }

        public String getPathInfo() {
            return requestPath;
        }
        
        @Override
        public String getParameter(String name) {
            return SlingPostConstants.RP_REDIRECT_TO.equals(name) ? redirect : null;
        }
    }

    private final class RedirectServletResponse extends MockSlingHttpServlet3Response {

        private String redirectLocation;

        @Override
        public String encodeRedirectURL(String s) {
            StringTokenizer st = new StringTokenizer(s, "/", true);
            StringBuilder sb = new StringBuilder();
        	try {
        	    while (st.hasMoreTokens()) {
        	        String token = st.nextToken();
        	        if ("/".equals(token)) {
                        sb.append(token);
        	        } else {
        	            sb.append(URLEncoder.encode(token, "UTF-8"));
        	        }
        	    }
        	} catch (UnsupportedEncodingException e) {
        		fail("Should have UTF-8?? " + e);
        		return null;
        	}
            return sb.toString();
        }

        @Override
        public void sendRedirect(String s) throws IOException {
        	redirectLocation = s;
        }
    }

    private static class StatusParamSlingHttpServletRequest extends
            MockSlingHttpServlet3Request {

        private String statusParam;

        public StatusParamSlingHttpServletRequest() {
            // nothing to setup, we don't care
            super(null, null, null, null, null);
        }

        @Override
        public String getParameter(String name) {
            if (SlingPostConstants.RP_STATUS.equals(name)) {
                return statusParam;
            }

            return super.getParameter(name);
        }

        void setStatusParam(String statusParam) {
            this.statusParam = statusParam;
        }

        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return null;
        }
    }
}
