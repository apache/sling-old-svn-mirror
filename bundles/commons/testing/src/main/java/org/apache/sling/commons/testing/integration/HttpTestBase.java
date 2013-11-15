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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.util.JavascriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Base class for HTTP-based Sling Launchpad integration tests */
public class HttpTestBase extends TestCase {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** If this system property is set, the startup check is skipped. */
    public static final String PROPERTY_SKIP_STARTUP_CHECK = "launchpad.skip.startupcheck";

    public static final String HTTP_URL = removeEndingSlash(System.getProperty("launchpad.http.server.url", "http://localhost:8888"));
    public static final String HTTP_BASE_URL = removePath(HTTP_URL);
    public static final String WEBDAV_BASE_URL = removeEndingSlash(System.getProperty("launchpad.webdav.server.url", HTTP_BASE_URL));
    public static final String SERVLET_CONTEXT = removeEndingSlash(System.getProperty("launchpad.servlet.context", getPath(HTTP_URL)));
    
    public static final String READY_URL_PROP_PREFIX = "launchpad.ready.";
    public static final int MAX_READY_URL_INDEX = 50;

    /** base path for test files */
    public static final String TEST_PATH = "/launchpad-integration-tests";
    
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";

    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_JS = "application/javascript";
    public static final String CONTENT_TYPE_CSS = "text/css";

    public static final String SLING_RESOURCE_TYPE = "sling:resourceType";

    public static final String SLING_POST_SERVLET_CREATE_SUFFIX = "/";
	public static final String DEFAULT_EXT = ".txt";

	public static final String EXECUTE_RESOURCE_TYPE = "SlingTesting" + HttpTestBase.class.getSimpleName();
	private static int executeCounter;

    public static final int READY_TIMEOUT_SECONDS = Integer.getInteger("HttpTestBase.readyTimeoutSeconds", 60);

    protected SlingIntegrationTestClient testClient;
    protected HttpClient httpClient;

    private static Boolean slingStartupOk;

    /** Means "don't care about Content-Type" in getContent(...) methods */
    public static final String CONTENT_TYPE_DONTCARE = "*";

    private static final Object startupCheckLock = new Object();

    /** URLs stored here are deleted in tearDown */
    protected final List<String> urlsToDelete = new LinkedList<String>();

    /** Need to execute javascript code */
    private final JavascriptEngine javascriptEngine = new JavascriptEngine();

    /** Class that creates a test node under the given parentPath, and
     *  stores useful values for testing. Created for JspScriptingTest,
     *  older test classes do not use it, but it might simplify them.
     */
    protected class TestNode extends HttpTestNode {
        public TestNode(String parentPath, Map<String, String> properties) throws IOException {
            super(testClient, parentPath, properties);
        }
    };

    public static String removeEndingSlash(String str) {
        if(str != null && str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    private static String removePath(String str) {
        final int pos = str.indexOf(":/");
        final int slashPos = str.indexOf('/', pos+3);
        if ( slashPos != -1 ) {
            return str.substring(0, slashPos);
        }
        return str;
    }

    private static String getPath(String str) {
        final int pos = str.indexOf(":/");
        final int slashPos = str.indexOf('/', pos+3);
        if ( slashPos != -1 ) {
            return str.substring(slashPos);
        }
        return "";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MDC.put("testclass", getClass().getName());
        MDC.put("testcase", getName());

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
        Credentials defaultcreds = getDefaultCredentials();
        httpClient.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);

        testClient = new SlingIntegrationTestClient(httpClient);

        waitForSlingStartup();
    }

    /**
     * Generate default credentials used for HTTP requests.
     */
    public Credentials getDefaultCredentials() {
        return new UsernamePasswordCredentials("admin", "admin");
    }

    @Override
    protected void tearDown() throws Exception {
        MDC.remove("testcase");
        MDC.remove("testclass");

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
        // we must synchronize on this if we don't 2 threads could enter the check concurrently
        // which would leave to random results.
        synchronized(startupCheckLock) {
            if (slingStartupOk != null) {
                if(slingStartupOk) {
                    return;
                }
                fail("Sling services not available. Already checked in earlier tests.");
            }
            if ( System.getProperty(PROPERTY_SKIP_STARTUP_CHECK) != null ) {
                slingStartupOk = true;
                return;
            }
            slingStartupOk = false;
        }

        System.err.println("Checking if the required Sling services are started (timeout " + READY_TIMEOUT_SECONDS + " seconds)...");
        System.err.println("(base URLs=" + HTTP_BASE_URL + " and " + WEBDAV_BASE_URL + "; servlet context="+ SERVLET_CONTEXT +")");

        // Try creating a node on server, every 500msec, until ok, with timeout
        final List<String> exceptionMessages = new LinkedList<String>();
        final long maxMsecToWait = READY_TIMEOUT_SECONDS * 1000L;
        final long startupTime = System.currentTimeMillis();
        String lastException = "";
        int nTimesOk = 0;
        
        // Wait until slingServerReady returns true this many times,
        // as in some cases more initializations might take place after
        // this returns true
        final int MIN_TIMES_OK = 4;
        
        while(!slingStartupOk && (System.currentTimeMillis() < startupTime + maxMsecToWait) ) {
            try {
                if(slingServerReady()) {
                    nTimesOk++;
                    if(nTimesOk >= MIN_TIMES_OK) {
                        slingStartupOk = true;
                        break;
                    }
                } else {
                    nTimesOk = 0;
                }
            } catch(Exception e) {
                nTimesOk = 0;
                final String newX = e.toString();
                if(!lastException.equals(newX)) {
                    exceptionMessages.add(newX);
                }
                lastException = newX;
            }
            Thread.sleep(500L);
        }

        if(slingStartupOk) {
            log.info("Sling server found ready after {} msec", System.currentTimeMillis() - startupTime);
        } else {
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
        String urlOfNewNode = null; 
        try {
            urlOfNewNode = testClient.createNode(url, props, null, true);
            final GetMethod get = new GetMethod(urlOfNewNode + DEFAULT_EXT);
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
        } finally {
            if(urlOfNewNode != null) {
                try {
                    testClient.delete(urlOfNewNode);
                } catch(Exception ignore) {
                }
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
        
        // And check optional additional URLs for readyness
        // Defined by system properties like
        //  launchpad.ready.1 = GET:/tmp/someUrl:200:expectedRegexpInResponse
        {
            for(int i=0; i <= MAX_READY_URL_INDEX ; i++) {
                final String propName = READY_URL_PROP_PREFIX + i;
                final String readyDef = System.getProperty(propName, "");
                final String [] parts = readyDef.split(":");
                if(parts.length == 4) {
                    final String info = propName + "=" + readyDef;
                    final HttpAnyMethod m = new HttpAnyMethod(parts[0],HTTP_BASE_URL + parts[1]);
                    final int expectedStatus = Integer.valueOf(parts[2]);
                    final int status = httpClient.executeMethod(m);
                    if(expectedStatus != status) {
                        throw new IOException("Status " + status + " does not match expected value: " + info); 
                    }
                    final String content = m.getResponseBodyAsString();
                    final Pattern p = Pattern.compile("(?s).*" + parts[3] + ".*");
                    if(!p.matcher(content).matches()) {
                        throw new IOException("Content does not match expected regexp:" + info  + ", content=" + content);
                    }
                }
            }
        }

        return true;
    }

    /** Verify that given URL returns expectedStatusCode
     * @return the HttpMethod executed
     * @throws IOException */
    public HttpMethod assertHttpStatus(String urlString, int expectedStatusCode, String assertMessage) throws IOException {
        final GetMethod get = new GetMethod(urlString);
        final int status = httpClient.executeMethod(get);
        if(assertMessage == null) {
            assertEquals(urlString,expectedStatusCode, status);
        } else {
            assertEquals(assertMessage, expectedStatusCode, status);
        }
        return get;
    }

    /** Verify that given URL returns expectedStatusCode
     * @return the HttpMethod executed
     * @throws IOException */
    public HttpMethod assertHttpStatus(String urlString, int expectedStatusCode) throws IOException {
        return assertHttpStatus(urlString, expectedStatusCode, null);
    }

    /** Execute a POST request and check status
     * @return the HttpMethod executed
     * @throws IOException */
    public HttpMethod assertPostStatus(String url, int expectedStatusCode, List<NameValuePair> postParams, String assertMessage)
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
        return post;
    }

    /** retrieve the contents of given URL and assert its content type (default to HTTP GET method)*/
    public String getContent(String url, String expectedContentType) throws IOException {
        return getContent(url, expectedContentType, null);
    }

    /** retrieve the contents of given URL and assert its content type (default to HTTP GET method)*/
    public String getContent(String url, String expectedContentType, List<NameValuePair> params) throws IOException {
        return getContent(url, expectedContentType, params, HttpServletResponse.SC_OK);
    }

    /** retrieve the contents of given URL and assert its content type (default to HTTP GET method)
     * @param expectedContentType use CONTENT_TYPE_DONTCARE if must not be checked
     * @throws IOException
     * @throws HttpException */
    public String getContent(String url, String expectedContentType, List<NameValuePair> params, int expectedStatusCode) throws IOException {
    	return getContent(url, expectedContentType, params, expectedStatusCode, HTTP_METHOD_GET);
    }
    
    /** retrieve the contents of given URL and assert its content type
     * @param expectedContentType use CONTENT_TYPE_DONTCARE if must not be checked
     * @param httMethod supports just GET and POST methods
     * @throws IOException
     * @throws HttpException */
    public String getContent(String url, String expectedContentType, List<NameValuePair> params, int expectedStatusCode, String httpMethod) throws IOException {
    	HttpMethodBase method = null;
    	
    	if (HTTP_METHOD_GET.equals(httpMethod)){
    		method= new GetMethod(url);
    	}else if (HTTP_METHOD_POST.equals(httpMethod)){
    		method = new PostMethod(url);
    	}  else{
    		fail("Http Method not supported in this test suite, method: "+httpMethod);
    	}
    	
    	if(params != null) {
            final NameValuePair [] nvp = new NameValuePair[0];
            method.setQueryString(params.toArray(nvp));
        }
        final int status = httpClient.executeMethod(method);
        final String content = getResponseBodyAsStream(method, 0);
        assertEquals("Expected status " + expectedStatusCode + " for " + url + " (content=" + content + ")",
                expectedStatusCode,status);
        final Header h = method.getResponseHeader("Content-Type");
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
    public String uploadTestScript(String scriptPath, String localFilename,String filenameOnServer) throws IOException {
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

    /** Upload script, execute with no parameters and return content */
    public String executeScript(String localFilename) throws Exception {
        return executeScript(localFilename, null);
    }

    /** Upload script, execute with given parameters (optional) and return content */
    public String executeScript(String localFilename, List<NameValuePair> params) throws Exception {

        // Use unique resource type
        int counter = 0;
        synchronized (getClass()) {
            counter = ++executeCounter;
        }
        final String resourceType = EXECUTE_RESOURCE_TYPE + counter;
        final String scriptPath = "/apps/" + resourceType;
        testClient.mkdirs(WEBDAV_BASE_URL , scriptPath);

        final int pos = localFilename.lastIndexOf(".");
        if(pos < 1) {
            throw new IllegalArgumentException("localFilename must have extension (" + localFilename + ")");
        }
        final String ext = localFilename.substring(pos + 1);
        final List<String> toDelete = new LinkedList<String>();
        try {
            toDelete.add(uploadTestScript(scriptPath, localFilename, "txt." + ext));
            final Map<String, String> props = new HashMap<String, String>();
            props.put(SLING_RESOURCE_TYPE, resourceType);
            final String nodePath = scriptPath + "/node" + counter;
            final String nodeUrl = testClient.createNode(HTTP_BASE_URL + nodePath, props);
            toDelete.add(nodeUrl);
            return getContent(nodeUrl + ".txt", CONTENT_TYPE_DONTCARE, params);
        } finally {
            for(String url : toDelete) {
                testClient.delete(url);
            }
        }
    }

    public void assertJavascript(String expectedOutput, String jsonData, String code) throws IOException {
        assertJavascript(expectedOutput, jsonData, code, null);
    }

    /** Evaluate given code using given jsonData as the "data" object */
    public void assertJavascript(String expectedOutput, String jsonData, String code, String testInfo) throws IOException {
    	final String result = javascriptEngine.execute(code, jsonData);
        if(!result.equals(expectedOutput)) {
            fail(
                    "Expected '" + expectedOutput
                    + "' but got '" + result
                    + "' for script='" + code + "'"
                    + "' and data='" + jsonData + "'"
                    + (testInfo==null ? "" : ", test info=" + testInfo)
            );
        }
    }

    /** Return m's response body as a string, optionally limiting the length that we read
     * @param maxLength if 0, no limit
     */
    public static String getResponseBodyAsStream(HttpMethodBase m, int maxLength) throws IOException {
        final InputStream is = m.getResponseBodyAsStream();
        final StringBuilder content = new StringBuilder();
        final String charset = m.getResponseCharSet();
        final byte [] buffer = new byte[16384];
        int n = 0;
        while( (n = is.read(buffer, 0, buffer.length)) > 0) {
            content.append(new String(buffer, 0, n, charset));
            if(maxLength != 0 && content.length() > maxLength) {
                throw new IllegalArgumentException("Response body size is over maxLength (" + maxLength + ")");
            }
        }
        return content.toString();
    }
}