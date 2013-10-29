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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.resolver.errorhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;

/** Test the sling error handling mechanism http://sling.apache.org/site/errorhandling.html*/
public class ErrorHandlingTest extends RenderingTestBase {

	public final static String TEST_ROOT = "/apps";

	public final static String THROW_ERROR_PATH= "servlets/errorhandler/testErrorHandler";

	public final static String THROW_ERROR_PAGE= "testErrorHandler.jsp";

	public static final String ERROR_HANDLER_PATH = "/apps/sling/servlet/errorhandler";

	private static final String NOT_EXISTING_NODE_PATH="/notExisting";

	private static final String SELECTOR_500 =".500";

	private static final String SELECTOR_401 =".401";

	private static final String SELECTOR_THROWABLE =".throwable";

	private String testNodePath;
	
	/** Need some retries as there might be some latency when installing error handler scripts */  
	public static final int RETRY_MAX_TIME_SEC = 10;
    public static final int RETRY_INTERVAL_MSEC = 500;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		scriptPath = TEST_ROOT;
		testClient.mkdirs(HTTP_BASE_URL, ERROR_HANDLER_PATH);
		testClient.mkdirs(HTTP_BASE_URL, TEST_ROOT+"/"+THROW_ERROR_PATH);
		uploadTestScript("servlets/errorhandler/404.jsp", "sling/servlet/errorhandler/404.jsp");
		uploadTestScript("servlets/errorhandler/Throwable.jsp", "sling/servlet/errorhandler/Throwable.jsp");
		uploadTestScript("servlets/errorhandler/500.jsp", "sling/servlet/errorhandler/500.jsp");
		uploadTestScript("servlets/errorhandler/401.jsp", "sling/servlet/errorhandler/401.jsp");
		uploadTestScript(THROW_ERROR_PATH+"/"+THROW_ERROR_PAGE, THROW_ERROR_PATH+"/"+THROW_ERROR_PAGE);
 
		final Map<String, String> props = new HashMap<String, String>();
		props.put(SLING_RESOURCE_TYPE, TEST_ROOT+"/"+THROW_ERROR_PATH);
        testNodePath = testClient.createNode(HTTP_BASE_URL + TEST_ROOT + "/testNode", props);
	}

	@Override
	protected void tearDown() throws Exception {
        testClient.delete(HTTP_BASE_URL + ERROR_HANDLER_PATH);
        testClient.delete(HTTP_BASE_URL + TEST_ROOT + "/" + THROW_ERROR_PATH);
        testClient.delete(testNodePath);
        super.tearDown();
	}
	
	
    private void assertWithRetries(String url, int expectedStatus, String expectedContent) throws Throwable {
        assertWithRetries(url, expectedStatus, expectedContent, HTTP_METHOD_GET, null);
    }
    
	private void assertWithRetries(String url, int expectedStatus, String expectedContent, String httpMethod, List<NameValuePair> params) throws Throwable {
	    final long endTime = System.currentTimeMillis() + RETRY_MAX_TIME_SEC * 1000L;
	    Throwable caught = null;
	    while(System.currentTimeMillis() < endTime) {
	        try {
                caught = null;
	            assertContains(getContent(url, CONTENT_TYPE_HTML,params,expectedStatus, httpMethod), expectedContent);
	            break;
	        } catch(Throwable t) {
	            caught = t;
	            try {
	                Thread.sleep(RETRY_INTERVAL_MSEC);
	            } catch(InterruptedException ignored) {
	            }
	        }
	    }
	    
	    if(caught != null) {
	        throw caught;
	    }
	}

	public void test_404_errorhandling() throws Throwable{	
		final String expected = "No resource found (404) - custom error page";
		final String url =  testNodePath+NOT_EXISTING_NODE_PATH +".html";	
        assertWithRetries(url, 200, expected);
	}

	public void test_500_errorhandling() throws Throwable{	
		final String expected = "Internal Server Error (500) - custom error page";
		final String url =  testNodePath +SELECTOR_500+".html";
        assertWithRetries(url, 500, expected);
 	}

	public void test_401_errorhandling() throws Throwable{
		final String expected = "401 Unauthorized - custom error page";
		final String url =  testNodePath +SELECTOR_401+".html"; 
        assertWithRetries(url, 401, expected);
	}

	public void test_throwable_errorhandling() throws Throwable{	
		final String expected = "Exception thrown - custom error page";
		final String url =  testNodePath +SELECTOR_THROWABLE+".html";
        assertWithRetries(url, 200, expected);
 	}
	
	public void test_500_errorhandling_POST_operation() throws Throwable{	
		final String expected = "Internal Server Error (500) - custom error page";
		final String url =  testNodePath +".html"; 
		uploadTestScript(THROW_ERROR_PATH+"/"+"POST.jsp", THROW_ERROR_PATH+"/"+"POST.jsp");
        assertWithRetries(url, 500, expected, HTTP_METHOD_POST, null);
  	}
	
	public void test_errorhandling_POST_operation_SlingPostServlet() throws Throwable{
		final String expected = "Exception thrown - custom error page";
		final String url =  testNodePath +".html";
		List <NameValuePair> params=new ArrayList<NameValuePair>();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION,"notExistingOperation"));
        params.add(new NameValuePair(SlingPostConstants.RP_SEND_ERROR,"true"));        
        assertWithRetries(url, 500, expected, HTTP_METHOD_POST, params);
	}

}
