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
package org.apache.sling.launchpad.webapp.integrationtest.scripting.htl;

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.commons.testing.integration.HttpTestBase;

public class HtlTest extends HttpTestBase {
    
    protected void tearDown() throws IOException {
        testClient.delete(HTTP_BASE_URL + "/apps/sling/test/htl");
        testClient.delete(HTTP_BASE_URL + "/content/htl");
    }

    public void testScriptWithJsUseBean() throws IOException {

        testClient.mkdirs(HTTP_BASE_URL, "/apps/sling/test/htl/js");
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/htl/js/js.html", getClass().getResourceAsStream("/integration-test/htl/js.html"));
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/htl/js/script.js", getClass().getResourceAsStream("/integration-test/htl/script.js"));
        
        testClient.createNode(HTTP_BASE_URL + "/content/htl/js-use-bean", Collections.singletonMap("sling:resourceType", "sling/test/htl/js"));
        
        String content = getContent(HTTP_BASE_URL + "/content/htl/js-use-bean.html", CONTENT_TYPE_DONTCARE, null, 200);
        
        assertTrue("Expected content to contain 'from-js-use-script'", content.contains("from-js-use-script"));
    }
    
    public void testScriptWithJavaUseBean() throws IOException {
        
        testClient.mkdirs(HTTP_BASE_URL, "/apps/sling/test/htl/java");
        testClient.mkdirs(HTTP_BASE_URL, "/content/htl");
        
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/htl/java/java.html", getClass().getResourceAsStream("/integration-test/htl/java.html"));
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/htl/java/Bean.java", getClass().getResourceAsStream("/integration-test/htl/Bean.java"));
        
        testClient.createNode(HTTP_BASE_URL + "/content/htl/java-use-bean", Collections.singletonMap("sling:resourceType", "sling/test/htl/java"));
        
        String content = getContent(HTTP_BASE_URL + "/content/htl/java-use-bean.html", CONTENT_TYPE_DONTCARE, null, 200);
        
        assertTrue("Expected content to contain 'from-java-use-script'", content.contains("from-java-use-bean"));
    }
    
    public void testScriptWithModelUseBean() throws IOException {
        
        testClient.mkdirs(HTTP_BASE_URL, "/apps/sling/test/htl/model");
        testClient.mkdirs(HTTP_BASE_URL, "/content/htl");
        
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/htl/model/model.html", getClass().getResourceAsStream("/integration-test/htl/model.html"));
        
        testClient.createNode(HTTP_BASE_URL + "/content/htl/model-use-bean", Collections.singletonMap("sling:resourceType", "sling/test/htl/model"));
        
        String content = getContent(HTTP_BASE_URL + "/content/htl/model-use-bean.html", CONTENT_TYPE_DONTCARE, null, 200);
        
        assertTrue("Expected content to contain 'from-sling-model'", content.contains("from-sling-model"));
    }
}
