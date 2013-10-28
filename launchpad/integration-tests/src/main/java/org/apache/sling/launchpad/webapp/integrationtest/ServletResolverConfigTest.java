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

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify that the ServletResolver cache is disabled for testing */
public class ServletResolverConfigTest extends HttpTestBase {

    public static final String CONFIG_PID = "org.apache.sling.servlets.resolver.SlingServletResolver";
    public static final String GET_CONFIG_PATH = "/testing/GetConfigServlet.tidy.json/" + CONFIG_PID;
    public static final String CONFIG_PROP = "servletresolver.cacheSize";
    
    public void testCacheDisabled() throws Exception {
        final String content = getContent(HTTP_BASE_URL + GET_CONFIG_PATH, CONTENT_TYPE_JSON);
        final JSONObject json = new JSONObject(content);
        final int cacheSize = json.getJSONObject("properties").getInt(CONFIG_PROP);
        if(cacheSize != 0) {
            fail(
                    "ServletResolver cache size should be set to zero for testing, current value=" + cacheSize
                    + " *** THIS MIGHT CAUSE MANY OTHER TESTS TO FAIL!! ***"
                    + " (" + CONFIG_PID + "/" + CONFIG_PROP + ")"
                    );
        }
    }
}