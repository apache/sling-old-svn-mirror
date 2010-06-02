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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test resource supertypes */
public class ResourceSuperTypeTest extends RenderingTestBase {
	public static final String ST_PROP = "sling:resourceSuperType";
	private List<String> toDelete = new ArrayList<String>();
	private String testPath;
	private int counter;
	private String uniqueId;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		toDelete.clear();
		uniqueId = "" + (++counter) + System.currentTimeMillis();
		testPath = "/" + ResourceSuperTypeTest.class.getSimpleName() + uniqueId;
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		testClient.delete(WEBDAV_BASE_URL + testPath);
		for(String url : toDelete) {
			testClient.delete(url);
		}
	}
	
	/** Test setting the sling:resourceSuperType directly on a node */
	public void testSuperTypeOnResource() throws Exception {
		final String superType = getClass().getSimpleName() + uniqueId + "SuperType";
		String superTypeScriptPath = "/apps/" + superType;
		testClient.mkdirs(WEBDAV_BASE_URL, superTypeScriptPath);
		
		final Map<String, String> props = new HashMap<String, String>();
		props.put(ST_PROP, superType);
		final TestNode tn= new TestNode(HTTP_BASE_URL + testPath, props);
		
		// Without any scripts -> default rendering
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "dumped by HtmlRendererServlet");
		
		// Add supertype script and check that it is used
		toDelete.add(uploadTestScript(superTypeScriptPath, "rendering-test.esp", "html.esp"));
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "ESP template");
		
		// Add own resource type, but no script -> no change
		final String myType = getClass().getSimpleName() + uniqueId + "MyResourceType";
		String myTypeScriptPath = "/apps/" + myType;
		testClient.mkdirs(WEBDAV_BASE_URL, myTypeScriptPath);
		props.put("sling:resourceType", myType);
		testClient.createNode(tn.nodeUrl, props);
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "ESP template");
		
		// Add script for own resource type and check that it is used
		toDelete.add(uploadTestScript(myTypeScriptPath, "rendering-test-2.esp", "html.esp"));
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "Template #2 for ESP tests");
	}
	
	/** Test setting the sling:resourceSuperType on the folder that contains the scripts
	 * 	of the node's own resource type */
	public void testSuperTypeOnScriptFolder() throws Exception {
		
		final String superType = getClass().getSimpleName() + uniqueId + "SuperType";
		String superTypeScriptPath = "/apps/" + superType;
		testClient.mkdirs(WEBDAV_BASE_URL, superTypeScriptPath);
		
		final String myType = getClass().getSimpleName() + uniqueId + "MyResourceType";
		String myTypeScriptPath = "/apps/" + myType;
		{
			// Set ST_PROP on the folder node that contains
			// the myType scripts
			final Map<String, String> props = new HashMap<String, String>();
			props.put("jcr:primaryType", "sling:Folder");
			props.put(ST_PROP, superType);
			testClient.createNode(HTTP_BASE_URL + myTypeScriptPath, props);
		}
		
		final Map<String, String> props = new HashMap<String, String>();
		props.put("sling:resourceType", myType);
		final TestNode tn= new TestNode(HTTP_BASE_URL + testPath, props);
		
		// Without any scripts -> default rendering
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "dumped by HtmlRendererServlet");
		
		// Add supertype script and check that it is used
		toDelete.add(uploadTestScript(superTypeScriptPath, "rendering-test.esp", "html.esp"));
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "ESP template");
		
		// Add script for own resource type and check that it is used
		toDelete.add(uploadTestScript(myTypeScriptPath, "rendering-test-2.esp", "html.esp"));
		assertContains(getContent(tn.nodeUrl + ".html", CONTENT_TYPE_HTML), "Template #2 for ESP tests");
	}
}
