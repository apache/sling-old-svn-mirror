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
package org.apache.sling.installer.core.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleBlackList {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BundleBlackList(BundleContext bc) {
        BufferedReader r = null;
        try {
            String launchpadFolder = bc.getProperty("sling.launchpad");

            String filepath = launchpadFolder + File.separator + BOOTSTRAP_FILENAME;
            logger.debug("Parsing {} for uninstall directives to be used as blacklist", filepath);

            r = new BufferedReader(new FileReader(filepath));
            getBlackListFromBootstrapFile(r);
        } catch (IOException ignore) {
            // ignore
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * The name of the bootstrap commands file
     */
    public static final String BOOTSTRAP_FILENAME = "sling_bootstrap.txt";

    /**
     * Prefix for uninstalls in command files
     */
    private static String UNINSTALL_PREFIX = "uninstall ";

    private Map<String, VersionRange> blacklistMap = new HashMap<String, VersionRange>();

    private void getBlackListFromBootstrapFile(BufferedReader r) throws IOException {
            String line = null;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                String bundleSymbolicName = null;
                VersionRange versionRange = null;
                if (line.length() > 0 && line.startsWith(UNINSTALL_PREFIX)) {
                    final String[] s = line.split(" ");
                    extractBlackList(bundleSymbolicName, versionRange, s, 1, 2);
                }
            }

    }

    public boolean isBlacklisted(String symbolicName, Version version) {
        if (blacklistMap.containsKey(symbolicName)) {
            VersionRange range = blacklistMap.get(symbolicName);
            return (range == null) || range.includes(version);
        }
        return false;
    }

    private void extractBlackList(String bundleSymbolicName, VersionRange versionRange, final String[] s, int posSymbolicName, int posVersionRange) {
        if (s.length > posSymbolicName) {
            bundleSymbolicName = s[posSymbolicName].trim();
        }
        if (s.length > posVersionRange) {
            versionRange = new VersionRange(s[posVersionRange].trim());
        }
        blacklistMap.put(bundleSymbolicName, versionRange);
    }
}
