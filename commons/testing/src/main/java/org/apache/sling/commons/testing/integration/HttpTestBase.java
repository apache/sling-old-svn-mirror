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
package org.apache.sling.commons.testing.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Base class for HTTP-based Sling Launchpad integration tests */
public class HttpTestBase extends TestCase {
    public static final String HTTP_BASE_URL = removeEndingSlash(System.getProperty("launchpad.http.server.url", "http://localhost:8888"));
    public static final String WEBDAV_BASE_URL = removeEndingSlash(System.getProperty("launchpad.webdav.server.url", "http://localhost:8888"));

    /** base path for test files */
    public static final String TEST_PATH = "/launchpad-integration-tests";

    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_JS = "application/x-javascript";
    public static final String CONTENT_TYPE_CSS = "text/css";

    public static final String SLING_RESOURCE_TYPE = "sling:resourceType";
    
    public static final String SLING_POST_SERVLET_CREATE_SUFFIX = "/";

    protected SlingIntegrationTestClient testClient;
    protected HttpClient httpClient;

    private static Boolean slingStartupOk;
    
    /** Means "don't care about Content-Type" in getContent(...) methods */
    public static final String CONTENT_TYPE_DONTCARE = "*";

    /** URLs stored here are deleted in tearDown */
    protected final List<String> urlsToDelete = new LinkedList<String>();

    /** Class that creates a test node under the given parentPath, and
     *  stores useful values for testing. Created for JspScriptingTest,
     *  older test classes do not use it, but it might simplify them.
     */
    protected class TestNode {
        public final String testText;
        public final String nodeUrl;
        public final String resourceType;
        public final String scriptPath;

        public TestNode(String parentPath, Map<String, String> properties) throws IOException {
            if(properties == null) {
                properties = new HashMap<String, String>();
            }
            testText = "This is a test node " + System.currentTimeMillis();
            properties.put("text", testText);
            nodeUrl = testClient.createNode(parentPath + SLING_POST_SERVLET_CREATE_SUFFIX, properties);
            resourceType = properties.get(SLING_RESOURCE_TYPE);
            scriptPath = "/apps/" + (resourceType == null ? "nt/unstructured" : resourceType);
            testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
        }

        public void delete() throws IOException {
            testClient.delete(nodeUrl);
        }
    };

    protected static String removeEndingSlash(String str) {
        if(str != null && str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // assume http and webdav are on the same host + port
        URL url = null;
        try {
            url = new URL(HTTP_BASE_URL);
        } catch(MalformedURLException mfe) {
            // MalformedURLException doesn't tell us the URL by default
            throw new IOException("MalformedURLException: " + HTTP_BASE_URL);
        }

        // setup HTTP client, with authentication (using default Jackrabbit credentials)
        httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials("admin", "admin");
        httpClient.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);

        testClient = new SlingIntegrationTestClient(httpClient);

        waitForSlingStartup();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        for(String url : urlsToDelete) {
            testClient.delete(url);
        }
    }

    /** On the server side, initialization of Sling bundles is done
     *  asynchronously once the webapp is started. This method checks
     *  that everything's ready on the server side, by calling an URL
     *  that requires the SlingPostServlet and the JCR repository to
     *  work correctly.
     */
    protected void waitForSlingStartup() throws Exception {
        // Use a static flag to make sure this runs only once in our test suite
        if (slingStartupOk != null) {
            if(slingStartupOk) {
                return;
            }
            fail("Sling services not available. Already checked in earlier tests.");
        }
        slingStartupOk = false;

        System.err.println("Checking if the required Sling services are started...");
        System.err.println("(base URLs=" + HTTP_BASE_URL + " and " + WEBDAV_BASE_URL + ")");

        // Try creating a node on server, every 500msec, until ok, with timeout
        final List<String> exceptionMessages = new LinkedList<String>();
        final long maxMsecToWait = 60 * 1000L;
        final long startupTime = System.currentTimeMillis();

        while(!slingStartupOk && (System.currentTimeMillis() < startupTime + maxMsecToWait) ) {
            try {
                slingStartupOk = slingServerReady();
            } catch(Exception e) {
                exceptionMessages.add(e.toString());
                Thread.sleep(500L);
            }
        }

        if(!slingStartupOk) {
            StringBuffer msg = new StringBuffer("Server does not seem to be ready, after ");
            msg.append(maxMsecToWait).append(" msec, got the following ").append(exceptionMessages.size()).append(" Exceptions:");
            for (String e: exceptionMessages) {
                msg.append(e).append("\n");
            }
            System.err.println(msg);
            fail(msg.toString());
        }

        System.err.println("Sling services seem to be started, continuing with integration tests.");
    }

    /** Return true if able to create and retrieve a node on server */
    protected boolean slingServerReady() throws Exception {
        // create a node on the server
        final String time = String.valueOf(System.currentTimeMillis());
        final String url = HTTP_BASE_URL + "/WaitForSlingStartup/" + time;

        // add some properties to the node
        final Map<String,String> props = new HashMap<String,String>();
        props.put("time", time);

        // POST, get URL of created node and get content
        {
            final String urlOfNewNode = testClient.createNode(url, props, null, true);
            final GetMethod get = new GetMethod(urlOfNewNode + ".txt");
            final int status = httpClient.executeMethod(get);
            if(status!=200) {
                throw new HttpStatusCodeException(200, status, "GET", urlOfNewNode);
            }

            final Header h = get.getResponseHeader("Content-Type");
            final String contentType = h==null ? "" : h.getValue();
            if(!contentType.startsWith("text/plain")) {
                throw new IOException("Expected Content-Type=text/plain but got '" + contentType + "' for URL=" + urlOfNewNode);
            }

            final String content = get.getResponseBodyAsString();
            if(!content.contains(time)) {
                throw new IOException("Content does not contain '" + time + "' (" + content + ") at URL=" + urlOfNewNode);
            }
        }

        // Also check that the WebDAV root is ready
        {
            // need the trailing slash in case the base URL is the context root
            final String webDavUrl = WEBDAV_BASE_URL + "/";
            final HttpAnyMethod options = new HttpAnyMethod("OPTIONS",webDavUrl);
            final int status = httpClient.executeMethod(options);
            if(status!=200) {
                throw new HttpStatusCodeException(200, status, "OPTIONS", webDavUrl);
            }

            // The Allow header tells us that we're talking to a WebDAV server
            final Header h = options.getResponseHeader("Allow");
            if(h == null) {
                throw new IOException("Response does not contain Allow header, at URL=" + webDavUrl);
            } else if(h.getValue() == null) {
                throw new IOException("Allow header has null value at URL=" + webDavUrl);
            } else if(!h.getValue().contains("PROPFIND")) {
                throw new IOException("Allow header (" + h.getValue() + " does not contain PROPFIND, at URL=" + webDavUrl);
            }
        }


        return true;
    }

    /** Verify that given URL returns expectedStatusCode
     * @throws IOException */
    protected void assertHttpStatus(String urlString, int expectedStatusCode, String assertMessage) throws IOException {
        final int status = httpClient.executeMethod(new GetMethod(urlString));
        if(assertMessage == null) {
            assertEquals(urlString,expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
    }

    /** Verify that given URL returns expectedStatusCode
     * @throws IOException */
    protected void assertHttpStatus(String urlString, int expectedStatusCode) throws IOException {
        assertHttpStatus(urlString, expectedStatusCode, null);
    }

    /** Execute a POST request and check status */
    protected void assertPostStatus(String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
    throws IOException {
        final PostMethod post = new PostMethod(url);
        post.setFollowRedirects(false);

        if(postParams!=null) {
            final NameValuePair [] nvp = {};
            post.setRequestBody(postParams.toArray(nvp));
        }

        final int status = httpClient.executeMethod(post);
        if(assertMessage == null) {
            assertEquals(expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
    }

    /** retrieve the contents of given URL and assert its content type */
    protected String getContent(String url, String expectedContentType) throws IOException {
        return getContent(url, expectedContentType, null);
    }

    /** retrieve the contents of given URL and assert its content type
     * @param expectedContentType use CONTENT_TYPE_DONTCARE if must not be checked 
     * @throws IOException
     * @throws HttpException */
    protected String getContent(String url, String expectedContentType, List<NameValuePair> params) throws IOException {
        final GetMethod get = new GetMethod(url);
        if(params != null) {
            final NameValuePair [] nvp = new NameValuePair[0];
            get.setQueryString(params.toArray(nvp));
        }
        final int status = httpClient.executeMethod(get);
        final InputStream is = get.getResponseBodyAsStream();
        final StringBuffer content = new StringBuffer();
        final String charset = get.getResponseCharSet();
        final byte [] buffer = new byte[16384];
        int n = 0;
        while( (n = is.read(buffer, 0, buffer.length)) > 0) {
            content.append(new String(buffer, 0, n, charset));
        }
        assertEquals("Expected status 200 for " + url + " (content=" + content + ")",200,status);
        final Header h = get.getResponseHeader("Content-Type");
        if(expectedContentType == null) {
            if(h!=null) {
                fail("Expected null Content-Type, got " + h.getValue());
            }
        } else if(CONTENT_TYPE_DONTCARE.equals(expectedContentType)) {
            // no check
        } else if(h==null) {
            fail(
                    "Expected Content-Type that starts with '" + expectedContentType
                    +" but got no Content-Type header at " + url
            );
        } else {
            assertTrue(
                "Expected Content-Type that starts with '" + expectedContentType
                + "' for " + url + ", got '" + h.getValue() + "'",
                h.getValue().startsWith(expectedContentType)
            );
        }
        return content.toString();
    }

    /** upload rendering test script, and return its URL for future deletion */
    protected String uploadTestScript(String scriptPath, String localFilename,String filenameOnServer) throws IOException {
        final String url = WEBDAV_BASE_URL + scriptPath + "/" + filenameOnServer;
        final String testFile = "/integration-test/" + localFilename;
        final InputStream data = getClass().getResourceAsStream(testFile);
        if(data==null) {
            fail("Test file not found:" + testFile);
        }
        try {
            testClient.upload(url, data);
        } finally {
            if(data!=null) {
                data.close();
            }
        }
        return url;
    }

    protected void assertJavascript(String expectedOutput, String jsonData, String code) throws IOException {
        assertJavascript(expectedOutput, jsonData, code, null);
    }

    /** Evaluate given code using given jsonData as the "data" object */
    protected void assertJavascript(String expectedOutput, String jsonData, String code, String testInfo) throws IOException {
        // build the code, something like
        //  data = <jsonData> ;
        //  <code>
        final String jsCode = "data=" + jsonData + ";\n" + code;
        final Context rhinoContext = Context.enter();
        final ScriptableObject scope = rhinoContext.initStandardObjects();

        // execute the script, out script variable maps to sw
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ScriptableObject.putProperty(scope, "out", Context.javaToJS(pw, scope));
        final int lineNumber = 1;
        final Object securityDomain = null;
        rhinoContext.evaluateString(scope, jsCode, getClass().getSimpleName(),
                lineNumber, securityDomain);

        // check script output
        pw.flush();
        final String result = sw.toString().trim();
        if(!result.equals(expectedOutput)) {
            fail(
                    "Expected '" + expectedOutput
                    + "' but got '" + result
                    + "' for script='" + jsCode + "'"
                    + (testInfo==null ? "" : ", test info=" + testInfo)
            );
        }
    }
}
