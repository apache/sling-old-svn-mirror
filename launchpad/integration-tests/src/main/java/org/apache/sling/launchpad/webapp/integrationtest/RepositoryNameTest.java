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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;

/** Verify the repository name, to make sure we're testing the right one */
public class RepositoryNameTest extends HttpTestBase {
    
    /** We use the same property that Sling uses to switch between repositories */
    public static final String RUN_MODE_PROP = "sling.run.modes";
    public static final String DEFAULT_RUN_MODE = "jackrabbit";
    
    public static final Map<String, String> RUNMODE_TO_NAME = new HashMap<String, String>();
    
    static {
        RUNMODE_TO_NAME.put("jackrabbit", "Jackrabbit");
        RUNMODE_TO_NAME.put("oak", "Apache Jackrabbit Oak");
    }
    
    public void testName() throws Exception {
        final String runMode = System.getProperty(RUN_MODE_PROP, DEFAULT_RUN_MODE);
        final String expectedName = RUNMODE_TO_NAME.get(runMode);
        assertNotNull("Expecting to have a repository name for run mode " + runMode, expectedName);
        
        final String path = "/testing/RepositoryDescriptors.json";
        final JSONObject json = new JSONObject(getContent(HTTP_BASE_URL + path, CONTENT_TYPE_JSON));
        final String key = "jcr.repository.name";
        final String actualName = json.getJSONObject("descriptors").getString(key);
        if(!expectedName.equals(actualName)) {
            fail(
                "Repository descriptor '" + key + "' value '" + actualName + "' does not match expected value '" + expectedName + "'. "
                + "Note that this test uses the " + RUN_MODE_PROP + " system property to select the expected value."
            );
        }
    }
}
