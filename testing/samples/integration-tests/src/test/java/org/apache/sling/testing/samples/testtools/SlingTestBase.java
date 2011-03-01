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
package org.apache.sling.testing.samples.testtools;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.junit.TimeoutsProvider;
import org.apache.sling.testing.tools.http.RequestBuilder;
import org.apache.sling.testing.tools.http.RequestExecutor;
import org.apache.sling.testing.tools.jarexec.JarExecutor;
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
    
    protected static String serverBaseUrl;
    protected static RequestBuilder builder;
    protected static DefaultHttpClient httpClient = new DefaultHttpClient();
    protected static RequestExecutor executor = new RequestExecutor(httpClient);
    
    private static boolean serverStarted;
    private static boolean serverStartedByThisClass;
    private static boolean serverReady;
    private static boolean serverReadyTestFailed;
    private static final Logger log = LoggerFactory.getLogger(SlingTestBase.class);
    
    protected SlingTestBase() {
        try {
            startRunnableJar();
            waitForServerReady();
            blockIfRequested();
        } catch(Exception e) {
            throw new IllegalStateException("Exception in SlingTestBase constructor", e);
        }
    }

    /** Start the configured runnable jar and initialize our http client */
    private synchronized void startRunnableJar() throws Exception {
        if(serverStarted) {
            return;
        }
        
        final String configuredUrl = System.getProperty(TEST_SERVER_URL_PROP);
        if(configuredUrl != null) {
            serverBaseUrl = configuredUrl;
            log.info(TEST_SERVER_URL_PROP + " is set: not starting server jar (" + serverBaseUrl + ")");
        } else {
            final JarExecutor j = JarExecutor.getInstance(System.getProperties());
            log.info(TEST_SERVER_URL_PROP + " not set, starting server jar {}", j);
            j.start();
            serverBaseUrl = "http://localhost:" + j.getServerPort();
            serverStartedByThisClass = true;
        }
        
        serverStarted = true;
        builder = new RequestBuilder(serverBaseUrl);
    }
    
    /** Optionally block here so that the runnable jar stays up - we can 
     *  then run tests against it from another VM.
     */
    private void blockIfRequested() {
        if ("true".equals(System.getProperty(KEEP_JAR_RUNNING_PROP))) {
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
    private void waitForServerReady() throws Exception {
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
                    log.info("Request to {}{} failed, will retry ({})", 
                            new Object[] { serverBaseUrl, path, ae});
                } catch(Exception e) {
                    errors = true;
                    log.info("Request to {}{} failed, will retry ({})",
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
        
        if(serverReady) {
            onServerReady();
        } else {
            serverReadyTestFailed = true;
            final String msg = "Server not ready after " + timeoutSec + " seconds, giving up";
            log.info(msg);
            fail(msg);
        }
    }
    
    /** Called once when the server is found to be ready, can be used for additional
     *  server setup (extra bundles etc.). If overridden, must be called by overriding
     *  method. 
     */
    protected void onServerReady() throws Exception {
        if(serverStartedByThisClass) {
            installExtraBundles();
        } else {
            // Assume extra bundles are already in place, avoid transient effects
            // caused by updating them
            log.info("Server was not started here, additional bundles will not be installed");
        }
    }
    
    /** Install all bundles found under our additional bundles path */
    protected void installExtraBundles() throws Exception {
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
                    installBundle(new File(dir, bundleFilename));
                    count++;
                }
            }
        }
        
        log.info("{} additional bundles installed from {}", count, dir.getAbsolutePath());
    }
 
    /** Install a bundle using the Felix webconsole HTTP interface */
    protected void installBundle(File f) throws Exception {
        log.info("Installing additional bundle {}", f.getName());
        
        // Setup request for Felix Webconsole bundle install
        final MultipartEntity entity = new MultipartEntity();
        entity.addPart("action",new StringBody("install"));
        entity.addPart("bundlestart", new StringBody("true"));
        entity.addPart("bundlefile", new FileBody(f));
        
        // Console returns a 302 on success (and in a POST this
        // is not handled automatically as per HTTP spec)
        executor.execute(
                builder.buildPostRequest("/system/console/bundles")
                .withCredentials(ADMIN, ADMIN)
                .withEntity(entity)
        ).assertStatus(302);
    }
    
    protected boolean isServerStartedByThisClass() {
        return serverStartedByThisClass;
    }
}