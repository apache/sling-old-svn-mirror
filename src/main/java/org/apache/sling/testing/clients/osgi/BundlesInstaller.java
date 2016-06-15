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
package org.apache.sling.testing.clients.osgi;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.poller.AbstractPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Utility for installing and starting additional bundles for testing
 */
public class BundlesInstaller {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OsgiConsoleClient osgiConsoleClient;
    public static final String ACTIVE_STATE = "active";

    public BundlesInstaller(OsgiConsoleClient cc) {
        osgiConsoleClient = cc;
    }

    /**
     * Checks if a bundle is installed or not. Does not retry.
     * @param bundleFile
     * @return
     * @throws ClientException
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean isInstalled(File bundleFile) throws InterruptedException, IOException {
        final String bundleSymbolicName = OsgiConsoleClient.getBundleSymbolicName(bundleFile);
        log.debug("Checking if installed: " + bundleSymbolicName);
        boolean installed = osgiConsoleClient.checkBundleInstalled(bundleSymbolicName, 1000, 1);
        // if this succeeds, then there's no need to install again
        if (installed) {
            log.debug("Already installed: " + bundleSymbolicName);
            return true;
        } else {
            log.debug("Not yet installed: " + bundleSymbolicName);
            return false;
        }
    }

    /**
     * Check if the installed version matches the one of the bundle (file)
     * @param bundleFile
     * @return
     * @throws Exception
     */
    public boolean isInstalledWithSameVersion(File bundleFile) throws ClientException, IOException {
        final String bundleSymbolicName = OsgiConsoleClient.getBundleSymbolicName(bundleFile);
        final String versionOnServer = osgiConsoleClient.getBundleVersion(bundleSymbolicName);
        final String versionInBundle = OsgiConsoleClient.getBundleVersionFromFile(bundleFile);
        if (versionOnServer.equals(versionInBundle)) {
            return true;
        } else {
            log.warn("Installed bundle doesn't match: {}, versionOnServer={}, versionInBundle={}",
                    bundleSymbolicName, versionOnServer, versionInBundle);
            return false;
        }
    }

    /**
     * Install a list of bundles supplied as Files
     * @param toInstall
     * @param startBundles
     * @throws Exception
     */
    public void installBundles(List<File> toInstall, boolean startBundles) throws ClientException, IOException, InterruptedException {
        for(File f : toInstall) {
            final String bundleSymbolicName = OsgiConsoleClient.getBundleSymbolicName(f);
            if (isInstalled(f)) {
                if (f.getName().contains("SNAPSHOT")) {
                    log.info("Reinstalling (due to SNAPSHOT version): {}", bundleSymbolicName);
                    osgiConsoleClient.uninstallBundle(bundleSymbolicName);
                } else if (!isInstalledWithSameVersion(f)) {
                    log.info("Reinstalling (due to version mismatch): {}", bundleSymbolicName);
                    osgiConsoleClient.uninstallBundle(bundleSymbolicName);
                } else {
                    log.info("Not reinstalling: {}", bundleSymbolicName);
                    continue;
                }
            }
            osgiConsoleClient.installBundle(f, startBundles);
            log.info("Installed: {}", bundleSymbolicName);
        }

        // ensure that bundles are re-wired esp. if an existing bundle was updated
        osgiConsoleClient.refreshPackages();

        log.info("{} additional bundles installed", toInstall.size());
    }

    /**
     * Uninstall a list of bundles supplied as Files
     * @param toUninstall
     * @throws ClientException
     * @throws IOException
     * @throws InterruptedException
     */
    public void uninstallBundles(List<File> toUninstall) throws ClientException, IOException, InterruptedException {
        for(File f : toUninstall) {
            final String bundleSymbolicName = OsgiConsoleClient.getBundleSymbolicName(f);
            if (isInstalled(f)) {
                log.info("Uninstalling bundle: {}", bundleSymbolicName);
                osgiConsoleClient.uninstallBundle(bundleSymbolicName);
            } else {
                log.info("Could not uninstall: {} as it never was installed", bundleSymbolicName);
            }
        }

        // ensure that bundles are re-wired esp. if an existing bundle was updated
        osgiConsoleClient.refreshPackages();

        log.info("{} additional bundles uninstalled", toUninstall.size());
    }


    /**
     * Wait for all bundles specified in symbolicNames list to be installed in the OSGi web console.
     * @param symbolicNames the list of names for the bundles
     * @param timeoutSeconds how many seconds to wait
     * @return
     * @throws Exception
     */
    public boolean waitForBundlesInstalled(List<String> symbolicNames, int timeoutSeconds) throws ClientException, InterruptedException {
        log.info("Checking that the following bundles are installed (timeout {} seconds): {}", timeoutSeconds, symbolicNames);
        for (String symbolicName : symbolicNames) {
            boolean started = osgiConsoleClient.checkBundleInstalled(symbolicName, 500, 2 * timeoutSeconds);
            if (!started) return false;
        }
        return true;
    }

    /**
     * Start all the bundles in a {{List}}
     * @param symbolicNames the list of bundles to start
     * @param timeoutSeconds number of seconds until it times out
     * @throws ClientException
     * @throws InterruptedException
     */
    public void startAllBundles(final List<String> symbolicNames, int timeoutSeconds) throws ClientException, InterruptedException {
        log.info("Starting bundles (timeout {} seconds): {}", timeoutSeconds, symbolicNames);
        class StartAllBundlesPoller extends AbstractPoller {
            private ClientException exception;
            public StartAllBundlesPoller(List<String> symbolicNames, long waitInterval, long waitCount) {
                super(waitInterval, waitCount);
            }

            @Override
            public boolean call() {
                for (String bundle : symbolicNames) {
                    final String state;
                    try {
                        state = osgiConsoleClient.getBundleState(bundle);
                        if (!state.equalsIgnoreCase(ACTIVE_STATE)) {
                            osgiConsoleClient.startBundle(bundle);
                        }
                    } catch (ClientException e) {
                        this.exception = e;
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean condition() {
                for (String bundle : symbolicNames) {
                    final String state;
                    try {
                        state = osgiConsoleClient.getBundleState(bundle);
                        if (!state.equalsIgnoreCase(ACTIVE_STATE)) {
                            return false;
                        }
                    } catch (ClientException e) {
                        this.exception = e;
                        return false;
                    }
                }
                return true;
            }

            public ClientException getException() {
                return exception;
            }
        }
        StartAllBundlesPoller poller = new StartAllBundlesPoller(symbolicNames, 1000, timeoutSeconds);
        if (!poller.callUntilCondition()) {
            throw new ClientException("Some bundles did not start or timed out", poller.getException());
        }

    }




}