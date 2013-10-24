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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.mockito.Mockito;

/** Test the BundleListContentProvider */
public class BundleListContentProviderTest {
    private static BundleList bundleList;
    
    public static final String TEST_BUNDLE_LIST = "test-bundle-list.xml";
    public static final int BUNDLES_IN_TEST_BUNDLE_LIST = 11;
    
    private LaunchpadContentProvider provider;
    private File resourceProviderRoot;
    private File resourceProviderFile;
    private File configDirectory;
    
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
    }
    
    private File getConfigFile(String name) {
        return new File(tempFolder.getRoot(), name);
    }
    
    @Before
    public void setupProvider() {
        final Log log = Mockito.mock(Log.class);
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
                .append("/")
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
            while(it.hasNext()) {
                kids.add(it.next());
            }
            for(String exp : expected) {
                assertTrue("Expecting " + exp + " in children of " + path + " (result=" + kids + ")", kids.contains(exp));
            }
        }
        assertEquals("Expecting the correct number of children for " + path, expected.length, kids.size());
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
                "resources/config");
    }
    
    @Test
    public void testBundles() {
        assertChildren("resources/bundles", 
                "resources/bundles/0/", 
                "resources/bundles/1/", 
                "resources/bundles/5/", 
                "resources/bundles/15/"); 
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
    public void testNonExistentConfigDirectory() {
        configDirectory = new File("/NON_EXISTENT_" + System.currentTimeMillis());
        assertChildren("resources/config");
    }

    @Test
    public void testBundles0() {
        assertChildren("resources/bundles/0", 
                "file:/commons-io/0/null", 
                "file:/commons-fileupload/0/null", 
                "file:/commons-collections/0/null", 
                "file:/org.apache.sling.installer.provider.jcr/0/test,dev"); 
    }
    
    @Test
    public void testBundles1() {
        assertChildren("resources/bundles/1", 
                "file:/slf4j-api/1/null", 
                "file:/org.apache.sling.commons.log/1/null"); 
    }
    
    @Test
    public void testBundles5() {
        assertChildren("resources/bundles/5", 
                "file:/org.apache.sling.extensions.webconsolebranding/5/dev", 
                "file:/org.apache.sling.extensions.webconsolesecurityprovider/5/test"); 
    }
    
    @Test
    public void testBundles15() {
        assertChildren("resources/bundles/15", 
                "file:/org.apache.sling.jcr.oak.server/15/oak", 
                "file:/guava/15/jackrabbit", 
                "file:/jsr305/15/oak,jackrabbit"); 
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
    
}
