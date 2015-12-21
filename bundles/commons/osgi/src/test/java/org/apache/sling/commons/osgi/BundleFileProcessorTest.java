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
package org.apache.sling.commons.osgi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.Test;

public class BundleFileProcessorTest {
    
    private static void closeQuietly(Closeable c) {
        try {
            c.close();
        } catch(IOException ignore) {
        }
    }
    
    @Test
    public void testBSNRenaming() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        // Just take any bundle from the maven deps as an example...
        File originalFile = getMavenArtifactFile(getMavenRepoRoot(), "com.google.guava", "guava", "15.0");

        File generatedFile = new BSNRenamer(originalFile, tempDir, "org.acme.baklava.guava").process();

        try {
            compareJarContents(originalFile, generatedFile);

            JarFile jfOrg = null;
            JarFile jfNew = null;
            try {
                    jfOrg = new JarFile(originalFile);
                    jfNew = new JarFile(generatedFile);
                    Manifest mfOrg = jfOrg.getManifest();
                    Manifest mfNew = jfNew.getManifest();

                    Attributes orgAttrs = mfOrg.getMainAttributes();
                    Attributes newAttrs = mfNew.getMainAttributes();
                    for (Object key : orgAttrs.keySet()) {
                        String orgVal = orgAttrs.getValue(key.toString());
                        String newVal = newAttrs.getValue(key.toString());

                        if ("Bundle-SymbolicName".equals(key.toString())) {
                            assertEquals("Should have recorded the original Bundle-SymbolicName",
                                    orgVal, newAttrs.getValue("X-Original-Bundle-SymbolicName"));

                            assertEquals("org.acme.baklava.guava", newVal);
                        } else {
                            assertEquals("Different keys: " + key, orgVal, newVal);
                        }
                    }
                } finally {
                    closeQuietly(jfOrg);
                    closeQuietly(jfNew);
                }

        } finally {
            assertTrue("Unable to delete temporary file", generatedFile.delete());
        }
    }

    private static void compareJarContents(File orgJar, File actualJar) throws IOException {
        JarInputStream jis1 = null;
        JarInputStream jis2 = null;
        try {
            jis1 = new JarInputStream(new FileInputStream(orgJar));
            jis2 = new JarInputStream(new FileInputStream(actualJar));
            JarEntry je1 = null;
            while ((je1 = jis1.getNextJarEntry()) != null) {
                if (je1.isDirectory())
                    continue;

                JarEntry je2 = null;
                while((je2 = jis2.getNextJarEntry()) != null) {
                    if (!je2.isDirectory())
                        break;
                }

                assertEquals(je1.getName(), je2.getName());
                assertEquals(je1.getSize(), je2.getSize());

                try {
                    byte[] buf1 = streamToByteArray(jis1);
                    byte[] buf2 = streamToByteArray(jis2);

                    assertArrayEquals("Contents not equal: " + je1.getName(), buf1, buf2);
                } finally {
                    jis1.closeEntry();
                    jis2.closeEntry();
                }
            }
        } finally {
            closeQuietly(jis1);
            closeQuietly(jis2);
        }
    }

    private static byte [] streamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BundleFileProcessor.pumpStream(is, baos);
        return baos.toByteArray();
    }

    private static File getMavenArtifactFile(File repoRoot, String gid, String aid, String ver) {
        return new File(repoRoot, gid.replace('.', '/') + '/' + aid + '/' + ver + '/' + aid + '-' + ver + ".jar");
    }

    private static File getMavenRepoRoot() throws IOException {
        URL res = BundleFileProcessorTest.class.getClassLoader().getResource(
                Test.class.getName().replace('.', '/') + ".class");

        String u = res.toExternalForm();
        if (u.startsWith("jar:"))
            u = u.substring(4);

        int idx = u.indexOf("junit");
        if (idx < 0)
            throw new IllegalStateException("Cannot infer maven repo root: " + res);

        return new File(new URL(u.substring(0, idx)).getFile());
    }
}
