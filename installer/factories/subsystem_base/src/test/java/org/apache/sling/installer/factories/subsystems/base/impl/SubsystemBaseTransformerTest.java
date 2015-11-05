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
package org.apache.sling.installer.factories.subsystems.base.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.installer.factories.subsystems.base.impl.SubsystemBaseTransformer.DeleteOnCloseFileInputStream;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubsystemBaseTransformerTest {
    @Test
    public void testTransformNoRunMode() throws Exception {
        SlingSettingsService slingSettings = Mockito.mock(SlingSettingsService.class);
        SubsystemBaseTransformer sbt = new SubsystemBaseTransformer(slingSettings);

        URL testArchive = getClass().getResource("/test1.subsystem-base");
        RegisteredResource resource = new TestRegisteredResource(testArchive);
        TransformationResult[] tra = sbt.transform(resource);
        assertEquals(1, tra.length);
        TransformationResult tr = tra[0];
        assertEquals("esa", tr.getResourceType());
        assertEquals("test1", tr.getId());
        DeleteOnCloseFileInputStream dcis = (DeleteOnCloseFileInputStream) tr.getInputStream();
        assertTrue("The file backing the stream should exist", dcis.file.exists());

        Set<String> foundBundles = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(dcis);
             JarFile jf = new JarFile(testArchive.getFile())) {
            ZipEntry ze = null;
            while((ze = zis.getNextEntry()) != null) {
                foundBundles.add(ze.getName());
                switch(ze.getName()) {
                case "OSGI-INF/SUBSYSTEM.MF":
                    Manifest mf = new Manifest(zis);
                    Attributes attrs = mf.getMainAttributes();
                    assertEquals("test1", attrs.getValue("Subsystem-SymbolicName"));
                    assertEquals("osgi.subsystem.composite", attrs.getValue("Subsystem-Type"));
                    assertEquals("(c) 2015 yeah!", attrs.getValue("Subsystem-Copyright"));
                    assertEquals("Extra subsystem headers can go here including very long ones "
                            + "that would span multiple lines in a manifest", attrs.getValue("Subsystem-Description"));
                    assertEquals("org.apache.sling.commons.osgi;version=2.3.0;type=osgi.bundle;start-order:=0,"
                            + "org.apache.sling.commons.json;version=2.0.12;type=osgi.bundle;start-order:=10,"
                            + "org.apache.sling.commons.mime;version=2.1.8;type=osgi.bundle;start-order:=10",
                            attrs.getValue("Subsystem-Content"));
                    break;
                case "org.apache.sling.commons.osgi-2.3.0.jar":
                    ZipEntry oze = jf.getEntry("Potential_Bundles/0/org.apache.sling.commons.osgi-2.3.0.jar");
                    assertArtifactsEqual(oze.getName(), jf.getInputStream(oze), zis);
                    break;
                case "org.apache.sling.commons.json-2.0.12.jar":
                    ZipEntry jze = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.json-2.0.12.jar");
                    assertArtifactsEqual(jze.getName(), jf.getInputStream(jze), zis);
                    break;
                case "org.apache.sling.commons.mime-2.1.8.jar":
                    ZipEntry mze = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.mime-2.1.8.jar");
                    assertArtifactsEqual(mze.getName(), jf.getInputStream(mze), zis);
                    break;
                }
            }
        }
        assertEquals(new HashSet<>(Arrays.asList("OSGI-INF/SUBSYSTEM.MF",
                "org.apache.sling.commons.osgi-2.3.0.jar",
                "org.apache.sling.commons.json-2.0.12.jar",
                "org.apache.sling.commons.mime-2.1.8.jar")), foundBundles);

        assertFalse("After closing the stream the temp file should have been deleted.",
                dcis.file.exists());
    }

    @Test
    public void testOtherRunMode() throws Exception {
        SlingSettingsService slingSettings = Mockito.mock(SlingSettingsService.class);
        Mockito.when(slingSettings.getRunModes()).thenReturn(new HashSet<String>(Arrays.asList("bar", "tar")));
        SubsystemBaseTransformer sbt = new SubsystemBaseTransformer(slingSettings);

        URL testArchive = getClass().getResource("/test1.subsystem-base");
        RegisteredResource resource = new TestRegisteredResource(testArchive);
        TransformationResult[] tra = sbt.transform(resource);
        assertEquals(1, tra.length);
        TransformationResult tr = tra[0];
        assertEquals("esa", tr.getResourceType());
        assertEquals("test1", tr.getId());
        DeleteOnCloseFileInputStream dcis = (DeleteOnCloseFileInputStream) tr.getInputStream();
        assertTrue("The file backing the stream should exist", dcis.file.exists());

        Set<String> foundBundles = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(dcis);
             JarFile jf = new JarFile(testArchive.getFile())) {
            ZipEntry ze = null;
            while((ze = zis.getNextEntry()) != null) {
                foundBundles.add(ze.getName());
                switch(ze.getName()) {
                case "OSGI-INF/SUBSYSTEM.MF":
                    Manifest mf = new Manifest(zis);
                    Attributes attrs = mf.getMainAttributes();
                    assertEquals("test1", attrs.getValue("Subsystem-SymbolicName"));
                    assertEquals("osgi.subsystem.composite", attrs.getValue("Subsystem-Type"));
                    assertEquals("(c) 2015 yeah!", attrs.getValue("Subsystem-Copyright"));
                    assertEquals("Extra subsystem headers can go here including very long ones "
                            + "that would span multiple lines in a manifest", attrs.getValue("Subsystem-Description"));
                    assertEquals("org.apache.sling.commons.osgi;version=2.3.0;type=osgi.bundle;start-order:=0,"
                            + "org.apache.sling.commons.json;version=2.0.12;type=osgi.bundle;start-order:=10,"
                            + "org.apache.sling.commons.mime;version=2.1.8;type=osgi.bundle;start-order:=10,"
                            + "org.apache.sling.commons.threads;version=3.2.0;type=osgi.bundle;start-order:=20,"
                            + "org.apache.sling.commons.contentdetection;version=1.0.2;type=osgi.bundle;start-order:=100",
                            attrs.getValue("Subsystem-Content"));
                    break;
                case "org.apache.sling.commons.osgi-2.3.0.jar":
                    ZipEntry oze = jf.getEntry("Potential_Bundles/0/org.apache.sling.commons.osgi-2.3.0.jar");
                    assertArtifactsEqual(oze.getName(), jf.getInputStream(oze), zis);
                    break;
                case "org.apache.sling.commons.json-2.0.12.jar":
                    ZipEntry jze = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.json-2.0.12.jar");
                    assertArtifactsEqual(jze.getName(), jf.getInputStream(jze), zis);
                    break;
                case "org.apache.sling.commons.mime-2.1.8.jar":
                    ZipEntry mze = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.mime-2.1.8.jar");
                    assertArtifactsEqual(mze.getName(), jf.getInputStream(mze), zis);
                    break;
                case "org.apache.sling.commons.threads-3.2.0.jar":
                    ZipEntry tze = jf.getEntry("Potential_Bundles/20/org.apache.sling.commons.threads-3.2.0.jar");
                    assertArtifactsEqual(tze.getName(), jf.getInputStream(tze), zis);
                    break;
                case "org.apache.sling.commons.contentdetection-1.0.2.jar":
                    ZipEntry cze = jf.getEntry("Potential_Bundles/100/org.apache.sling.commons.contentdetection-1.0.2.jar");
                    assertArtifactsEqual(cze.getName(), jf.getInputStream(cze), zis);
                    break;
                }
            }
        }
        assertEquals(new HashSet<>(Arrays.asList("OSGI-INF/SUBSYSTEM.MF",
                "org.apache.sling.commons.osgi-2.3.0.jar",
                "org.apache.sling.commons.json-2.0.12.jar",
                "org.apache.sling.commons.mime-2.1.8.jar",
                "org.apache.sling.commons.threads-3.2.0.jar",
                "org.apache.sling.commons.contentdetection-1.0.2.jar")), foundBundles);

        assertFalse("After closing the stream the temp file should have been deleted.",
                dcis.file.exists());
    }

    private void assertArtifactsEqual(String name, InputStream is1, InputStream is2) throws IOException {
        byte[] bytes1 = suckStreams(is1);
        byte[] bytes2 = suckStreams(is2);
        assertArrayEquals(name, bytes1, bytes2);
    }

    public static void pumpStreams(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[16384];

        int length = 0;
        int offset = 0;

        while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += length;

            if (offset == bytes.length) {
                os.write(bytes, 0, bytes.length);
                offset = 0;
            }
        }
        if (offset != 0) {
            os.write(bytes, 0, offset);
        }
    }

    public static byte [] suckStreams(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pumpStreams(is, baos);
        return baos.toByteArray();
    }

    private static class TestRegisteredResource implements RegisteredResource {
        private final URL resourceURL;

        TestRegisteredResource(URL url) {
            resourceURL = url;
        }

        @Override
        public String getScheme() {
            return resourceURL.getProtocol();
        }

        @Override
        public String getURL() {
            return resourceURL.toExternalForm();
        }

        @Override
        public String getType() {
            return "file";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resourceURL.openStream();
        }

        @Override
        public Dictionary<String, Object> getDictionary() {
            return null;
        }

        @Override
        public String getDigest() {
            return null;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public String getEntityId() {
            return null;
        }
    }
}
