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
package org.apache.sling.osgi.installer.impl;

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

import org.osgi.service.log.LogService;

/** Store bundle digests in a file, to avoid re-installing
 *  snapshots needlessly when restarting.
 */
class BundleDigestsStorage {
    private Properties digests = new Properties();
    private final File dataFile;
    private final OsgiInstallerContext ctx;
    
    /** Load the list from supplied file, which is also
     *  used by purgeAndSave to save our data
     */
    BundleDigestsStorage(OsgiInstallerContext ctx, File dataFile) throws IOException {
        this.ctx = ctx;
        this.dataFile = dataFile;
        InputStream is = null;
        try {
            is = new FileInputStream(dataFile);
            digests.load(is);
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_INFO, "Digests restored from data file " + dataFile.getName());
            }
        } catch(IOException ioe) {
            if(ctx.getLogService() != null) {
                ctx.getLogService().log(LogService.LOG_INFO, 
                        "No digests retrieved, cannot read properties file " + dataFile.getName());
            }
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }
    
    /** Remove digests which do not belong to installed bundles,
     *  and save our data
     */
    void purgeAndSave(TreeSet<String> installedBundlesSymbolicNames) throws IOException {
        final List<String> toRemove = new ArrayList<String>();
        for(Object o : digests.keySet()) {
            final String key = (String)o;
            if(!installedBundlesSymbolicNames.contains(key)) {
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
            if(os != null) {
                os.flush();
                os.close();
            }
        }
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_INFO, 
                    "Stored digests of " + digests.size() + " bundles in data file " + dataFile.getName());
        }
    }
    
    /** Store a bundle digest - not persisted until purgeAndSave is called */
    void putDigest(String bundleSymbolicName, String digest) {
        digests.setProperty(bundleSymbolicName, digest);
    }
    
    /** Retrieve digest, null if not found */
    String getDigest(String bundleSymbolicName) {
        return digests.getProperty(bundleSymbolicName);
    }
}