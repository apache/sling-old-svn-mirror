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
package org.apache.sling.launchpad.webapp.integrationtest.xproc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.launchpad.webapp.integrationtest.RenderingTestBase;

/**
 * Integration tests concerning XSLT transfomations
 * over XProc pipelines.
 * @see http://www.w3.org/TR/xproc/
 */
public class XsltTest extends RenderingTestBase {
	
	private final String random = getClass().getSimpleName() + String.valueOf(System.currentTimeMillis());
	
	/**
	 * Prepare the tests uploading a Xpl script and
	 * a couple of XSLT stylesheets.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// Upload xpl pipeline
		final String scriptPath = "/apps/" + random;
		testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
		urlsToDelete.add(WEBDAV_BASE_URL + scriptPath);
		final String urlXplScript = uploadTestScript(scriptPath, "xproc/xslt/html.xpl", "html.xpl");
		urlsToDelete.add(urlXplScript);
		
		// XSLT stylesheets
		final String xsltsPath = "/xsl";
		testClient.mkdirs(WEBDAV_BASE_URL, xsltsPath);
		urlsToDelete.add(WEBDAV_BASE_URL + xsltsPath);
		final String urlXslContent = uploadTestScript(xsltsPath, "xproc/xslt/test-content.xslt", "test-content.xslt");
		urlsToDelete.add(urlXslContent);
		final String urlXslHtml = uploadTestScript(xsltsPath, "xproc/xslt/test-html.xslt", "test-html.xslt");
		urlsToDelete.add(urlXslHtml);
	}
	
	/**
	 * Xpl (XProc) pipeline which XML source is a
	 * static XML file.
	 */
	public void testStaticXml() throws IOException {
		
		// New "xpl" resource
		final String mokeNodePath = HTTP_BASE_URL + "/sling-test/" + random + "/static_xml";
		Map<String, String> mokeNodeProps = new HashMap<String, String>();
		mokeNodeProps.put("sling:resourceType", random);
		testClient.createNode(mokeNodePath, mokeNodeProps);
		urlsToDelete.add(mokeNodePath);
		
		// The pipeline source: a static XML
		final String staticXmlPath = "/sling-test/" + random;
		final String urlStaticXml = uploadTestScript(staticXmlPath, "xproc/xslt/static_xml.xml", "static_xml.xml");
		urlsToDelete.add(urlStaticXml);
		
		// Render content and assertions
		final String content = getContent(mokeNodePath + ".html", CONTENT_TYPE_HTML);
		assertContains(content, "static content");
	}
	
	/**
	 * Xpl (XProc) pipeline which XML source is a
	 * dynamically generated XML (through a script,
	 * for instance).
	 */
	public void testDynamicXml() throws IOException {
		
		// New "xpl" resource
		final String mokeNodePath = HTTP_BASE_URL + "/sling-test/" + random + "/dynamic_xml";
		Map<String, String> mokeNodeProps = new HashMap<String, String>();
		mokeNodeProps.put("title", "dynamic_xml");
		mokeNodeProps.put("sling:resourceType", random + "/dynamic_xml");
		mokeNodeProps.put("sling:resourceSuperType", random);
		testClient.createNode(mokeNodePath, mokeNodeProps);
		urlsToDelete.add(mokeNodePath);
		
		// The pipeline source: a script generating XML
		final String scriptPath = "/apps/" + random + "/dynamic_xml";
		testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
		urlsToDelete.add(WEBDAV_BASE_URL + scriptPath);
		final String urlScript = uploadTestScript(scriptPath, "xproc/xslt/dynamic_xml.xml.esp", "dynamic_xml.xml.esp");
		urlsToDelete.add(urlScript);
		
		// Render content and assertions
		final String content = getContent(mokeNodePath + ".html", CONTENT_TYPE_HTML);
		assertContains(content, "dynamic content");
		
	}
	
	/**
	 * Xpl (XProc) pipeline which XML source is
	 * the node´s XML document view (default behaviour).
	 */
	public void testNoXml() throws IOException {
		
		// New "xpl" resource
		final String mokeNodePath = HTTP_BASE_URL + "/sling-test/" + random + "/no_xml";
		Map<String, String> mokeNodeProps = new HashMap<String, String>();
		mokeNodeProps.put("title", "no_xml");
		mokeNodeProps.put("sling:resourceType", random);
		testClient.createNode(mokeNodePath, mokeNodeProps);
		urlsToDelete.add(mokeNodePath);
		
		// Render content and assertions
		final String content = getContent(mokeNodePath + ".xml", CONTENT_TYPE_XML);
		assertContains(content, "no_xml");
		
	}
	
}
