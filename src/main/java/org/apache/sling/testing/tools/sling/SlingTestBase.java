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
package org.apache.sling.testing.tools.sling;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor;
import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for running tests against a Sling instance,
 *  takes care of starting Sling and waiting for it to be ready.
 */
public class SlingTestBase implements SlingInstance {
    public static final String TEST_SERVER_URL_PROP = "test.server.url";
    public static final String TEST_SERVER_USERNAME = "test.server.username";
    public static final String TEST_SERVER_PASSWORD = "test.server.password";
    public static final String SERVER_READY_TIMEOUT_PROP = "server.ready.timeout.seconds";
    public static final String SERVER_READY_PROP_PREFIX = "server.ready.path";
    public static final String KEEP_JAR_RUNNING_PROP = "keepJarRunning";
    public static final String SERVER_HOSTNAME_PROP = "test.server.hostname";
    public static final String ADDITONAL_BUNDLES_PATH = "additional.bundles.path";
    public static final String BUNDLE_TO_INSTALL_PREFIX = "sling.additional.bundle";
    public static final String START_BUNDLES_TIMEOUT_SECONDS = "start.bundles.timeout.seconds";
    public static final String BUNDLE_INSTALL_TIMEOUT_SECONDS = "bundle.install.timeout.seconds";
    public static final String ADMIN = "admin";

    private final boolean keepJarRunning;
    private final String serverUsername;
    private final String serverPassword;
    private final SlingInstanceState slingTestState;
    private final Properties systemProperties;
    private RequestBuilder builder;
    private DefaultHttpClient httpClient = new DefaultHttpClient();
    private RequestExecutor executor = new RequestExecutor(httpClient);
    private WebconsoleClient webconsoleClient;
    private BundlesInstaller bundlesInstaller;
    private boolean serverStartedByThisClass;


    private final Logger log = LoggerFactory.getLogger(getClass());


    public SlingTestBase() {
        this(SlingInstanceState.getInstance(SlingInstanceState.DEFAULT_INSTANCE_NAME),
                System.getProperties());
    }

    /** Get configuration but do not start server yet, that's done on demand */
    public SlingTestBase(SlingInstanceState slingTestState, Properties systemProperties) {
        this.slingTestState = slingTestState;
        this.systemProperties = systemProperties;
        this.keepJarRunning = "true".equals(systemProperties.getProperty(KEEP_JAR_RUNNING_PROP));


        final String configuredUrl = systemProperties.getProperty(TEST_SERVER_URL_PROP, systemProperties.getProperty("launchpad.http.server.url"));
        if(configuredUrl != null) {
            slingTestState.setServerBaseUrl(configuredUrl);
            slingTestState.setServerStarted(true);
        } else {
            synchronized(slingTestState) {
                try {
                    if(slingTestState.getJarExecutor() == null) {
                        slingTestState.setJarExecutor(new JarExecutor(systemProperties));
                    }
                } catch(Exception e) {
                    log.error("JarExecutor setup failed", e);
                    fail("JarExecutor setup failed: " + e);
                }
            }
            String serverHost = systemProperties.getProperty(SERVER_HOSTNAME_PROP);
            if(serverHost == null || serverHost.trim().length() == 0) {
                serverHost = "localhost";
            }
            slingTestState.setServerBaseUrl("http://" + serverHost + ":" + slingTestState.getJarExecutor().getServerPort());
        }

        // Set configured username using "admin" as default credential
        final String configuredUsername = systemProperties.getProperty(TEST_SERVER_USERNAME);
        if (configuredUsername != null && configuredUsername.trim().length() > 0) {
            serverUsername = configuredUsername;
        } else {
            serverUsername = ADMIN;
        }

        // Set configured password using "admin" as default credential
        final String configuredPassword = systemProperties.getProperty(TEST_SERVER_PASSWORD);
        if (configuredPassword != null && configuredPassword.trim().length() > 0) {
            serverPassword = configuredPassword;
        } else {
            serverPassword = ADMIN;
        }

        builder = new RequestBuilder(slingTestState.getServerBaseUrl());
        webconsoleClient = new WebconsoleClient(slingTestState.getServerBaseUrl(), serverUsername, serverPassword);
        builder = new RequestBuilder(slingTestState.getServerBaseUrl());
        bundlesInstaller = new BundlesInstaller(webconsoleClient);

        if(!slingTestState.isServerInfoLogged()) {
            log.info("Server base URL={}", slingTestState.getServerBaseUrl());
            slingTestState.setServerInfoLogged(true);
        }
    }

    /** Start the server, if not done yet */
    private void startServerIfNeeded() {
        try {
            if(slingTestState.isServerStarted() && !serverStartedByThisClass && !slingTestState.isStartupInfoProvided()) {
                log.info(TEST_SERVER_URL_PROP + " was set: not starting server jar (" + slingTestState.getServerBaseUrl() + ")");
            }
            if(!slingTestState.isServerStarted()) {
                synchronized (slingTestState) {
                    if(!slingTestState.isServerStarted()) {
                        slingTestState.getJarExecutor().start();
                        serverStartedByThisClass = true;
                        if(!slingTestState.setServerStarted(true)) {
                            fail("A server is already started at " + slingTestState.getServerBaseUrl());
                        }
                    }
                }
            }
            slingTestState.setStartupInfoProvided(true);
            waitForServerReady();
            installAdditionalBundles();
            blockIfRequested();
        } catch(Exception e) {
            log.error("Exception in maybeStartServer()", e);
            fail("maybeStartServer() failed: " + e);
        }
    }

    protected void installAdditionalBundles() {
        if(slingTestState.isInstallBundlesFailed()) {
            fail("Bundles could not be installed, cannot run tests");
        } else if(!slingTestState.isExtraBundlesInstalled()) {
            final String paths = systemProperties.getProperty(ADDITONAL_BUNDLES_PATH);
            if(paths == null) {
                log.info("System property {} not set, additional bundles won't be installed",
                        ADDITONAL_BUNDLES_PATH);
            } else {
                final List<File> toInstall = new ArrayList<File>();
                try {
                    // Paths can contain a comma-separated list
                    final String [] allPaths = paths.split(",");
                    for(String path : allPaths) {
                        toInstall.addAll(getBundlesToInstall(path.trim()));
                    }
                    
                    // Install bundles, check that they are installed and start them all
                    bundlesInstaller.installBundles(toInstall, false);
                    final List<String> symbolicNames = new LinkedList<String>();
                    for(File f : toInstall) {
                        symbolicNames.add(bundlesInstaller.getBundleSymbolicName(f));
                    }
                    bundlesInstaller.waitForBundlesInstalled(symbolicNames,
                            TimeoutsProvider.getInstance().getTimeout(BUNDLE_INSTALL_TIMEOUT_SECONDS, 10));
                    bundlesInstaller.startAllBundles(symbolicNames,
                            TimeoutsProvider.getInstance().getTimeout(START_BUNDLES_TIMEOUT_SECONDS, 30));
                } catch(AssertionError ae) {
                    log.info("Exception while installing additional bundles", ae);
                    slingTestState.setInstallBundlesFailed(true);
                } catch(Exception e) {
                    log.info("Exception while installing additional bundles", e);
                    slingTestState.setInstallBundlesFailed(true);
                }

                if(slingTestState.isInstallBundlesFailed()) {
                    fail("Could not start all installed bundles:" + toInstall);
                }
            }
        }

        slingTestState.setExtraBundlesInstalled(!slingTestState.isInstallBundlesFailed());
    }

    /** Start server if needed, and return a RequestBuilder that points to it */
    public RequestBuilder getRequestBuilder() {
        startServerIfNeeded();
        return builder;
    }

    /** Start server if needed, and return its base URL */
    public String getServerBaseUrl() {
        startServerIfNeeded();
        return slingTestState.getServerBaseUrl();
    }

    /** Return username configured for execution of HTTP requests */
    public String getServerUsername() {
        return serverUsername;
    }

    /** Return password configured for execution of HTTP requests */
    public String getServerPassword() {
        return serverPassword;
    }

    /** Optionally block here so that the runnable jar stays up - we can
     *  then run tests against it from another VM.
     */
    protected void blockIfRequested() {
        if (keepJarRunning) {
            log.info(KEEP_JAR_RUNNING_PROP + " set to true - entering infinite loop"
                     + " so that runnable jar stays up. Kill this process to exit.");
            synchronized (slingTestState) {
                try {
                    wait();
                } catch(InterruptedException iex) {
                    log.info("InterruptedException in blockIfRequested");
                }
            }
        }
    }

    /** Check a number of server URLs for readyness */
    protected void waitForServerReady() throws Exception {
        if(slingTestState.isServerReady()) {
            return;
        }
        if(slingTestState.isServerReadyTestFailed()) {
            fail("Server is not ready according to previous tests");
        }

        // Timeout for readiness test
        final String sec = systemProperties.getProperty(SERVER_READY_TIMEOUT_PROP);
        final int timeoutSec = TimeoutsProvider.getInstance().getTimeout(sec == null ? 60 : Integer.valueOf(sec));
        log.info("Will wait up to " + timeoutSec + " seconds for server to become ready");
        final long endTime = System.currentTimeMillis() + timeoutSec * 1000L;

        // Get the list of paths to test and expected content regexps
        final List<String> testPaths = new ArrayList<String>();
        final TreeSet<Object> propertyNames = new TreeSet<Object>();
        propertyNames.addAll(systemProperties.keySet());
        for(Object o : propertyNames) {
            final String key = (String)o;
            if(key.startsWith(SERVER_READY_PROP_PREFIX)) {
                testPaths.add(systemProperties.getProperty(key));
            }
        }

        // Consider the server ready if it responds to a GET on each of
        // our configured request paths with a 200 result and content
        // that contains the pattern that's optionally supplied with the
        // path, separated by a colon
        log.info("Checking that GET requests return expected content (timeout={} seconds): {}", timeoutSec, testPaths);
        while(System.currentTimeMillis() < endTime) {
            boolean errors = false;
            for(String p : testPaths) {
                final String [] s = p.split(":");
                final String path = s[0];
                final String pattern = (s.length > 0 ? s[1] : "");
                try {
                    executor.execute(builder.buildGetRequest(path).withCredentials(serverUsername, serverPassword))
                    .assertStatus(200)
                    .assertContentContains(pattern);
                } catch(AssertionError ae) {
                    errors = true;
                    log.debug("Request to {}@{}{} failed, will retry ({})",
                            new Object[] { serverUsername, slingTestState.getServerBaseUrl(), path, ae});
                } catch(Exception e) {
                    errors = true;
                    log.debug("Request to {}@{}{} failed, will retry ({})",
                            new Object[] { serverUsername, slingTestState.getServerBaseUrl(), path, pattern, e });
                }
            }

            if(!errors) {
                slingTestState.setServerReady(true);
                log.info("All {} paths return expected content, server ready", testPaths.size());
                break;
            }
            Thread.sleep(TimeoutsProvider.getInstance().getTimeout(1000L));
        }

        if(!slingTestState.isServerReady()) {
            slingTestState.setServerReadyTestFailed(true);
            final String msg = "Server not ready after " + timeoutSec + " seconds, giving up";
            log.info(msg);
            fail(msg);
        }
    }

    /** Get the list of additional bundles to install, as specified by path parameter */
    protected List<File> getBundlesToInstall(String additionalBundlesPath) {
        final List<File> result = new LinkedList<File>();
        if(additionalBundlesPath == null) {
            return result;
        }

        final File dir = new File(additionalBundlesPath);
        if(!dir.isDirectory() || !dir.canRead()) {
            log.info("Cannot read additional bundles directory {}, ignored", dir.getAbsolutePath());
            return result;
        }

        // Collect all filenames of candidate bundles
        final List<String> bundleNames = new ArrayList<String>();
        final String [] files = dir.list();
        if(files != null) {
            for(String file : files) {
                if(file.endsWith(".jar")) {
                    bundleNames.add(file);
                }
            }
        }

        // We'll install those that are specified by system properties, in order
        final List<String> sortedPropertyKeys = new ArrayList<String>();
        for(Object key : systemProperties.keySet()) {
            final String str = key.toString();
            if(str.startsWith(BUNDLE_TO_INSTALL_PREFIX)) {
                sortedPropertyKeys.add(str);
            }
        }
        Collections.sort(sortedPropertyKeys);
        for(String key : sortedPropertyKeys) {
            final String filenamePrefix = systemProperties.getProperty(key);
            for(String bundleFilename : bundleNames) {
                if(bundleFilename.startsWith(filenamePrefix)) {
                    result.add(new File(dir, bundleFilename));
                }
            }
        }

        return result;
    }

    public boolean isServerStartedByThisClass() {
        return serverStartedByThisClass;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public RequestExecutor getRequestExecutor() {
        return executor;
    }

    public WebconsoleClient getWebconsoleClient() {
        startServerIfNeeded();
        return webconsoleClient;
    }
}
