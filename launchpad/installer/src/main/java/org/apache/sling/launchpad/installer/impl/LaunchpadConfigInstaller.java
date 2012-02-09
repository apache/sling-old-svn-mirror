/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.installer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class scans the launchpad resources folder and provides the artifacts
 * to the OSGi installer.
 */
public class LaunchpadConfigInstaller {

    /**
     * Resources supplied under this path by
     * LaunchpadContentProvider are considered for installation
     * as configurations
     */
    private static final String ROOT_CONFIG_PATH = "resources/config";

    /**
     * Resources supplied under this path by
     * LaunchpadContentProvider are considered for installation
     * as files
     */
    private static final String ROOT_INSTALL_PATH = "resources/install";

    /** Artifact priority. */
    private static final Integer PRIORITY = new Integer(50);

    /**
     * Check the path for installable artifacts.
     */
    private static boolean checkPath(final LaunchpadContentProvider resourceProvider,
            final Collection<InstallableResource> installables,
            final String rootPath,
            final String resourceType) {
        int count = 0;
        final Logger logger = LoggerFactory.getLogger(LaunchpadConfigInstaller.class);
        final Iterator<String> configPaths = resourceProvider.getChildren(rootPath);
        if ( configPaths != null ) {
            final int hintPos = rootPath.lastIndexOf('/');
            final String hint = rootPath.substring(hintPos + 1);
            while (configPaths.hasNext()) {
                String path = configPaths.next();
                if ( path.endsWith("/") ) {
                    path = path.substring(0, path.length() - 1);
                }
                if ( !checkPath(resourceProvider, installables, path, resourceType) ) {
                    final URL url = resourceProvider.getResource(path);
                    Dictionary<String, Object> dict = null;
                    if ( InstallableResource.TYPE_FILE.equals(resourceType) ) {
                        dict = new Hashtable<String, Object>();
                        dict.put(InstallableResource.INSTALLATION_HINT, hint);
                        try {
                            dict.put(InstallableResource.RESOURCE_URI_HINT, url.toURI().toString());
                        } catch (final URISyntaxException e) {
                            // we just ignore this
                        }
                    }
                    long lastModified = -1;
                    try {
                        lastModified = url.openConnection().getLastModified();
                    } catch (final IOException e) {
                        // we ignore this
                    }
                    final String digest = (lastModified > 0 ? String.valueOf(lastModified) : null);
                    final InputStream stream = resourceProvider.getResourceAsStream(path);
                    installables.add(new InstallableResource(path, stream, dict, digest, resourceType, PRIORITY));
                    count++;
                }
            }
        }
        return count > 0;
    }

    /**
     * Install artifacts
     */
    public static void install(final OsgiInstaller installer,
            final LaunchpadContentProvider resourceProvider) {
        final Logger logger = LoggerFactory.getLogger(LaunchpadConfigInstaller.class);
        logger.info("Activating launchpad config installer, configuration path={}, install path={}",
                ROOT_CONFIG_PATH, ROOT_INSTALL_PATH);

        final Collection<InstallableResource> installables = new HashSet<InstallableResource>();

        // configurations
        checkPath(resourceProvider, installables, ROOT_CONFIG_PATH, InstallableResource.TYPE_PROPERTIES);

        // files
        checkPath(resourceProvider, installables, ROOT_INSTALL_PATH, InstallableResource.TYPE_FILE);

        final InstallableResource [] toInstall = installables.toArray(new InstallableResource []{});
        // sort by url to have lower start levels first
        Arrays.sort(toInstall, new ResourceComparator());
        for(final InstallableResource rsrc : toInstall ) {
            logger.info("Launchpad {} will be installed: {}", rsrc.getType(), rsrc.getId());
        }
        installer.registerResources("launchpad", (toInstall));
        logger.info("{} resources registered with OsgiInstaller", toInstall.length);
    }

    /**
     * Resource comparator
     */
    private static final class ResourceComparator implements Comparator<InstallableResource> {

        private Integer getStartLevel(final InstallableResource ir) {
            try {
                final Integer level = Integer.valueOf((String)ir.getDictionary().get(InstallableResource.INSTALLATION_HINT));
                if ( level == 0 ) {
                    return 100;
                }
                return level;
            } catch ( final NumberFormatException ignore) {
                return 1000;
            }
        }

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(final InstallableResource o1,
                           final InstallableResource o2) {
            int result = o2.getType().compareTo(o1.getType());
            if ( result == 0 ) {
                if ( o1.getType() == InstallableResource.TYPE_PROPERTIES ) {
                    result = o1.getId().compareTo(o2.getId());
                } else {
                    result = getStartLevel(o1).compareTo(getStartLevel(o2));
                }
            }
            return result;
        }
    }
}