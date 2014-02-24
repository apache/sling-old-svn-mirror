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
package org.apache.sling.maven.projectsupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Test the BundleListContentProvider */
public class BundleListContentProviderTest {
    private static BundleList bundleList;
    
    public static final String TEST_BUNDLE_LIST = "test-bundle-list.xml";
    public static final int BUNDLES_IN_TEST_BUNDLE_LIST = 13;
    
    private LaunchpadContentProvider provider;
    private File resourceProviderRoot;
    private File resourceProviderFile;
    private File configDirectory;
    private int logWarningsCount;
    private String fakeBundlePath;
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private static final String [] CONFIG_FILES = {
        "file1.txt",
        "file2.cfg",
        "someFile.properties"
    };
    
    @BeforeClass
    public static void parseBundleList() throws Exception {
        final BundleListXpp3Reader reader = new BundleListXpp3Reader();
        final InputStream is = BundleListContentProviderTest.class.getClassLoader().getResourceAsStream(TEST_BUNDLE_LIST);
        assertNotNull("Expecting " + TEST_BUNDLE_LIST + " to be found", is);
        try {
            bundleList = reader.read(is);
        } finally {
            is.close();
        }
    }
    
    @Before
    public void setupTemporaryFiles() throws IOException {
        for(String filename: CONFIG_FILES) {
            final File f = getConfigFile(filename);
            f.createNewFile();
            assertTrue("Expecting temporary config file to have been created: " + f.getAbsolutePath(), f.exists());
        }
        configDirectory = tempFolder.getRoot();

        resourceProviderRoot = new File(tempFolder.getRoot(), "RESOURCE_PROVIDER_ROOT");
        resourceProviderRoot.mkdirs();
        resourceProviderFile = new File(resourceProviderRoot, "RP_FILE_" + System.currentTimeMillis());
        resourceProviderFile.createNewFile();
        fakeBundlePath = getFakeBundlePath();
    }
    
    private File getConfigFile(String name) {
        return new File(tempFolder.getRoot(), name);
    }

    private String getFakeBundlePath() {
        try {
            return new File("/FAKE_BUNDLE").toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setupProvider() {
        final Log log = Mockito.mock(Log.class);
        final Answer<Void> countWarningAnswers = new Answer<Void>() {

            public Void answer(InvocationOnMock invocation) throws Throwable {
                logWarningsCount++;
                return null;
            }
            
        };
        Mockito.doAnswer(countWarningAnswers).when(log).warn(Matchers.any(String.class));
        Mockito.doAnswer(countWarningAnswers).when(log).warn(Matchers.any(Throwable.class));
        Mockito.doAnswer(countWarningAnswers).when(log).warn(Matchers.any(String.class), Matchers.any(Throwable.class));
        
        provider = new BundleListContentProvider(resourceProviderRoot) {

            @Override
            BundleList getInitializedBundleList() {
                return bundleList;
            }

            @Override
            File getConfigDirectory() {
                return configDirectory;
            }

            @Override
            Artifact getArtifact(ArtifactDefinition def) throws MojoExecutionException {
                final Artifact a = Mockito.mock(Artifact.class);
                final String fakeName = new StringBuilder()
                .append("/FAKE_BUNDLE/")
                .append(def.getArtifactId())
                .append("/")
                .append(def.getStartLevel())
                .append("/")
                .append(def.getRunModes())
                .toString();
                Mockito.when(a.getFile()).thenReturn(new File(fakeName));
                return a;
            }

            @Override
            Log getLog() {
                return log;
            }
        };
    }
    
    private void assertChildren(String path, String ...expected) {
        final List<String> kids = new ArrayList<String>();
        final Iterator<String> it = provider.getChildren(path);
        if(expected.length == 0) {
           assertTrue("Expecting no children for " + path, it == null || !it.hasNext()); 
        } else {
            assertNotNull("Expecting non-null iterator for " + path, it);
            while(it.hasNext()) {
                kids.add(it.next());
            }
            for(String exp : expected) {
                assertTrue("Expecting " + exp + " in children of " + path + " (result=" + kids + ")", 
                        kids.contains(exp));
            }
        }
        assertEquals("Expecting the correct number of children for " + path + " (result=" + kids + ")", 
                expected.length, kids.size());
    }
    
    @Test
    public void testParsedBundlesCount() {
        int counter = 0;
        for(StartLevel level : bundleList.getStartLevels()) {
            counter += level.getBundles().size();
        }
        assertEquals(BUNDLES_IN_TEST_BUNDLE_LIST, counter);
    }
    
    @Test
    public void testRoot() {
        assertChildren("resources", 
                "resources/bundles", 
                "resources/corebundles", 
                "resources/config",
                "resources/install",
                "resources/install.dev", 
                "resources/install.test", 
                "resources/install.oak", 
                "resources/install.jackrabbit");
    }
    
    @Test
    public void testBundles() {
        assertChildren("resources/bundles", 
                "resources/bundles/1/"); 
    }
    
    @Test
    public void testInstall() {
        assertChildren("resources/install",
                "resources/install/0", 
                "resources/install/1", 
                "resources/install/5", 
                "resources/install/15"); 
    }
    
    @Test
    public void testInstallDev() {
        assertChildren("resources/install.dev",
                "resources/install.dev/0", 
                "resources/install.dev/5");
    }
    
    @Test
    public void testInstallTest() {
        assertChildren("resources/install.test",
                "resources/install.test/0", 
                "resources/install.test/5");
    }
    
    @Test
    public void testInstallOak() {
        assertChildren("resources/install.oak",
                "resources/install.oak/15");
    }
    
    @Test
    public void testInstallJackrabbit() {
        assertChildren("resources/install.jackrabbit",
                "resources/install.jackrabbit/15");
    }
    
    @Test
    public void testCoreBundles() {
        assertChildren("resources/corebundles"); 
    }
    
    @Test
    public void testConfig() {
        assertChildren("resources/config", 
                "resources/config/file1.txt", 
                "resources/config/file2.cfg", 
                "resources/config/someFile.properties"); 
    }
    
    @Test
    public void testConfigFile() {
        assertChildren("resources/config/file1.txt");
        assertEquals("Expecting no warnings", 0, logWarningsCount);
    }
    
    @Test
    public void testConfigSubpath() {
        assertChildren("resources/config/someFolder/someSubFolder");
        assertEquals("Expecting a warning", 1, logWarningsCount);
    }
    
    @Test
    public void testNonExistentConfigDirectory() {
        configDirectory = new File("/NON_EXISTENT_" + System.currentTimeMillis());
        assertChildren("resources/config");
    }

    @Test
    public void testIgnoredNonBootstrapBundles() {
        // All these start levels do not provide resources/bundles anymore - moved to resources/install
        for(int i=0; i <= 30; i++) {
            if(i == 1) {
                continue;
            }
            final String path ="resources/bundles/" + i;
            assertNull("Expecting no resources under " + path, provider.getChildren(path));
        }
    }
    
    @Test
    public void testInstall0() {
        assertChildren("resources/install/0",
                fakeBundlePath + "/commons-io/0/null",
                fakeBundlePath + "/commons-fileupload/0/null",
                fakeBundlePath + "/commons-collections/0/null");
    }
    
    @Test
    public void testBootstrapBundles() {
        assertChildren("resources/bundles/1",
                fakeBundlePath + "/slf4j-api/-1/null",
                fakeBundlePath + "/org.apache.sling.commons.log/-1/null");
    }
    
    @Test
    public void testInstall5() {
        assertChildren("resources/install/5",
                fakeBundlePath + "/five.norunmode/5/null");
    }
    
    @Test
    public void testInstall5Dev() {
        assertChildren("resources/install.dev/5",
                fakeBundlePath + "/org.apache.sling.extensions.webconsolebranding/5/dev");
    }
    
    @Test
    public void testInstall5Test() {
        assertChildren("resources/install.test/5",
                fakeBundlePath + "/org.apache.sling.extensions.webconsolesecurityprovider/5/test");
    }
    
    @Test
    public void testInstall15() {
        assertChildren("resources/install/15",
                fakeBundlePath + "/fifteen.norunmode/15/null");
    }
    
    @Test
    public void testInstall15Oak() {
        assertChildren("resources/install.oak/15",
                fakeBundlePath + "/org.apache.sling.jcr.oak.server/15/oak",
                fakeBundlePath + "/jsr305/15/oak,jackrabbit");
    }
    
    @Test
    public void testInstall15Jackrabbit() {
        assertChildren("resources/install.jackrabbit/15",
                fakeBundlePath + "/guava/15/jackrabbit",
                fakeBundlePath + "/jsr305/15/oak,jackrabbit");
    }
    
    @Test
    public void testConfigResource() throws Exception {
        final URL url = provider.getResource("resources/config/file1.txt");
        assertNotNull("Expecting config resource to be found", url);
        assertEquals(getConfigFile("file1.txt").toURI().toURL().toExternalForm(), url.toExternalForm());
    }
    
    @Test
    public void testResourceProviderResource() throws Exception {
        final URL url = provider.getResource(resourceProviderFile.getName());
        assertNotNull("Expecting resource provider file to be found", url);
        assertEquals(resourceProviderFile.toURI().toURL().toExternalForm(), url.toExternalForm());
    }
    
    @Test
    public void testClasspathResource() throws Exception {
        final URL url = provider.getResource(TEST_BUNDLE_LIST);
        assertNotNull("Expecting classpath resource to be found", url);
        assertTrue(url.toExternalForm().endsWith(TEST_BUNDLE_LIST));
    }
    
    @Test
    public void testClasspathResourceAsStream() throws Exception {
        final InputStream is = provider.getResourceAsStream(TEST_BUNDLE_LIST);
        assertNotNull("Expecting classpath resource stream to be found", is);
        is.close();
    }
    
    @Test
    public void testNotFoundStream() throws Exception {
        final InputStream is = provider.getResourceAsStream("resources/config/NOT_HERE.txt");
        assertNull("Expecting null stream for non-existent resource", is);
    }
    
    @Test
    public void testURLResource() throws Exception {
        final String urlStr = "http://www.perdu.com";
        final URL url = provider.getResource(urlStr);
        assertNotNull("Expecting URL resource to be found", url);
        assertEquals(new URL(urlStr), url);
    }
    
    @Test
    public void testNullResult() {
        assertNull(provider.getChildren("/FOO/bar"));
    }
    
    @Test
    public void testAllBundlesFound() {
        final List<String> allResources = new LinkedList<String>();
        addRecursively(allResources, "resources");
        final List<String> bundles = new LinkedList<String>();
        for(String r : allResources) {
            if(r.contains("FAKE_BUNDLE")) {
                bundles.add(r);
            }
        }
        
        // Bundles that have two run modes appear in two folders, we have two of those
        // with two run modes each
        final int expected = BUNDLES_IN_TEST_BUNDLE_LIST + 2;
        assertEquals("Expecting the exact number of test bundles to be found", expected, bundles.size());
    }
    
    @Test
    public void testFile() {
        assertChildren("file:/something");
    }
                
    private void addRecursively(List<String> resources, String path) {
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        resources.add(path);
        final Iterator<String> it = provider.getChildren(path);
        if(it != null) {
            while(it.hasNext()) {
                addRecursively(resources, it.next());
            }
        }
    }
    
}
