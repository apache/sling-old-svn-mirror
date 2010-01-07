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
package org.apache.sling.commons.osgi.bundleversion;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osgi.framework.Version;

public class FileBundleVersionInfoTest {
    
    private File getTestJar(String namePrefix) {
        final File testJarsFolder = new File(System.getProperty("test.jars.folder"));
        for(String jar : testJarsFolder.list()) {
            if(jar.startsWith(namePrefix)) {
                return new File(testJarsFolder, jar);
            }
        }
        fail("No test jar found with prefix " + namePrefix + " in " + testJarsFolder.getAbsolutePath());
        return null;
    }
    
    @Test
    public void testSlingApiJar() throws IOException {
        final File testJar = getTestJar("org.apache.sling.api");
        final BundleVersionInfo<?> vi = new FileBundleVersionInfo(testJar);
        assertEquals("org.apache.sling.api", vi.getBundleSymbolicName());
        assertEquals(vi.getVersion(), new Version("2.0.6"));
        assertFalse(vi.isSnapshot());
        assertEquals(1250080966786L, vi.getBundleLastModified());
        assertTrue(vi.isBundle());
        final Object src = vi.getSource();
        assertTrue(src instanceof File);
        assertEquals(testJar.getAbsolutePath(), ((File)src).getAbsolutePath());
    }
    
    @Test
    public void testSlf4jJar() throws IOException {
        final File testJar = getTestJar("jcr");
        final BundleVersionInfo<?> vi = new FileBundleVersionInfo(testJar);
        assertFalse(vi.isBundle());
        assertNull(vi.getBundleSymbolicName());
        assertNull(vi.getVersion());
        assertEquals(BundleVersionInfo.BND_LAST_MODIFIED_MISSING, vi.getBundleLastModified());
        final Object src = vi.getSource();
        assertTrue(src instanceof File);
        assertEquals(testJar.getAbsolutePath(), ((File)src).getAbsolutePath());
    }
}
