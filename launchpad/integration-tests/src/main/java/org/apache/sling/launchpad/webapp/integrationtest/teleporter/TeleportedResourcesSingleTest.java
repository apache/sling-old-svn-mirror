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
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;

/** Verify that specified resources are included in the
 *  teleported bundle, using the single resource notation.
 */
public class TeleportedResourcesSingleTest {

    @Rule
    public final TeleporterRule teleporter = 
        TeleporterRule
            .forClass(getClass(), "Launchpad")
            .withResources("/teleporter/file2.txt","/teleporter/subfolder/thirdfile.txt");
    
    private void assertResource(String path, String expected) throws IOException {
        final InputStream is = getClass().getResourceAsStream(path);
        assertNotNull("Expecting resource " + path, is);
        try {
            final StringWriter w = new StringWriter();
            IOUtils.copy(is, w);
            assertTrue("Expecting in content:" + expected, w.toString().contains(expected));
        } finally {
            is.close();
        }
    }
    
    @Test
    public void testFile1() throws IOException {
        assertNull(getClass().getResource("/teleporter/file1.txt"));
    }
    
    @Test
    public void testFile2() throws IOException {
        assertResource("/teleporter/file2.txt", "And this is 2");
    }
    
    @Test
    public void testFile3() throws IOException {
        assertResource("/teleporter/subfolder/thirdfile.txt", "The third file");
    }
}
