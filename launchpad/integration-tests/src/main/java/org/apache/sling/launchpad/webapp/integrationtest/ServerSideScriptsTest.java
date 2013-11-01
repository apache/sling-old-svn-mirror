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
package org.apache.sling.launchpad.webapp.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.launchpad.webapp.integrationtest.util.ServerSideTestClient;
import org.codehaus.plexus.util.Expand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute all server-side test scripts found in a specified
 * (class) resource folder.
 */
@RunWith(Parameterized.class)
public class ServerSideScriptsTest {

    /** Script directory default value */
    private static String TEST_SCRIPT_DIR_DEFAULT = "scripts/sling-it";

    /** Script directory for assumed to fail scripting tests default value */
    private static String TEST_SCRIPT_DIR_FAIL_DEFAULT = "scripts/sling-it/expected-to-fail";

    /** The resource type prefix for the uploaded test script folder */
    private static final String RESOURCE_TYPE_PREFIX = "testing/sling/scripted-tests";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final class Definition {
        public final File testScriptFile;
        public final String testName;
        public final String scriptExtension;
        public final boolean willFail;

        public Definition(final File file, final boolean willFail) {
            final String name = file.getName();
            final int pos = name.lastIndexOf('.');

            this.scriptExtension = name.substring(pos);
            final String prefix = (willFail ? "fail-" : "");
            this.testName = prefix + name.substring(0, pos);
            this.testScriptFile = file;
            this.willFail = willFail;
        }
        
        @Override
        public String toString() {
            return testScriptFile.getName();
        }
    }
    
    private static class Collector {
        private final List<Definition> tests = new ArrayList<Definition>();
        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        
        Collector() {
            addScripts(TEST_SCRIPT_DIR_DEFAULT, false);
            addScripts(TEST_SCRIPT_DIR_FAIL_DEFAULT, true);
        }
        
        /**
         * Get the directory for a resource path
         * @param resourcePath The resource path pointing to the script directory
         * @return A file object if the path points to a directory
         */
        private File getScriptDirectory(final String resourcePath) {
            final URL url = ServerSideScriptsTest.class.getClassLoader().getResource(resourcePath);
            if (url != null) {
                if ( url.getProtocol().equals("file") ) {
                    URI uri = null;
                    try {
                        uri = url.toURI();
                        final File dir = new File(uri);
                        if ( dir.exists() && dir.isDirectory() ) {
                            return dir;
                        }
                    } catch (final URISyntaxException e) {
                        logger.info("Failed to get scripts from " + url , e);
                        // ignore
                    }
                } else if ( url.getProtocol().equals("jar") ) {
                    final String urlString = url.toString();
                    final int pos = urlString.indexOf('!');
                    try {
                        final String jarFilePath = urlString.substring(4, pos);
                        final URL jarURL = new URL(jarFilePath);
                        final URI uri = jarURL.toURI();

                        // create a temp dir
                        final File baseDir = new File(System.getProperty("java.io.tmpdir"));

                        final File tempDir = new File(baseDir, System.currentTimeMillis() + ".dir");
                        if (!tempDir.mkdir()) {
                            throw new IllegalStateException("Failed to create temporary directory");
                        }
                        tempDir.deleteOnExit();
                        final Expand expander = new Expand();
                        expander.setDest(tempDir);
                        expander.setSrc(new File(uri));
                        expander.execute();

                        final File dir = new File(tempDir, resourcePath);
                        if ( dir.exists() && dir.isDirectory() ) {
                            return dir;
                        }
                    } catch (final Exception e) {
                        logger.info("Script path is not readable: " + urlString, e);
                    }
                } else {
                    logger.info("Script path is in unknown url protocol: " + resourcePath + " - " + url);
                }
            } else {
                logger.info("Script path not found " + resourcePath);
            }
            return null;
        }
        
        /**
         * Collect all scripts of a directory specified by the resource path.
         *
         * @param resourcePath The resource path pointing to the script directory
         * @param willFail <code>false</code> if this test is expected to succeed, <code>true</code> otherwise
         */
        private void addScripts(final String resourcePath,
                                final boolean willFail) {
            final File scriptDir = getScriptDirectory(resourcePath);

            if ( scriptDir != null && scriptDir.list() != null && scriptDir.list().length > 0) {
                for(final File f : scriptDir.listFiles()) {
                    if ( !f.isHidden() ) {
                        if ( f.isFile() ) {
                            logger.info("Found test script {}", f.getAbsolutePath());

                            final Definition test = new Definition(f, willFail);

                            this.tests.add(test);
                        }
                    }
                }
            } else {
                logger.info("No test scripts found with resource path {}", resourcePath);
            }
        }
        
        List<Definition> getTests() {
            return tests;
        }
    };

    private final ServerSideTestClient slingClient;
    private final Definition test;
    
    @Parameters(name="{index} - {0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<Object []>();
        for(Definition t : new Collector().getTests()) {
            result.add(new Object[] { t });
        }
        return result;
    }

    public ServerSideScriptsTest(Definition scriptableTest) {
        this.test = scriptableTest;
        this.slingClient = new ServerSideTestClient();
    }

    @Before
    public void setup() throws Exception {
        slingClient.mkdirs("/apps/" + RESOURCE_TYPE_PREFIX);
    }

    @After
    public void cleanup() throws Exception {
        slingClient.delete("/apps/" + RESOURCE_TYPE_PREFIX);
    }

    @Test
    public void runScripts() throws Exception {
        final String resourceType = RESOURCE_TYPE_PREFIX + '/' + test.testName;
        final String scriptPath = "/apps/" + resourceType;
        String toDelete = null;

        try {
            // create test node
            this.slingClient.createNode(scriptPath,
                    "jcr:primaryType", "sling:Folder",
                    "jcr:mixinTypes", "sling:Test",
                    "sling:resourceType", RESOURCE_TYPE_PREFIX + '/' + test.testName);
            toDelete = scriptPath;

            final String destPath = scriptPath + "/test.txt" + test.scriptExtension;
            logger.info("Setting up node {} for {}", destPath, test.testScriptFile.getAbsoluteFile());
            this.slingClient.upload(destPath, new FileInputStream(test.testScriptFile), -1, false);
            
            // SLING-3087 with Oak, the Sling JUnit's scriptable module TestAllPaths class
            // might still see the old script, and not yet the new one, for some time.
            // There's probably a better way to avoid this...for now just wait a bit
            Thread.sleep(2000L);

            final long startTime = System.currentTimeMillis();
            final ServerSideTestClient.TestResults results = slingClient.runTests("org.apache.sling.junit.scriptable.ScriptableTestsProvider");
            assertEquals("Expecting 1 scriptable test", 1, results.getTestCount());

            final int failureCount = test.willFail ? 1 : 0;
            if( results.getFailures().size() != failureCount) {
                fail("Expected "
                        + failureCount + " failing tests but got " + results.getFailures().size()
                        + " for " + test.testScriptFile.getAbsolutePath()
                        + ": " + results.getFailures());
            }
            
            logger.info("Execution of {} took {} msec", test, System.currentTimeMillis() - startTime);

        } finally {
            if(toDelete != null) {
                this.slingClient.delete(toDelete);
            }
        }
    }
}