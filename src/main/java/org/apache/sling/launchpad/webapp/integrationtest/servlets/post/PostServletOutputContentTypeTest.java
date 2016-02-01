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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test switching the output content-type of the POST servlet using
 *  either an Accept header or :http-equiv-accept parameter */
public class PostServletOutputContentTypeTest extends HttpTestBase {
    
    private final String MY_TEST_PATH = TEST_PATH + "/" + getClass().getSimpleName() + "/" + System.currentTimeMillis();
    
    private void runTest(String acceptHeaderValue, boolean useHttpEquiv, String expectedContentType) throws Exception {
        final String info = (useHttpEquiv ? "Using http-equiv parameter" : "Using Accept header") + ": ";
        final String url = HTTP_BASE_URL + MY_TEST_PATH;
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);
        
        if(acceptHeaderValue != null) {
            if(useHttpEquiv) {
                post.addParameter(":http-equiv-accept", acceptHeaderValue);
            } else {
                post.addRequestHeader("Accept", acceptHeaderValue);
            }
        }
        
        final int status = httpClient.executeMethod(post) / 100;
        assertEquals(info + "Expected status 20x for POST at " + url, 2, status);
        final Header h = post.getResponseHeader("Content-Type");
        assertNotNull(info + "Expected Content-Type header", h);
        final String ct = h.getValue();
        assertTrue(info + "Expected Content-Type '" + expectedContentType + "' for Accept header=" + acceptHeaderValue
                + " but got '" + ct + "'",
                ct.startsWith(expectedContentType));
    }
    
    public void runTest(String acceptHeaderValue, String expectedContentType) throws Exception {
        runTest(acceptHeaderValue, false, expectedContentType);
        runTest(acceptHeaderValue, true, expectedContentType);
    }
    
    public void testDefaultContentType() throws Exception {
        runTest(null, CONTENT_TYPE_HTML);
    }

    public void testJsonContentType() throws Exception {
        runTest("application/json,*/*;q=0.9", CONTENT_TYPE_JSON);
    }

    public void testHtmlContentType() throws Exception {
        runTest("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", CONTENT_TYPE_HTML);
    }

    public void testHtmlContentTypeWithQ() throws Exception {
        runTest("text/plain; q=0.5, text/html,application/xhtml+xml,application/xml;q=0.9, application/json;q=0.8", 
                CONTENT_TYPE_HTML);         
    }
    
    public void testJsonContentTypeWithQ() throws Exception {
        runTest("text/plain; q=0.5, text/html; q=0.8, application/json; q=0.9", CONTENT_TYPE_JSON);         
    }
    
    public void testJsonContentTypeException() throws Exception {

      // Perform a POST that fails: invalid PostServlet operation
      // with Accept header set to JSON  
      final String url = HTTP_BASE_URL + MY_TEST_PATH;
      final PostMethod post = new PostMethod(url);
      post.setFollowRedirects(false);
      post.addParameter(new NameValuePair(
          SlingPostConstants.RP_OPERATION,
          "InvalidTestOperationFor" + getClass().getSimpleName()));
      post.addRequestHeader("Accept", CONTENT_TYPE_JSON);

      final int status = httpClient.executeMethod(post);
      assertEquals(500, status);
      final String contentType = post.getResponseHeader("Content-Type").getValue();
      final String expected = CONTENT_TYPE_JSON;
      assertTrue("Expecting content-type " + expected + " for failed POST request, got " + contentType,
              contentType!=null && contentType.startsWith(expected));
    }
}
