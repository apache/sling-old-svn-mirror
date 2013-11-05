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

import static org.junit.Assert.assertEquals;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTest;
import org.apache.sling.commons.testing.junit.categories.JackrabbitOnly;
import org.apache.sling.commons.testing.junit.categories.OakOnly;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/** Verify the repository name, to make sure we're testing the right one */
public class RepositoryNameTest {

    private final HttpTest H = new HttpTest();
    
    @Before
    public void setup() throws Exception {
        H.setUp();
    }
    
    @After
    public void cleanup() throws Exception {
        H.tearDown();
    }
    
    private void assertRepositoryName(String expectedName) throws Exception {
        final String path = "/testing/RepositoryDescriptors.json";
        final JSONObject json = new JSONObject(H.getContent(HttpTest.HTTP_BASE_URL + path, HttpTest.CONTENT_TYPE_JSON));
        final String key = "jcr.repository.name";
        final String actualName = json.getJSONObject("descriptors").getString(key);
        assertEquals("Expecting the correct value for " + key, expectedName, actualName);
    }
    
    @Category(JackrabbitOnly.class)
    @Test
    public void checkJackrabbitName() throws Exception {
        assertRepositoryName("Jackrabbit");
    }
    
    @Category(OakOnly.class)
    @Test
    public void checkOakName() throws Exception {
        assertRepositoryName("Apache Jackrabbit Oak");
    }
}