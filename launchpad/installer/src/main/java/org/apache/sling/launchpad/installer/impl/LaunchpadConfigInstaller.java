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
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Integer PRIORITY = new Integer(50);

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
                    logger.info("Launchpad {} will be installed: {}", resourceType, path);
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
        installer.registerResources("launchpad", (toInstall));
        logger.info("{} resources registered with OsgiInstaller", toInstall.length);
    }
}