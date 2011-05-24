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
package org.apache.sling.launchpad.webapp.integrationtest.issues;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.testing.integration.HttpTestBase;

public class SLING2085Test extends HttpTestBase {
    
    public void testRecentRequestsEscape() throws Exception {
        final String basePath = "/" + getClass().getSimpleName() + "/" + Math.random(); 
        final String path = basePath + ".html/%22%3e%3cscript%3ealert(29679)%3c/script%3e";
        
        // POST to create node 
        {
            final PostMethod post = new PostMethod(HTTP_BASE_URL + path);
            post.setFollowRedirects(false);
            final int status = httpClient.executeMethod(post);
            assertEquals(201, status);
        }
        
        // And check that recent requests output does not contain <script>
        {
            final String content = getContent(HTTP_BASE_URL + "/system/console/requests?index=1", CONTENT_TYPE_HTML);
            final String scriptTag = "<script>";
            assertFalse("Content should not contain '" + scriptTag + "'", content.contains(scriptTag));
        }
    }
}
