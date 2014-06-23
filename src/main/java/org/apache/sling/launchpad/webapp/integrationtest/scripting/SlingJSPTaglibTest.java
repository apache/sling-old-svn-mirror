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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;

import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test of the new taglibs. Sets up a Sling instance, posts a simple
 * Test JSP and page to it and then checks to make sure the page responds with a
 * 200 response.
 */
public class SlingJSPTaglibTest extends RenderingTestBase {
	private static final Logger log = LoggerFactory
			.getLogger(SlingJSPTaglibTest.class);
	private String testPage;

	protected void setUp() throws Exception {
		super.setUp();
		log.info("setUp");

		final String testRootPath = HTTP_BASE_URL + "/"
				+ getClass().getSimpleName() + "/" + System.currentTimeMillis();
		log.info("Creating testing content...");
		Map<String, String> props = new HashMap<String, String>() {
			{
				this.put("jcr:primaryType", "nt:unstructured");
				this.put("sling:resourceType", "integration-test/taglib-test");
			}
		};
		testPage = testClient.createNode(testRootPath
				+ SlingPostConstants.DEFAULT_CREATE_SUFFIX, props);

		log.info("Creating testing component...");
		testClient.mkdirs(HTTP_BASE_URL, "/apps/integration-test/taglib-test");
		testClient.upload(
				HTTP_BASE_URL
						+ "/apps/integration-test/taglib-test/taglib-test.jsp",
				SlingJSPTaglibTest.class.getClassLoader().getResourceAsStream(
						"integration-test/taglib-test.jsp"));

		log.info("Initialization successful");
	}

	/**
	 * Tests the taglib.
	 * 
	 * @throws Exception
	 */
	public void testTaglib() throws Exception {
		log.info("testTaglib");

		log.info("Executing content check");

		final String content = getContent(testPage + ".html", CONTENT_TYPE_HTML);
		log.info(content);
		assertContains(content, "All Tests Succeeded");
		
		// tests for the encoding stuff
		assertContains(content, "HTML_ENCODE:&amp;amp&#x3b;Hello World&#x21;&lt;script&gt;&lt;&#x2f;script&gt;");
		assertContains(content, "DEFAULT:&amp;amp&#x3b;Hello World&#x21;&lt;script&gt;&lt;&#x2f;script&gt;");
		assertContains(content, "EL_VALUE:I&#x27;m Awesome&#x21;&#x21;");
		assertContains(content, "BODY_CONTENT:&amp;copy&#x3b;Body Content");
		assertContains(content, "BODY_CONTENT_FALLBACK:1");

		log.info("testTaglib - TEST SUCCESSFUL");
	}

}
