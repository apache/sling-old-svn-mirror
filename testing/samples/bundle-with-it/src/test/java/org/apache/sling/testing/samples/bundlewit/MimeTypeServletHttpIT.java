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
package org.apache.sling.testing.samples.bundlewit;

import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.rules.SlingBaseInstanceRule;
import org.apache.sling.testing.samples.bundlewit.impl.MimeTypeServlet;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.ClassRule;
import org.junit.Test;

/** HTTP test of the MimeTypeServlet provided
 *  by this bundle.
 */
public class MimeTypeServletHttpIT {

    @ClassRule
    public static SlingBaseInstanceRule slingInstanceRule = new SlingBaseInstanceRule();
    
    private void assertMimeType(String path, String expected) throws Exception {
        SlingClient client = slingInstanceRule.getAdminClient();
        SlingHttpResponse response = client.doGet(path + ".mimetype.txt", 200);
        response.checkContentContains(expected);
    }
    
    @Test
    public void htmlResource() throws Exception {
        assertMimeType("/index.html", MimeTypeServlet.PREFIX + "text/html");
    }
    
    @Test
    public void noMimeTypeResource() throws Exception {
        assertMimeType("/tmp", MimeTypeServlet.PREFIX + "text/plain");
    }
}