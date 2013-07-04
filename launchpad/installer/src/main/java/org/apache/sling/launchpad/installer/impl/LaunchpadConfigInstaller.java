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
import java.util.Set;

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

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Resources supplied under this path by
     * LaunchpadContentProvider are considered for installation
     */
    private static final String ROOT_PATH = "resources";

    /**
     * Resources supplied under this path by
     * LaunchpadContentProvider are considered for installation
     * as configurations
     */
    private static final String CONFIG_NAME = "config";

    /**
     * Resources supplied under this path by
     * LaunchpadContentProvider are considered for installation
     * as files
     */
    private static final String INSTALL_NAME = "install";
    private static final String INSTALL_PREFIX = "install.";

    /** Artifact priority. */
    private static final Integer PRIORITY = new Integer(50);
    private static final int PRIORITY_BOOST = 5;

    /**
     * Check the path for installable artifacts.
     */
    private boolean checkPath(final String rootPath,
            final String resourceType,
            Integer prio) {
        int count = 0;

        final Iterator<String> configPaths = resourceProvider.getChildren(rootPath);
        if ( configPaths != null ) {
            final int hintPos = rootPath.lastIndexOf('/');
            final String hint = rootPath.substring(hintPos + 1);

            while (configPaths.hasNext()) {
                String path = configPaths.next();
                if ( path.endsWith("/") ) {
                    path = path.substring(0, path.length() - 1);
                }
                if ( !checkPath(path, resourceType, prio) ) {
                    count++;

                    final URL url = resourceProvider.getResource(path);
                    Dictionary<String, Object> dict = null;
                    if ( InstallableResource.TYPE_FILE.equals(resourceType) ) {
                        dict = new Hashtable<String, Object>();
                        if ( !hint.startsWith(INSTALL_NAME) ) {
                            dict.put(InstallableResource.INSTALLATION_HINT, hint);
                        }
                        try {
                            dict.put(InstallableResource.RESOURCE_URI_HINT, url.toURI().toString());
                        } catch (final URISyntaxException e) {
                            // we just ignore this
                        }
                    } else if ( !hint.equals(CONFIG_NAME) ) {
                        final int activeModes = isActive(hint);
                        if ( activeModes == 0 ) {
                            logger.debug("Launchpad ignoring {} : {} due to unactivated run mode: {}", new Object[] {resourceType, path, hint});
                            continue;
                        }
                        prio = PRIORITY + PRIORITY_BOOST * activeModes;
                    }
                    long lastModified = -1;
                    try {
                        lastModified = url.openConnection().getLastModified();
                    } catch (final IOException e) {
                        // we ignore this
                    }
                    logger.debug("Launchpad {} will be registered: {}", resourceType, path);
                    final String digest = (lastModified > 0 ? String.valueOf(lastModified) : null);
                    final InputStream stream = resourceProvider.getResourceAsStream(path);
                    installables.add(new InstallableResource(path, stream, dict, digest, resourceType, prio));
                }
            }
        }
        return count > 0;
    }

    private int isActive(final String runModesString) {
        final String[] runModes = runModesString.split("\\.");
        boolean active = true;
        for(final String mode : runModes) {
            if ( !activeRunModes.contains(mode) ) {
                active = false;
                break;
            }
        }

        return active ? runModes.length : 0;
    }

    /**
     * Install artifacts
     */
    public static void install(final OsgiInstaller installer,
            final LaunchpadContentProvider resourceProvider,
            final Set<String> activeRunModes) {
        new LaunchpadConfigInstaller(resourceProvider, activeRunModes).install(installer);
    }

    private final LaunchpadContentProvider resourceProvider;

    private final Set<String> activeRunModes;

    private final Collection<InstallableResource> installables = new HashSet<InstallableResource>();

    private LaunchpadConfigInstaller(final LaunchpadContentProvider resourceProvider,
            final Set<String> activeRunModes) {
        this.resourceProvider = resourceProvider;
        this.activeRunModes = activeRunModes;
    }

    private void install(final OsgiInstaller installer) {
        logger.info("Activating launchpad config installer, configuration path={}/{}, install path={}/{}",
                new Object[] {ROOT_PATH, CONFIG_NAME, ROOT_PATH, INSTALL_NAME});

        final Iterator<String> configPaths = resourceProvider.getChildren(ROOT_PATH);
        if ( configPaths != null ) {
            while ( configPaths.hasNext() ) {
                String path = configPaths.next();
                logger.debug("Found launchpad resource {}", path);
                if ( path.endsWith("/") ) {
                    path = path.substring(0, path.length() - 1);
                }
                final int namePos = path.lastIndexOf('/');
                String name = path.substring(namePos + 1);
                if ( name.equals(CONFIG_NAME) ) {
                    // configurations
                    checkPath(path, InstallableResource.TYPE_PROPERTIES, PRIORITY);
                } else if ( name.equals(INSTALL_NAME) ) {
                    // files
                    checkPath(path, InstallableResource.TYPE_FILE, PRIORITY);
                } else if ( name.startsWith(INSTALL_PREFIX) ) {
                    final int activeModes = isActive(name.substring(INSTALL_PREFIX.length()));
                    if ( activeModes > 0 ) {
                        // files
                        final int prio = PRIORITY + PRIORITY_BOOST * activeModes;
                        checkPath(path, InstallableResource.TYPE_FILE, prio);
                    } else {
                        logger.debug("Ignoring path {} due to unactivated run mode: {}", path, name.substring(INSTALL_PREFIX.length()));
                    }
                } else {
                    logger.debug("Ignoring path {} - not an installation path", path);
                }
            }
        } else {
            logger.warn("Run mode dependent installation not supported by launchpad content provider {}", resourceProvider);
            // revert to old behavior
            checkPath(ROOT_PATH + '/' + CONFIG_NAME, InstallableResource.TYPE_PROPERTIES, PRIORITY);
            checkPath(ROOT_PATH + '/' + INSTALL_NAME, InstallableResource.TYPE_FILE, PRIORITY);

        }

        final InstallableResource [] toInstall = installables.toArray(new InstallableResource []{});
        installer.registerResources("launchpad", (toInstall));
        logger.info("{} resources registered with OsgiInstaller", toInstall.length);
    }
}