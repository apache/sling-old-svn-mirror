/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
 package org.apache.sling.launchpad.webapp.integrationtest;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.servlets.post.SlingPostConstants;

/** Base abstract test for creating a Node and rendering it using scripts in
 *  various supported languages, using slingResourceType-based
 *  script resolution
 */
public abstract class AbstractSlingResourceTypeRenderingTest extends RenderingTestBase {
    
    protected String slingResourceType;
    protected String secondFolderOfContentPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set test values
        slingResourceType = "integration-test/srt." + System.currentTimeMillis();
        testText = "This is a test " + System.currentTimeMillis();

        // create the test node, under a path that's specific to this class to allow collisions
        secondFolderOfContentPath = "" + System.currentTimeMillis();
        final String url = HTTP_BASE_URL + "/" + getClass().getSimpleName() + "/" + secondFolderOfContentPath + SlingPostConstants.DEFAULT_CREATE_SUFFIX;
        final Map<String,String> props = new HashMap<String,String>();
        props.put("sling:resourceType", slingResourceType);
        props.put("text", testText);
        displayUrl = testClient.createNode(url, props);

        // the rendering script goes under /apps in the repository
        scriptPath = "/apps/" + slingResourceType;
        testClient.mkdirs(WEBDAV_BASE_URL, scriptPath);
    }
}
