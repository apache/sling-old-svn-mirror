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
package org.apache.sling.osgi.installer.impl.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.impl.Logger;

/**
 * Store the digests and version numbers of installed bundles
 * in a file, to keep track of what we installed.
 */
class PersistentBundleInfo {

    private final Properties digests = new Properties();
    private final File dataFile;
    private static final String VERSION_PREFIX = "V:";

    /**
     * Load the list from supplied file, which is also
     * used by purgeAndSave to save our data.
     */
    PersistentBundleInfo(final File dataFile) {
        this.dataFile = dataFile;
        InputStream is = null;
        try {
            is = new FileInputStream(dataFile);
            digests.load(is);
            Logger.logInfo("Digests restored from data file " + dataFile.getName());
        } catch(IOException ioe) {
            Logger.logInfo("No digests retrieved, cannot read properties file " + dataFile.getName());
        } finally {
            if (is != null) {
                try {is.close(); } catch (final IOException ignore) {}
            }
        }
    }

    /**
     * Remove data which do not belongs to installed bundles,
     * and save our data
     */
    void purgeAndSave(TreeSet<String> installedBundlesSymbolicNames) throws IOException {
        final List<String> toRemove = new ArrayList<String>();
        for(Object o : digests.keySet()) {
            final String key = (String)o;
            if(!installedBundlesSymbolicNames.contains(key)
                    && !installedBundlesSymbolicNames.contains(key.substring(VERSION_PREFIX.length()))) {
                toRemove.add(key);
            }
        }
        for(String key : toRemove) {
            digests.remove(key);
        }

        OutputStream os = null;
        try {
            os = new FileOutputStream(dataFile);
            digests.store(os, "Stored by " + getClass().getName());
        } finally {
            if (os != null) {
                try {os.flush(); } catch (final IOException ignore) {}
                try {os.close(); } catch (final IOException ignore) {}
            }
        }
        Logger.logInfo("Stored digests of " + digests.size() + " bundles in data file " + dataFile.getName());
    }

    /**
     * Store a bundle digest - not persisted until purgeAndSave is called.
     */
    void putInfo(String bundleSymbolicName, String digest, String installedVersion) {
        digests.setProperty(bundleSymbolicName, digest);
        digests.setProperty(VERSION_PREFIX + bundleSymbolicName, installedVersion);
    }

    /**
     * Retrieve digest, null if not found.
     */
    String getDigest(String bundleSymbolicName) {
        return digests.getProperty(bundleSymbolicName);
    }

    /**
     * Retrieve installed version, null if not found.
     */
    String getInstalledVersion(String bundleSymbolicName) {
        return digests.getProperty(VERSION_PREFIX + bundleSymbolicName);
    }
}