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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.testing.tools.osgi.WebconsoleClient;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility that installs and starts additional bundles for testing */ 
public class BundlesInstaller {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final WebconsoleClient webconsoleClient;
    public static final String ACTIVE_STATE = "active";
    
    public BundlesInstaller(WebconsoleClient wcc) {
        webconsoleClient = wcc;
    }
   
    public boolean isInstalled(File bundleFile) throws Exception {
        final String bundleSymbolicName = getBundleSymbolicName(bundleFile);
        try{
            log.debug("Checking if installed: "+bundleSymbolicName);
            webconsoleClient.checkBundleInstalled(bundleSymbolicName, 1);
            // if this succeeds, then there's no need to install again
            log.debug("Already installed: "+bundleSymbolicName);
            return true;
        } catch(AssertionError e) {
            log.debug("Not yet installed: "+bundleSymbolicName);
            return false;
        }

    }
    
    /** Check if the installed version matches the one of the bundle (file) **/
    public boolean isInstalledWithSameVersion(File bundleFile) throws Exception {
        final String bundleSymbolicName = getBundleSymbolicName(bundleFile);
        final String versionOnServer = webconsoleClient.getBundleVersion(bundleSymbolicName);
        final String versionInBundle = getBundleVersion(bundleFile);
        if (versionOnServer.equals(versionInBundle)) {
            return true;
        } else {
            log.info("Bundle installed doesn't match: "+bundleSymbolicName+
                    ", versionOnServer="+versionOnServer+", versionInBundle="+versionInBundle);
            return false;
        }
    }
    
    /** Install a list of bundles supplied as Files */
    public void installBundles(List<File> toInstall, boolean startBundles) throws Exception {
        for(File f : toInstall) {
            final String bundleSymbolicName = getBundleSymbolicName(f);
            if (isInstalled(f)) {
                if (f.getName().contains("SNAPSHOT")) {
                    log.info("Reinstalling (due to SNAPSHOT version): "+bundleSymbolicName);
                    webconsoleClient.uninstallBundle(bundleSymbolicName, f);
                } else if (!isInstalledWithSameVersion(f)) {
                    log.info("Reinstalling (due to version mismatch): "+bundleSymbolicName);
                    webconsoleClient.uninstallBundle(bundleSymbolicName, f);
                } else {
                    log.info("Not reinstalling: "+bundleSymbolicName);
                    continue;
                }
            }
            webconsoleClient.installBundle(f, startBundles);
        }
        log.info("{} additional bundles installed from {}", toInstall.size(), toInstall.get(0).getAbsolutePath());
    }
    
    /** Wait for all bundles specified in symbolicNames list to be installed in the
     *  remote web console.
     */
    public void waitForBundlesInstalled(List<String> symbolicNames, int timeoutSeconds) throws Exception {
        log.info("Checking that bundles are installed (timeout {} seconds): {}", timeoutSeconds, symbolicNames);
        for(String symbolicName : symbolicNames) {
            webconsoleClient.checkBundleInstalled(symbolicName, timeoutSeconds);
        }
    }
    
    public void startAllBundles(List<String> symbolicNames, int timeoutSeconds) throws Exception {
        log.info("Starting bundles (timeout {} seconds): {}", timeoutSeconds, symbolicNames);
        
        final long timeout = System.currentTimeMillis() + timeoutSeconds * 1000L;
        final List<String> toStart = new LinkedList<String>();
        while(System.currentTimeMillis() < timeout) {
            toStart.clear();
            for(String name : symbolicNames) {
                final String state = webconsoleClient.getBundleState(name);
                if(!state.equalsIgnoreCase(ACTIVE_STATE)) {
                    toStart.add(name);
                    break;
                }
            }
            
            if(toStart.isEmpty()) {
                log.info("Ok - all bundles are in the {} state", ACTIVE_STATE);
                break;
            }
            
            for(String name : toStart) {
                webconsoleClient.startBundle(name);
            }
            
            Thread.sleep(500L);
        }
        
        if(!toStart.isEmpty()) {
            fail("Some bundles did not start: " + toStart);
        }
    }
    
    public String getBundleSymbolicName(File bundleFile) throws IOException {
        String name = null;
        final JarInputStream jis = new JarInputStream(new FileInputStream(bundleFile));
        try {
            final Manifest m = jis.getManifest();
            if(m == null) {
                fail("Manifest is null in " + bundleFile.getAbsolutePath());
            }
            name = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        } finally {
            jis.close();
        }
        return name;
    }
    
    public String getBundleVersion(File bundleFile) throws IOException {
        String version = null;
        final JarInputStream jis = new JarInputStream(new FileInputStream(bundleFile));
        try {
            final Manifest m = jis.getManifest();
            if(m == null) {
                fail("Manifest is null in " + bundleFile.getAbsolutePath());
            }
            version = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        } finally {
            jis.close();
        }
        return version;
    }
}