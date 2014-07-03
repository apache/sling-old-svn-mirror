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
package org.apache.sling.launchpad.webapp.integrationtest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;

/** Test redirects */
public class RedirectTest extends HttpTestBase {

    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create the test node, under a path that's specific to this class to
        // allow collisions
        postUrl = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "_"
            + System.currentTimeMillis()
            + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
    }
    
    private void testRedirectToIndexHtml(String redirNodeUrl, int statusCode) throws IOException {

        // get the created node without following redirects
        GetMethod get = new GetMethod(redirNodeUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect redirect ...
        assertEquals(statusCode, status);

        // ... to */index.html
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.endsWith("/index.html"));

        // get the created node without following redirects
        get = new GetMethod(redirNodeUrl + ".html");
        get.setFollowRedirects(false);
        status = httpClient.executeMethod(get);

        // expect redirect ...
        assertEquals(statusCode, status);

        // ... to */index.html
        location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.endsWith("/index.html.html"));

        // get the created node without following redirects
        get = new GetMethod(redirNodeUrl + "?param=value");
        get.setFollowRedirects(false);
        status = httpClient.executeMethod(get);

        // expect redirect ...
        assertEquals(statusCode, status);

        // ... to */index.html
        location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.endsWith("/index.html?param=value"));
    }

    /** test 302 as the default redirect */
    public void testRedirect302() throws IOException {

        // create a node redirecting to /index with default status code
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        props.put("sling:target", "/index.html");
        String redirNodeUrl = testClient.createNode(postUrl, props);
        
        testRedirectToIndexHtml(redirNodeUrl, 302);
    }

    /** test 301 specified by sling:status */
    public void testRedirect301() throws IOException {

        // create a node redirecting to /index with default status code
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        props.put("sling:target", "/index.html");
        props.put("sling:status", "301");
        String redirNodeUrl = testClient.createNode(postUrl, props);
        
        testRedirectToIndexHtml(redirNodeUrl, 301);
    }
    
    /** test 302 response with existing sling:target */
    public void testRedirect302_absolute() throws IOException {

        // create a node redirecting to /index
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        props.put("sling:target", "http://some.host.none/index.html");
        String redirNodeUrl = testClient.createNode(postUrl, props);

        // get the created node without following redirects
        GetMethod get = new GetMethod(redirNodeUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect temporary redirect ...
        assertEquals(302, status);

        // ... to */index.html
        String location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.equals("http://some.host.none/index.html"));

        // get the created node without following redirects
        get = new GetMethod(redirNodeUrl + ".html");
        get.setFollowRedirects(false);
        status = httpClient.executeMethod(get);

        // expect temporary redirect ...
        assertEquals(302, status);

        // ... to */index.html
        location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.equals("http://some.host.none/index.html.html"));

        // get the created node without following redirects
        get = new GetMethod(redirNodeUrl + "?param=value");
        get.setFollowRedirects(false);
        status = httpClient.executeMethod(get);

        // expect temporary redirect ...
        assertEquals(302, status);

        // ... to */index.html
        location = get.getResponseHeader("Location").getValue();
        assertNotNull(location);
        assertTrue(location.equals("http://some.host.none/index.html?param=value"));
    }

    /** test 404 response when sling:target is missing */
    public void testRedirect404() throws IOException {
        // create a sling:redirect node without a target
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        String redirNodeUrl = testClient.createNode(postUrl, props);

        // get the created node without following redirects
        GetMethod get = new GetMethod(redirNodeUrl);
        get.setFollowRedirects(false);
        int status = httpClient.executeMethod(get);

        // expect 404 not found
        assertEquals(404, status);

        // get the created node without following redirects
        get = new GetMethod(redirNodeUrl + ".html");
        get.setFollowRedirects(false);
        status = httpClient.executeMethod(get);

        // expect 404 not found
        assertEquals(404, status);
    }

    /** test JSON result for .json requests with sling:target */
    public void testRedirectJson() throws JSONException, IOException {
        // create a sling:redirect node without a target
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        props.put("sling:target", "/index.html");
        String redirNodeUrl = testClient.createNode(postUrl, props);

        // get the created node without following redirects
        final GetMethod get = new GetMethod(redirNodeUrl + ".json");
        get.setFollowRedirects(false);
        final int status = httpClient.executeMethod(get);

        // expect 200 OK with the JSON data
        assertEquals(200, status);
        assertTrue(get.getResponseHeader("Content-Type").getValue().startsWith(CONTENT_TYPE_JSON));

        // the json data
        String jsonString = get.getResponseBodyAsString();
        JSONObject json = new JSONObject(jsonString);

        assertEquals("sling:redirect", json.get("sling:resourceType"));
        assertEquals("/index.html", json.get("sling:target"));
    }

    /** test JSON result for .json requests with sling:target */
    public void testRedirectJson2() throws JSONException, IOException {
        // create a sling:redirect node without a target
        Map<String, String> props = new HashMap<String, String>();
        props.put("sling:resourceType", "sling:redirect");
        String redirNodeUrl = testClient.createNode(postUrl, props);

        // get the created node without following redirects
        final GetMethod get = new GetMethod(redirNodeUrl + ".json");
        get.setFollowRedirects(false);
        final int status = httpClient.executeMethod(get);

        // expect 200 OK with the JSON data
        assertEquals(200, status);
        assertTrue(get.getResponseHeader("Content-Type").getValue().startsWith(CONTENT_TYPE_JSON));

        // the json data
        String jsonString = get.getResponseBodyAsString();
        JSONObject json = new JSONObject(jsonString);

        assertEquals("sling:redirect", json.get("sling:resourceType"));
    }

}
