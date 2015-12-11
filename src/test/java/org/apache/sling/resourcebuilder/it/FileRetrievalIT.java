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
package org.apache.sling.resourcebuilder.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.resourcebuilder.test.ResourceAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Verify that our file structure is correct,
 *  by creating a file and retrieving it via
 *  a Sling request. 
 */
public class FileRetrievalIT {
    
    @Rule
    public final TeleporterRule teleporter = 
        TeleporterRule
        .forClass(getClass(), "RBIT_Teleporter")
        .withResources("/files/");
    
    private TestEnvironment E;
    private ResourceAssertions A;

    @Before
    public void setup() throws LoginException, PersistenceException {
        E = new TestEnvironment(teleporter);
        A = new ResourceAssertions(E.testRootPath, E.resolver);
    }
    
    @After
    public void cleanup() throws PersistenceException {
        E.cleanup();
    }
    
    @Test
    public void createAndeRtrieveFile() throws IOException, ServletException {
        final String expected = "yes, it worked";
        final long startTime = System.currentTimeMillis();
        final String mimeType = "application/javascript";
        
        E.builder
            .resource("somefolder")
            .file("the-model.js", getClass().getResourceAsStream("/files/models.js"))
            .commit();
        
        final Resource r = A.assertFile("somefolder/the-model.js", mimeType, expected, -1L);
        
        final ResourceMetadata meta = r.getResourceMetadata();
        assertTrue("Expecting a last modified time >= startTime", meta.getModificationTime() >= startTime);
        assertEquals("Expecting the correct mime-type", mimeType, meta.getContentType());

        final InputStream is = r.adaptTo(InputStream.class);
        assertNotNull("Expecting InputStream for file resource " + r.getPath(), is);
        try {
            final String content = A.readFully(is);
            assertTrue("Expecting [" + expected + "] in content", content.contains(expected));
        } finally {
            is.close();
        }
    }
}