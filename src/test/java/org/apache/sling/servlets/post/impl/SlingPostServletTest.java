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
import java.net.URLEncoder;

import junit.framework.TestCase;

import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.MediaRangeList;

public class SlingPostServletTest extends TestCase {

    public void testIsSetStatus() {
        StatusParamSlingHttpServletRequest req = new StatusParamSlingHttpServletRequest();
        SlingPostServlet servlet = new SlingPostServlet();

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
        MockSlingHttpServletRequest req = new MockSlingHttpServletRequest(null, null, null, null, null) {
            @Override
            public String getHeader(String name) {
                return name.equals(MediaRangeList.HEADER_ACCEPT) ? "application/json" : super.getHeader(name);
            }

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                return null;
            }
        };
        SlingPostServlet servlet = new SlingPostServlet();
        PostResponse result = servlet.createPostResponse(req);
        assertTrue(result instanceof JSONResponse);
    }
    
    public void testRedirection() throws Exception {
    	final String[] redirectLocation = new String[1];
    	MockSlingHttpServletResponse resp = new MockSlingHttpServletResponse() {
    		@Override
    		public String encodeRedirectURL(String s) {
    			try {
					return URLEncoder.encode(s, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					fail("Should have UTF-8?? " + e);
					return null;
				}
    		}
    		
    		@Override
    		public void sendRedirect(String s) throws IOException {
    			redirectLocation[0] = s;
    		}
    	};
    	
    	SlingPostServlet servlet = new SlingPostServlet();

    	assertTrue(servlet.redirectIfNeeded("\u0414\u0440\u0443\u0433\u0430.html", resp));
    	assertEquals("Should encode UTF-8", "%D0%94%D1%80%D1%83%D0%B3%D0%B0.html", redirectLocation[0]);

    	redirectLocation[0] = null;
    	assertTrue(servlet.redirectIfNeeded("fred.html", resp));
    	assertEquals("Plain old ASCII passes through", "fred.html", redirectLocation[0]);

    	redirectLocation[0] = null;
    	assertFalse(servlet.redirectIfNeeded(null, resp));
    	assertNull("Shouldn't have encoded anything", redirectLocation[0]);
    }

    private static class StatusParamSlingHttpServletRequest extends
            MockSlingHttpServletRequest {

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
