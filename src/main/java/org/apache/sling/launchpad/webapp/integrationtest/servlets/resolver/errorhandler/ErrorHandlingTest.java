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
import org.apache.sling.launchpad.webapp.integrationtest.JspTestBase;


/** Test the sling error handling mechanism http://sling.apache.org/site/errorhandling.html*/
public class ErrorHandlingTest extends JspTestBase {

	public final static String TEST_ROOT = "/apps";
 
	public static final String ERROR_HANDLER_PATH = "/apps/sling/servlet/errorhandler";

	private static final String NOT_EXISTING_NODE_PATH="/notExisting";

	private String testNodePath;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		scriptPath = TEST_ROOT;
		testClient.mkdirs(HTTP_BASE_URL, "/apps/sling/servlet/errorhandler");
		uploadTestScript("servlets/errorhandler/404.jsp", "sling/servlet/errorhandler/404.jsp");
		
		testNodePath = testClient.createNode(HTTP_BASE_URL + TEST_ROOT, null);
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

}
