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
package org.apache.sling.launchpad.webapp.jsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import org.apache.sling.commons.testing.integration.HttpTestBase;

/**
 * Verifies that JSP Tag files function correctly
 *
 */
public class TagFileTest extends HttpTestBase {
    
    private static String TAG_FILE_SCRIPT = 
            "<%@ taglib prefix=\"t\" uri=\"https://sling.apache.org/tags/test/1.0\" %>\n" + 
            "\n" + 
            "<t:test/>";
    
    /**
     * Tests a tag file packaged in a jar file is properly executed
     */
    public void testTagFileDeployedInBundle() throws IOException {
        
        if ( !isBundleVersionAtLeast("org.apache.sling.scripting.jsp", "2.3.1") ) {
            System.out.println("Bundle version is too old, skipping");
            return;
        }
        
        testClient.createNode(HTTP_BASE_URL + "/content/tagtest", Collections.singletonMap("sling:resourceType", "sling/test/tagfile"));
        testClient.mkdirs(HTTP_BASE_URL, "/apps/sling/test/tagfile");
        testClient.upload(HTTP_BASE_URL + "/apps/sling/test/tagfile/html.jsp", new ByteArrayInputStream(TAG_FILE_SCRIPT.getBytes(Charset.forName("UTF-8"))));
        
        String content = getContent(HTTP_BASE_URL + "/content/tagtest.html", CONTENT_TYPE_DONTCARE, null, 200);
        assertEquals("Incorrect output from rendering script", "TEST OUTPUT", content.trim());
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        testClient.delete(HTTP_BASE_URL + "/content/tagtest");
        testClient.delete(HTTP_BASE_URL + "/apps/sling/test/tagfile");
    }

}
