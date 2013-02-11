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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.launchpad.webapp.integrationtest.JspTestBase;
import org.apache.sling.servlets.post.SlingPostConstants;


/** Test the sling error handling mechanism http://sling.apache.org/site/errorhandling.html*/
public class ErrorHandlingTest extends JspTestBase {

	public final static String TEST_ROOT = "/apps";

	public final static String THROW_ERROR_PATH= "servlets/errorhandler/testErrorHandler";

	public final static String THROW_ERROR_PAGE= "testErrorHandler.jsp";

	public static final String ERROR_HANDLER_PATH = "/apps/sling/servlet/errorhandler";

	private static final String NOT_EXISTING_NODE_PATH="/notExisting";

	private static final String SELECTOR_500 =".500";

	private static final String SELECTOR_401 =".401";

	private static final String SELECTOR_THROWABLE =".throwable";

	private String testNodePath;

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
		testNodePath = testClient.createNode(HTTP_BASE_URL + TEST_ROOT, props);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		testClient.delete(HTTP_BASE_URL + TEST_ROOT);
	}

	public void test_404_errorhandling() throws IOException{	
		final String expected = "No resource found (404) - custom error page";
		final String url =  testNodePath+NOT_EXISTING_NODE_PATH +".html";	
		assertContains(getContent(url, CONTENT_TYPE_HTML,null,200), expected);
	}

	public void test_500_errorhandling() throws IOException{	
		final String expected = "Internal Server Error (500) - custom error page";
		final String url =  testNodePath +SELECTOR_500+".html"; 
		assertContains(getContent(url, CONTENT_TYPE_HTML,null,500), expected);
		//assertNotContains(getContent(url, CONTENT_TYPE_HTML,null,200), "All good");
	}

	public void test_401_errorhandling() throws IOException{
		final String expected = "401 Unauthorized - custom error page";
		final String url =  testNodePath +SELECTOR_401+".html"; 
		assertContains(getContent(url, CONTENT_TYPE_HTML,null,401), expected);
		//assertNotContains(getContent(url, CONTENT_TYPE_HTML,null,401), "All good");	
	}

	public void test_throwable_errorhandling() throws IOException{	
		final String expected = "Exception thrown - custom error page";
		final String url =  testNodePath +SELECTOR_THROWABLE+".html";
		assertContains(getContent(url, CONTENT_TYPE_HTML,null,200), expected);
		assertNotContains(getContent(url, CONTENT_TYPE_HTML,null,200), "All good");
	}
	
	public void test_500_errorhandling_POST_operation() throws IOException{	
		final String expected = "Internal Server Error (500) - custom error page";
		final String url =  testNodePath +".html"; 
		uploadTestScript(THROW_ERROR_PATH+"/"+"POST.jsp", THROW_ERROR_PATH+"/"+"POST.jsp");
		assertContains(getContent(url, CONTENT_TYPE_HTML,null,500,HTTP_METHOD_POST), expected);
		//assertNotContains(getContent(url, CONTENT_TYPE_HTML,null,200), "All good");
 	}
	
	public void test_errorhandling_POST_operation_SlingPostServlet() throws IOException{
		final String expected = "Exception thrown - custom error page";
		final String url =  testNodePath +".html";
		List <NameValuePair> params=new ArrayList<NameValuePair>();
        params.add(new NameValuePair(SlingPostConstants.RP_OPERATION,"notExistingOperation"));
        params.add(new NameValuePair(SlingPostConstants.RP_SEND_ERROR,"true"));        
        assertContains(getContent(url, CONTENT_TYPE_HTML,params,500,HTTP_METHOD_POST), expected);
	}

}
