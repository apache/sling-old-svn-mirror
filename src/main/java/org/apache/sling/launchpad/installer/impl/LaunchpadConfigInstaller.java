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

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
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

    public static void install(final OsgiInstaller installer,
            final LaunchpadContentProvider resourceProvider) {
        final Logger logger = LoggerFactory.getLogger(LaunchpadConfigInstaller.class);
        logger.info("Activating launchpad config installer, configuration path={}, install path={}",
                ROOT_CONFIG_PATH, ROOT_INSTALL_PATH);

        final Collection<InstallableResource> installables = new HashSet<InstallableResource>();

        // configurations
        final Iterator<String> configPaths = resourceProvider.getChildren(ROOT_CONFIG_PATH);
        if ( configPaths != null ) {
            while (configPaths.hasNext()) {
                final String path = configPaths.next();
                logger.info("Config launchpad file will be installed: {}", path);
                final InputStream stream = resourceProvider.getResourceAsStream(path);
                installables.add(new InstallableResource(path, stream, null, null, InstallableResource.TYPE_PROPERTIES, null));
            }
        }

        // files
        final Iterator<String> filePaths = resourceProvider.getChildren(ROOT_INSTALL_PATH);
        if ( filePaths != null ) {
            while (filePaths.hasNext()) {
                final String path = filePaths.next();
                logger.info("Launchpad file will be installed: {}", path);
                final InputStream stream = resourceProvider.getResourceAsStream(path);
                installables.add(new InstallableResource(path, stream, null, null, InstallableResource.TYPE_FILE, null));
            }
        }

        final InstallableResource [] toInstall = installables.toArray(new InstallableResource []{});
        installer.registerResources("launchpad", (toInstall));
        logger.info("{} resources registered with OsgiInstaller", toInstall.length);
    }
}