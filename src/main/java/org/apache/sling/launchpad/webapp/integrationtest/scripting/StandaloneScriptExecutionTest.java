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
package org.apache.sling.launchpad.webapp.integrationtest.scripting;

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class StandaloneScriptExecutionTest extends HttpTestBase {
	/** Use the StandaloneScriptExecutionServlet to verify that scripts
	 * 	can be executed in the simplest way from java code (SLING-1423)
	 */
	public void testScriptExecution() throws Exception {
		final String scriptPath = TEST_PATH + "/" + getClass().getSimpleName();
		final String script = "standalone-test.ecma";
		testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
		final String scriptUrl = uploadTestScript(scriptPath, script, script);
		
		{
			final String content = getContent(scriptUrl, CONTENT_TYPE_DONTCARE);
			assertTrue("Expecting script URL to return raw script contents (" + content + ")", 
					content.contains("TEST_SCRIPT"));
		}
		
		{
			final String execSuffix = ".StandaloneScriptExecutionServlet.txt";
			final String expect = "2+2=4";
			final String content = getContent(scriptUrl + execSuffix, CONTENT_TYPE_PLAIN);
			assertTrue("Expecting execution URL to return " + expect + " (returns '" + content + "')", 
					content.contains(expect));
		}
		
		testClient.delete(scriptUrl);
	}
}
