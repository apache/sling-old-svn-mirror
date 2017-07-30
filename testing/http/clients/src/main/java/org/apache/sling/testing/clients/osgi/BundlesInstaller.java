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
import org.apache.sling.testing.clients.util.poller.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;


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
     * @param bundleFile bundle file
     * @return true if the bundle is installed
     * @throws ClientException if the state of the bundle could not be determined
     */
    public boolean isInstalled(File bundleFile) throws ClientException {
        String bundleSymbolicName = "";
        try {
            bundleSymbolicName = OsgiConsoleClient.getBundleSymbolicName(bundleFile);
            log.debug("Checking if installed: " + bundleSymbolicName);

            osgiConsoleClient.getBundleState(bundleSymbolicName);
            log.debug("Already installed: " + bundleSymbolicName);
            return true;
        } catch (ClientException e) {
            log.debug("Not yet installed: " + bundleSymbolicName);
            return false;
        } catch (IOException e) {
            log.debug("Failed to retrieve bundle symbolic name from file. ", e);
            throw new ClientException("Failed to retrieve bundle symbolic name from file. ", e);
        }
    }

    /**
     * Check if the installed version matches the one of the bundle (file)
     * @param bundleFile bundle file
     * @return true if the bundle is installed and has the same version
     * @throws ClientException if the installed version cannot be retrieved
     * @throws IOException if the file version cannot be read
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
     * @param toInstall list ob bundles to install
     * @param startBundles whether to start the bundles
     * @throws ClientException if an error occurs during installation
     */
    public void installBundles(List<File> toInstall, boolean startBundles) throws ClientException, IOException {
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
     * @param toUninstall bundles to uninstall
     * @throws ClientException if one of the requests failed
     * @throws IOException if the files cannot be read from disk
     */
    public void uninstallBundles(List<File> toUninstall) throws ClientException, IOException {
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
     * @deprecated use {@link #waitBundlesInstalled(List, long)}
     * @param symbolicNames the list of names for the bundles
     * @param timeoutSeconds how many seconds to wait
     * @return true if all the bundles were installed
     */
    @Deprecated
    public boolean waitForBundlesInstalled(List<String> symbolicNames, int timeoutSeconds) throws ClientException, InterruptedException {
        log.info("Checking that the following bundles are installed (timeout {} seconds): {}", timeoutSeconds, symbolicNames);
        for (String symbolicName : symbolicNames) {
            boolean started = osgiConsoleClient.checkBundleInstalled(symbolicName, 500, 2 * timeoutSeconds);
            if (!started) return false;
        }
        return true;
    }

    /**
     * Wait for multiple bundles to be installed in the OSGi web console.
     * @param symbolicNames the list bundles to be checked
     * @param timeout max total time to wait for all bundles, in ms, before throwing {@code TimeoutException}
     * @throws TimeoutException if the timeout was reached before all the bundles were installed
     * @throws InterruptedException to mark this operation as "waiting", callers should rethrow it
     */
    public void waitBundlesInstalled(List<String> symbolicNames, long timeout)
            throws InterruptedException, TimeoutException {
        log.info("Checking that the following bundles are installed (timeout {} ms): {}", timeout, symbolicNames);
        long start = System.currentTimeMillis();
        for (String symbolicName : symbolicNames) {
            osgiConsoleClient.waitBundleInstalled(symbolicName, timeout, 500);

            if (System.currentTimeMillis() > start + timeout) {
                throw new TimeoutException("Waiting for bundles did not finish in " + timeout + " ms.");
            }
        }
    }

    /**
     * Start all the bundles in a {{List}}
     * @param symbolicNames the list of bundles to start
     * @param timeout total max time to wait for all the bundles, in ms
     * @throws TimeoutException if the timeout is reached before all the bundles are started
     * @throws InterruptedException to mark this operation as "waiting", callers should rethrow it
     */
    public void startAllBundles(final List<String> symbolicNames, int timeout) throws InterruptedException, TimeoutException {
        log.info("Starting bundles (timeout {} seconds): {}", timeout, symbolicNames);

        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                boolean allActive = true;
                for (String bundle : symbolicNames) {
                    String state = osgiConsoleClient.getBundleState(bundle);
                    if (!state.equalsIgnoreCase(ACTIVE_STATE)) {
                        osgiConsoleClient.startBundle(bundle);
                        allActive = false;
                    }
                }
                return allActive;
            }
        };

        p.poll(timeout, 500);
    }
}
