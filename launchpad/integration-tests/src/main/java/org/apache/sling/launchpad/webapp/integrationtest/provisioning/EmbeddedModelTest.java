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
package org.apache.sling.launchpad.webapp.integrationtest.provisioning;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Verify that the provisioning model used to build this instance is available 
 *  as a Launchpad resource.
 */
public class EmbeddedModelTest {
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    public static final String MODEL_RESOURCE_PATH = "/resources/provisioning/model.txt";
    
    private String modelContent;
    
    @Before
    public void setup() throws IOException {
        final InputStream modelStream = teleporter.getService(LaunchpadContentProvider.class).getResourceAsStream(MODEL_RESOURCE_PATH);
        assertNotNull("Expecting embedded model resource at " + MODEL_RESOURCE_PATH, modelStream);
        try {
            modelContent = new String(IOUtils.toByteArray(modelStream));
        } finally {
            modelStream.close();
        }
    }
    
    @Test
    public void testLaunchpadFeature() {
        assertTrue(modelContent.contains("[feature name=:launchpad]"));
    }
    
    @Test
    public void testBootFeature() {
        assertTrue(modelContent.contains("[feature name=:boot]"));
    }
}
