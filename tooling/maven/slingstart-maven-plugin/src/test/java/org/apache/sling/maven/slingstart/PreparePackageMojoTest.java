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
package org.apache.sling.maven.slingstart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PreparePackageMojoTest {
    @Test
    public void testBSNRenaming() throws Exception {
        // Provide the system with some artifacts that are known to be in the local .m2 repo
        // These are explicitly included in the test section of the pom.xml
        PreparePackageMojo ppm = getMojoUnderTest(
                "org.apache.sling/org.apache.sling.commons.classloader/1.3.2",
                "org.apache.sling/org.apache.sling.commons.classloader/1.3.2/app",
                "org.apache.sling/org.apache.sling.commons.json/2.0.12");
        try {
            String modelTxt = "[feature name=:launchpad]\n" +
                    "[artifacts]\n" +
                    "  org.apache.sling/org.apache.sling.commons.classloader/1.3.2\n" +
                    "" +
                    "[feature name=rename_test]\n" +
                    "  org.apache.sling/org.apache.sling.commons.json/2.0.12 [bundle:rename-bsn=r-foo.bar.renamed.sling.commons.json]\n";

            Model model = ModelReader.read(new StringReader(modelTxt), null);
            ppm.execute(model);

            File orgJar = getMavenArtifactFile(getMavenRepoRoot(), "org.apache.sling", "org.apache.sling.commons.json", "2.0.12");
            File generatedJar = new File(ppm.getTmpDir() + "/r-foo.bar.renamed.sling.commons.json-2.0.12.jar");

            compareJarContents(orgJar, generatedJar);

            try (JarFile jfOrg = new JarFile(orgJar);
                JarFile jfNew = new JarFile(generatedJar)) {
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

                        assertEquals("r-foo.bar.renamed.sling.commons.json", newVal);
                    } else {
                        assertEquals("Different keys: " + key, orgVal, newVal);
                    }
                }
            }
        } finally {
            FileUtils.deleteDirectory(new File(ppm.project.getBuild().getDirectory()));
        }
    }

    private static void compareJarContents(File orgJar, File actualJar) throws IOException {
        try (JarInputStream jis1 = new JarInputStream(new FileInputStream(orgJar));
            JarInputStream jis2 = new JarInputStream(new FileInputStream(actualJar))) {
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
                    byte[] buf1 = IOUtils.toByteArray(jis1);
                    byte[] buf2 = IOUtils.toByteArray(jis2);

                    assertArrayEquals("Contents not equal: " + je1.getName(), buf1, buf2);
                } finally {
                    jis1.closeEntry();
                    jis2.closeEntry();
                }
            }
        }
    }

    @Test
    public void testSubsystemBaseGeneration() throws Exception {
        // Provide the system with some artifacts that are known to be in the local .m2 repo
        // These are explicitly included in the test section of the pom.xml
        PreparePackageMojo ppm = getMojoUnderTest(
                "org.apache.sling/org.apache.sling.commons.classloader/1.3.2",
                "org.apache.sling/org.apache.sling.commons.classloader/1.3.2/app",
                "org.apache.sling/org.apache.sling.commons.contentdetection/1.0.2",
                "org.apache.sling/org.apache.sling.commons.json/2.0.12",
                "org.apache.sling/org.apache.sling.commons.mime/2.1.8",
                "org.apache.sling/org.apache.sling.commons.osgi/2.3.0",
                "org.apache.sling/org.apache.sling.commons.threads/3.2.0");

        try {
            // The launchpad feature is a prerequisite for the model
            String modelTxt =
                    "[feature name=:launchpad]\n" +
                    "[artifacts]\n" +
                    "  org.apache.sling/org.apache.sling.commons.classloader/1.3.2\n" +
                    "" +
                    "[feature name=test1 type=osgi.subsystem.composite]\n" +
                    "" +
                    "[:subsystem-manifest startLevel=123]\n" +
                    "  Subsystem-Description: Extra subsystem headers can go here including very long ones that would span multiple lines in a manifest\n" +
                    "  Subsystem-Copyright: (c) 2015 yeah!\n" +
                    "" +
                    "[artifacts]\n" +
                    "  org.apache.sling/org.apache.sling.commons.osgi/2.3.0\n" +
                    "" +
                    "[artifacts startLevel=10]\n" +
                    "  org.apache.sling/org.apache.sling.commons.json/2.0.12\n" +
                    "  org.apache.sling/org.apache.sling.commons.mime/2.1.8\n" +
                    "" +
                    "[artifacts startLevel=20 runModes=foo,bar,:blah]\n" +
                    "  org.apache.sling/org.apache.sling.commons.threads/3.2.0\n" +
                    "" +
                    "[artifacts startLevel=100 runModes=bar]\n" +
                    "  org.apache.sling/org.apache.sling.commons.contentdetection/1.0.2\n";
            Model model = ModelReader.read(new StringReader(modelTxt), null);
            ppm.execute(model);

            File generatedFile = new File(ppm.getTmpDir() + "/test1.subsystem-base");
            try (JarFile jf = new JarFile(generatedFile)) {
                // Test META-INF/MANIFEST.MF
                Manifest mf = jf.getManifest();
                Attributes attrs = mf.getMainAttributes();
                String expected = "Potential_Bundles/0/org.apache.sling.commons.osgi-2.3.0.jar|"
                        + "Potential_Bundles/10/org.apache.sling.commons.json-2.0.12.jar|"
                        + "Potential_Bundles/10/org.apache.sling.commons.mime-2.1.8.jar";
                assertEquals(expected, attrs.getValue("_all_"));
                assertEquals("Potential_Bundles/20/org.apache.sling.commons.threads-3.2.0.jar", attrs.getValue("foo"));
                assertEquals("Potential_Bundles/20/org.apache.sling.commons.threads-3.2.0.jar|"
                        + "Potential_Bundles/100/org.apache.sling.commons.contentdetection-1.0.2.jar", attrs.getValue("bar"));

                // Test SUBSYSTEM-MANIFEST-BASE.MF
                ZipEntry smbZE = jf.getEntry("SUBSYSTEM-MANIFEST-BASE.MF");
                try (InputStream smbIS = jf.getInputStream(smbZE)) {
                    Manifest smbMF = new Manifest(smbIS);
                    Attributes smbAttrs = smbMF.getMainAttributes();
                    assertEquals("test1", smbAttrs.getValue("Subsystem-SymbolicName"));
                    assertEquals("osgi.subsystem.composite", smbAttrs.getValue("Subsystem-Type"));
                    assertEquals("(c) 2015 yeah!", smbAttrs.getValue("Subsystem-Copyright"));
                    assertEquals("Extra subsystem headers can go here including very long ones "
                            + "that would span multiple lines in a manifest",
                            smbAttrs.getValue("Subsystem-Description"));
                }

                // Test embedded bundles
                File mrr = getMavenRepoRoot();
                File soj = getMavenArtifactFile(mrr, "org.apache.sling", "org.apache.sling.commons.osgi", "2.3.0");
                ZipEntry sojZE = jf.getEntry("Potential_Bundles/0/org.apache.sling.commons.osgi-2.3.0.jar");
                try (InputStream is = jf.getInputStream(sojZE)) {
                    assertArtifactsEqual(soj, is);
                }

                File sjj = getMavenArtifactFile(mrr, "org.apache.sling", "org.apache.sling.commons.json", "2.0.12");
                ZipEntry sjZE = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.json-2.0.12.jar");
                try (InputStream is = jf.getInputStream(sjZE)) {
                    assertArtifactsEqual(sjj, is);
                }

                File smj = getMavenArtifactFile(mrr, "org.apache.sling", "org.apache.sling.commons.mime", "2.1.8");
                ZipEntry smjZE = jf.getEntry("Potential_Bundles/10/org.apache.sling.commons.mime-2.1.8.jar");
                try (InputStream is = jf.getInputStream(smjZE)) {
                    assertArtifactsEqual(smj, is);
                }

                File stj = getMavenArtifactFile(mrr, "org.apache.sling", "org.apache.sling.commons.threads", "3.2.0");
                ZipEntry stjZE = jf.getEntry("Potential_Bundles/20/org.apache.sling.commons.threads-3.2.0.jar");
                try (InputStream is = jf.getInputStream(stjZE)) {
                    assertArtifactsEqual(stj, is);
                }

                File ctj = getMavenArtifactFile(mrr, "org.apache.sling", "org.apache.sling.commons.contentdetection", "1.0.2");
                ZipEntry ctjZE = jf.getEntry("Potential_Bundles/100/org.apache.sling.commons.contentdetection-1.0.2.jar");
                try (InputStream is = jf.getInputStream(ctjZE)) {
                    assertArtifactsEqual(ctj, is);
                }
            }
        } finally {
            FileUtils.deleteDirectory(new File(ppm.project.getBuild().getDirectory()));
        }
    }

    private void assertArtifactsEqual(File f, InputStream is) throws IOException {
        byte[] bytes1 = Files.readAllBytes(f.toPath());
        byte[] bytes2 = IOUtils.toByteArray(is);
        assertArrayEquals("Bytes not equal on file " + f.getName(), bytes1, bytes2);
    }

    private PreparePackageMojo getMojoUnderTest(String ... knownArtifacts) throws Exception {
        File mrr = getMavenRepoRoot();

        ArtifactHandler ah = Mockito.mock(ArtifactHandler.class);
        ArtifactHandlerManager ahm = Mockito.mock(ArtifactHandlerManager.class);
        Mockito.when(ahm.getArtifactHandler(Mockito.anyString())).thenReturn(ah);

        Set<org.apache.maven.artifact.Artifact> artifacts = new HashSet<>();
        for (String s : knownArtifacts) {
            String[] parts = s.split("[/]");
            switch (parts.length) {
            case 3:
                artifacts.add(getMavenArtifact(mrr, ah, parts[0], parts[1], parts[2]));
                break;
            case 4:
                artifacts.add(getMavenArtifact(mrr, ah, parts[0], parts[1], parts[2], parts[3]));
                break;
            default: throw new IllegalStateException(s);
            }
        }

        MavenProject mavenPrj = new MavenProject();
        Build build = new Build();
        Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
        build.setOutputDirectory(tempDir.toString());
        build.setDirectory(tempDir.toString());
        mavenPrj.setBuild(build);
        mavenPrj.setDependencyArtifacts(artifacts);

        PreparePackageMojo ppm = new PreparePackageMojo();
        ppm.mavenSession = Mockito.mock(MavenSession.class);
        ppm.project = mavenPrj;
        ArchiverManager am = Mockito.mock(ArchiverManager.class);
        UnArchiver ua = Mockito.mock(UnArchiver.class);
        Mockito.when(am.getUnArchiver(Mockito.isA(File.class))).thenReturn(ua);
        setPrivateField(ppm, "archiverManager", am);
        setPrivateField(ppm, "artifactHandlerManager", ahm);
        setPrivateField(ppm, "resolver", Mockito.mock(ArtifactResolver.class));
        return ppm;
    }

    private org.apache.maven.artifact.Artifact getMavenArtifact(File repoRoot, ArtifactHandler ah, String gid, String aid, String ver) {
        return getMavenArtifact(repoRoot, ah, gid, aid, ver, null);
    }

    private org.apache.maven.artifact.Artifact getMavenArtifact(File repoRoot, ArtifactHandler ah, String gid, String aid, String ver, String classifier) {
        DefaultArtifact art = new DefaultArtifact(gid, aid, ver, "compile", "jar", classifier, ah);
        art.setFile(getMavenArtifactFile(repoRoot, gid, aid, ver));
        return art;
    }

    private File getMavenArtifactFile(File repoRoot, String gid, String aid, String ver) {
        return new File(repoRoot, gid.replace('.', '/') + '/' + aid + '/' + ver + '/' + aid + '-' + ver + ".jar");
    }

    private File getMavenRepoRoot() throws IOException {
        URL res = getClass().getClassLoader().getResource(
                Test.class.getName().replace('.', '/') + ".class");

        String u = res.toExternalForm();
        if (u.startsWith("jar:"))
            u = u.substring(4);

        int idx = u.indexOf("junit");
        if (idx < 0)
            throw new IllegalStateException("Cannot infer maven repo root: " + res);

        return new File(new URL(u.substring(0, idx)).getFile());
    }

    private void setPrivateField(Object obj, String name, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
