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
import java.util.HashMap;
import java.util.Map;

/** Utility to manage test nodes */
public class HttpTestNode {
    public final String testText;
    public final String nodeUrl;
    public final String resourceType;
    public final String scriptPath;
    private final SlingIntegrationTestClient testClient;

    public HttpTestNode(SlingIntegrationTestClient testClient, String parentPath, Map<String, String> properties) throws IOException {
        this.testClient = testClient;
        if(properties == null) {
            properties = new HashMap<String, String>();
        }
        testText = "This is a test node " + System.currentTimeMillis();
        properties.put("text", testText);
        nodeUrl = testClient.createNode(parentPath + HttpTestBase.SLING_POST_SERVLET_CREATE_SUFFIX, properties);
        resourceType = properties.get(HttpTestBase.SLING_RESOURCE_TYPE);
        scriptPath = "/apps/" + (resourceType == null ? "nt/unstructured" : resourceType);
        testClient.mkdirs(HttpTestBase.WEBDAV_BASE_URL, scriptPath);
    }

    public void delete() throws IOException {
        testClient.delete(nodeUrl);
    }

}