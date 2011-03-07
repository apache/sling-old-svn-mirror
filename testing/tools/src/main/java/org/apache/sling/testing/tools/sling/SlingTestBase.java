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
import java.util.List;
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
public class SlingTestBase {
    public static final String TEST_SERVER_URL_PROP = "test.server.url";
    public static final String SERVER_READY_TIMEOUT_PROP = "server.ready.timeout.seconds";
    public static final String SERVER_READY_PROP_PREFIX = "server.ready.path";
    public static final String KEEP_JAR_RUNNING_PROP = "keepJarRunning";
    public static final String ADDITONAL_BUNDLES_PATH = "additional.bundles.path";
    public static final String BUNDLE_TO_INSTALL_PREFIX = "sling.additional.bundle";
    public static final String ADMIN = "admin";
    
    private static final boolean keepJarRunning = "true".equals(System.getProperty(KEEP_JAR_RUNNING_PROP));
    private final String serverBaseUrl;
    private static RequestBuilder builder;
    private static DefaultHttpClient httpClient = new DefaultHttpClient();
    private static RequestExecutor executor = new RequestExecutor(httpClient);
    private static WebconsoleClient webconsoleClient;
    private static boolean serverStarted;
    private static boolean serverStartedByThisClass;
    private static boolean serverReady;
    private static boolean serverReadyTestFailed;
    private static boolean extraBundlesInstalled;
    private static boolean startupInfoProvided;

    private static final Logger log = LoggerFactory.getLogger(SlingTestBase.class);
    private static JarExecutor jarExecutor;
    
    /** Get configuration but do not start server yet, that's done on demand */
    public SlingTestBase() {
        if(jarExecutor == null) {
            synchronized(this) {
                try {
                    jarExecutor = new JarExecutor(System.getProperties());
                } catch(Exception e) {
                    log.error("JarExecutor setup failed", e);
                    fail("JarExecutor setup failed: " + e);
                }
            }
        }
        
        final String configuredUrl = System.getProperty(TEST_SERVER_URL_PROP);
        if(configuredUrl != null) {
            serverBaseUrl = configuredUrl;
            serverStarted = true;
        } else {
            serverBaseUrl = "http://localhost:" + jarExecutor.getServerPort();
        }
        
        builder = new RequestBuilder(serverBaseUrl);
        webconsoleClient = new WebconsoleClient(serverBaseUrl, ADMIN, ADMIN);
        builder = new RequestBuilder(serverBaseUrl);
    }

    /** Start the server, if not done yet */
    private void startServerIfNeeded() {
        try {
            if(serverStarted && !serverStartedByThisClass && !startupInfoProvided) {
                log.info(TEST_SERVER_URL_PROP + " was set: not starting server jar (" + serverBaseUrl + ")");
            }
            if(!serverStarted) {
                synchronized (jarExecutor) {
                    if(!serverStarted) {
                        jarExecutor.start();
                        serverStartedByThisClass = true;
                        serverStarted = true;
                    }
                }
            }
            startupInfoProvided = true;
            waitForServerReady();
            installExtraBundles();
            blockIfRequested();
        } catch(Exception e) {
            log.error("Exception in maybeStartServer()", e);
            fail("maybeStartServer() failed: " + e);
        }
    }
    
    /** Start server if needed, and return a RequestBuilder that points to it */
    protected RequestBuilder getRequestBuilder() {
        startServerIfNeeded();
        return builder;
    }

    /** Start server if needed, and return its base URL */
    protected String getServerBaseUrl() {
        startServerIfNeeded();
        return serverBaseUrl;
    }

    /** Optionally block here so that the runnable jar stays up - we can 
     *  then run tests against it from another VM.
     */
    protected void blockIfRequested() {
        if (keepJarRunning) {
            log.info(KEEP_JAR_RUNNING_PROP + " set to true - entering infinite loop"
                     + " so that runnable jar stays up. Kill this process to exit.");
            synchronized (this) {
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
        if(serverReady) {
            return;
        }
        if(serverReadyTestFailed) {
            fail("Server is not ready according to previous tests");
        }
        
        // Timeout for readiness test
        final String sec = System.getProperty(SERVER_READY_TIMEOUT_PROP);
        final int timeoutSec = TimeoutsProvider.getInstance().getTimeout(sec == null ? 60 : Integer.valueOf(sec));
        log.info("Will wait up to " + timeoutSec + " seconds for server to become ready");
        final long endTime = System.currentTimeMillis() + timeoutSec * 1000L;

        // Get the list of paths to test and expected content regexps
        final List<String> testPaths = new ArrayList<String>();
        final TreeSet<Object> propertyNames = new TreeSet<Object>();
        propertyNames.addAll(System.getProperties().keySet());
        for(Object o : propertyNames) {
            final String key = (String)o;
            if(key.startsWith(SERVER_READY_PROP_PREFIX)) {
                testPaths.add(System.getProperty(key));
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
                    executor.execute(builder.buildGetRequest(path))
                    .assertStatus(200)
                    .assertContentContains(pattern);
                } catch(AssertionError ae) {
                    errors = true;
                    log.debug("Request to {}{} failed, will retry ({})", 
                            new Object[] { serverBaseUrl, path, ae});
                } catch(Exception e) {
                    errors = true;
                    log.debug("Request to {}{} failed, will retry ({})",
                            new Object[] { serverBaseUrl, path, pattern, e });
                }
            }
            
            if(!errors) {
                serverReady = true;
                log.info("All {} paths return expected content, server ready", testPaths.size());
                break;
            }
            Thread.sleep(TimeoutsProvider.getInstance().getTimeout(1000L));
        }
        
        if(!serverReady) {
            serverReadyTestFailed = true;
            final String msg = "Server not ready after " + timeoutSec + " seconds, giving up";
            log.info(msg);
            fail(msg);
        }
    }
    
    /** Install all bundles found under our additional bundles path */
    protected void installExtraBundles() throws Exception {
        if(extraBundlesInstalled) {
            return;
        }
        extraBundlesInstalled = true;
        
        if(!serverStartedByThisClass) {
            log.info("Server was not started here, additional bundles will not be installed");
            return;
        }
        
        final String path = System.getProperty(ADDITONAL_BUNDLES_PATH);
        if(path == null) {
            log.info("System property {} not set, additional bundles won't be installed", 
                    ADDITONAL_BUNDLES_PATH);
            return;
        }
        
        final File dir = new File(path);
        if(!dir.isDirectory() || !dir.canRead()) {
            log.info("Cannot read additional bundles directory {}, ignored", dir.getAbsolutePath());
            return;
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
        
        // And install those that are specified by system properties, in order
        int count = 0;
        final List<String> sortedPropertyKeys = new ArrayList<String>();
        for(Object key : System.getProperties().keySet()) {
            final String str = key.toString();
            if(str.startsWith(BUNDLE_TO_INSTALL_PREFIX)) {
                sortedPropertyKeys.add(str);
            }
        }
        Collections.sort(sortedPropertyKeys);
        for(String key : sortedPropertyKeys) {
            final String filenamePrefix = System.getProperty(key);
            for(String bundleFilename : bundleNames) {
                if(bundleFilename.startsWith(filenamePrefix)) {
                    webconsoleClient.installBundle(new File(dir, bundleFilename), true);
                    count++;
                }
            }
        }
        
        log.info("{} additional bundles installed from {}", count, dir.getAbsolutePath());
    }
 
    protected boolean isServerStartedByThisClass() {
        return serverStartedByThisClass;
    }
    
    protected HttpClient getHttpClient() {
        return httpClient;
    }
    
    protected RequestExecutor getRequestExecutor() {
        return executor;
    }
    
    protected WebconsoleClient getWebconsoleClient() {
        startServerIfNeeded();
        return webconsoleClient;
    }
}