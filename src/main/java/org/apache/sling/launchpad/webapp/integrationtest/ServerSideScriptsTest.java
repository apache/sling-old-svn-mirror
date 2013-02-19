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

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.sling.junit.remote.httpclient.RemoteTestHttpClient;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.sling.SlingClient;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.codehaus.plexus.util.Expand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Execute all server-side test scripts found in a specified
 * (class) resource folder.
 */
public class ServerSideScriptsTest extends TestCase {

    /** Script directory default value */
    private static String TEST_SCRIPT_DIR_DEFAULT = "scripts/sling-it";

    /** Script directory for assumed to fail scripting tests default value */
    private static String TEST_SCRIPT_DIR_FAIL_DEFAULT = "scripts/sling-it/expected-to-fail";

    /** The resource type prefix for the uploaded test script folder */
    private static final String RESOURCE_TYPE_PREFIX = "testing/sling/scripted-tests";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final class Description {
        public final File testScriptFile;
        public final String testName;
        public final String scriptExtension;
        public final boolean willFail;

        public Description(final File file, final boolean willFail) {
            final String name = file.getName();
            final int pos = name.lastIndexOf('.');

            this.scriptExtension = name.substring(pos);
            final String prefix = (willFail ? "fail-" : "");
            this.testName = prefix + name.substring(0, pos);
            this.testScriptFile = file;
            this.willFail = willFail;
        }
    }

    private final List<Description> tests = new ArrayList<Description>();

    private final SlingClient slingClient;

    private final String serverBaseUrl;
    private final String serverUsername;
    private final String serverPassword;

    public ServerSideScriptsTest() {
        // collect test scripts
        this.addScripts(TEST_SCRIPT_DIR_DEFAULT, false);
        this.addScripts(TEST_SCRIPT_DIR_FAIL_DEFAULT, true);

        // get configuration - we can't inherit from SlingTestBase as
        // this tries to start the JarExecutor which will fail in some
        // situations
        final String configuredUrl = System.getProperty(SlingTestBase.TEST_SERVER_URL_PROP,
                                                        System.getProperty("launchpad.http.server.url"));
        if (configuredUrl != null) {
            if ( configuredUrl.endsWith("/") ) {
                serverBaseUrl = configuredUrl.substring(0, configuredUrl.length() - 1);
            } else {
                serverBaseUrl = configuredUrl;
            }
        } else {
            String serverHost = System.getProperty(SlingTestBase.SERVER_HOSTNAME_PROP);
            if (serverHost == null || serverHost.trim().length() == 0) {
                serverHost = "localhost";
            }
            final String portStr = System.getProperty(JarExecutor.PROP_SERVER_PORT);
            final int serverPort = portStr == null ? JarExecutor.DEFAULT_PORT : Integer.valueOf(portStr);
            serverBaseUrl = "http://" + serverHost + ":" + String.valueOf(serverPort);
        }

        // Set configured username using "admin" as default credential
        final String configuredUsername = System.getProperty(SlingTestBase.TEST_SERVER_USERNAME);
        if (configuredUsername != null && configuredUsername.trim().length() > 0) {
            serverUsername = configuredUsername;
        } else {
            serverUsername = SlingTestBase.ADMIN;
        }

        // Set configured password using "admin" as default credential
        final String configuredPassword = System.getProperty(SlingTestBase.TEST_SERVER_PASSWORD);
        if (configuredPassword != null && configuredPassword.trim().length() > 0) {
            serverPassword = configuredPassword;
        } else {
            serverPassword = SlingTestBase.ADMIN;
        }

        this.slingClient = new SlingClient(this.serverBaseUrl, this.serverUsername, this.serverPassword);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create base path (if not existing)
        final String[] pathSegments = ("apps/" + RESOURCE_TYPE_PREFIX).split("/");
        String path = "";
        for(final String segment : pathSegments) {
            path = path + '/' + segment;
            if ( !this.slingClient.exists(path) ) {
                this.slingClient.createNode(path,
                        "jcr:primaryType", "sling:Folder");
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        this.slingClient.delete("/apps/" + RESOURCE_TYPE_PREFIX);
        super.tearDown();
    }

    public void testRunScripts() throws Exception {
        // upload test scripts
        for(final Description test : this.tests) {
            final String resourceType = RESOURCE_TYPE_PREFIX + '/' + test.testName;
            final String scriptPath = "/apps/" + resourceType;

            try {
                // create test node
                this.slingClient.createNode(scriptPath,
                        "jcr:primaryType", "sling:Folder",
                        "jcr:mixinTypes", "sling:Test",
                        "sling:resourceType", RESOURCE_TYPE_PREFIX + '/' + test.testName);

                final String destPath = scriptPath + "/test.txt" + test.scriptExtension;
                logger.info("Setting up node {} for {}", destPath, test.testScriptFile.getAbsoluteFile());
                this.slingClient.upload(destPath, new FileInputStream(test.testScriptFile), -1, false);

                final RemoteTestHttpClient testClient = new RemoteTestHttpClient(
                    this.serverBaseUrl + "/system/sling/junit",
                    this.serverUsername,
                    this.serverPassword,
                    true);

                final RequestExecutor executor = testClient.runTests(
                        "org.apache.sling.junit.scriptable.ScriptableTestsProvider",
                        null,
                        "json"
                );
                executor.assertContentType("application/json");
                String content = executor.getContent();
                final JSONArray json = new JSONArray(new JSONTokener(content));

                int testsCount = 0;
                final List<String> failures = new ArrayList<String>();
                for(int i = 0 ; i < json.length(); i++) {
                    final JSONObject obj = json.getJSONObject(i);
                    if("test".equals(obj.getString("INFO_TYPE"))) {
                        testsCount++;
                        if(obj.has("failure")) {
                            failures.add(obj.get("failure").toString());
                        }
                    }
                }

                assertEquals("Expecting 1 scriptable tests: ", 1, testsCount);

                final int failureCount = test.willFail ? 1 : 0;
                if( failures.size() != failureCount) {
                    fail("Expected "
                            + failureCount + " failing tests but got " + failures.size()
                            + " for " + test.testScriptFile.getAbsolutePath()
                            + ": " + failures);
                }

            } finally {
                this.slingClient.delete(scriptPath);
            }
        }
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

                        final Description test = new Description(f, willFail);

                        this.tests.add(test);
                    }
                }
            }
        } else {
            logger.info("No test scripts found with resource path {}", resourcePath);
        }
    }
}