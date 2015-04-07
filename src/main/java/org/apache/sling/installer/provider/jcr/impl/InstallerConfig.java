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
package org.apache.sling.installer.provider.jcr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.provider.jcr.impl.JcrInstaller.NodeConverter;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

public class InstallerConfig {

    /** Write back enabled? */
    private final boolean writeBack;

    /** Our NodeConverters*/
    private final Collection <NodeConverter> converters = new ArrayList<NodeConverter>();

    private final int maxWatchedFolderDepth;

    /** Filter for folder names */
    private final FolderNameFilter folderNameFilter;

    /** The root folders that we watch */
    private final String[] roots;

    /** The path for new configurations. */
    private final String newConfigPath;

    /** The path for pauseInstallation property */
    private final String pauseScanNodePath;

    /** List of watched folders */
    private final List<WatchedFolder> watchedFolders = new LinkedList<WatchedFolder>();

    private final Logger logger;

    public InstallerConfig(
            final Logger logger,
            final ComponentContext ctx,
            final Dictionary<?, ?> cfg,
            final SlingSettingsService settings) {
        this.logger = logger;
        this.writeBack = PropertiesUtil.toBoolean(getPropertyValue(logger, ctx, cfg, JcrInstaller.PROP_ENABLE_WRITEBACK), JcrInstaller.DEFAULT_ENABLE_WRITEBACK);

        // Setup converters
        converters.add(new FileNodeConverter());
        converters.add(new ConfigNodeConverter());

        // Configurable max depth, system property (via bundle context) overrides default value
        final Object obj = getPropertyValue(logger, ctx, cfg, JcrInstaller.PROP_INSTALL_FOLDER_MAX_DEPTH);
        if (obj != null) {
            // depending on where it's coming from, obj might be a string or integer
            maxWatchedFolderDepth = Integer.valueOf(String.valueOf(obj)).intValue();
            logger.debug("Using configured ({}) folder name max depth '{}'", JcrInstaller.PROP_INSTALL_FOLDER_MAX_DEPTH, maxWatchedFolderDepth);
        } else {
            maxWatchedFolderDepth = JcrInstaller.DEFAULT_FOLDER_MAX_DEPTH;
            logger.debug("Using default folder max depth {}, not provided by {}", maxWatchedFolderDepth, JcrInstaller.PROP_INSTALL_FOLDER_MAX_DEPTH);
        }

        // Configurable folder regexp, system property overrides default value
        String folderNameRegexp = (String)getPropertyValue(logger, ctx, cfg, JcrInstaller.FOLDER_NAME_REGEXP_PROPERTY);
        if (folderNameRegexp != null) {
            folderNameRegexp = folderNameRegexp.trim();
            logger.debug("Using configured ({}) folder name regexp '{}'", JcrInstaller.FOLDER_NAME_REGEXP_PROPERTY, folderNameRegexp);
        } else {
            folderNameRegexp = JcrInstaller.DEFAULT_FOLDER_NAME_REGEXP;
            logger.debug("Using default folder name regexp '{}', not provided by {}", folderNameRegexp, JcrInstaller.FOLDER_NAME_REGEXP_PROPERTY);
        }

        // Setup folder filtering and watching
        this.folderNameFilter = new FolderNameFilter(PropertiesUtil.toStringArray(getPropertyValue(logger, ctx, cfg, JcrInstaller.PROP_SEARCH_PATH), JcrInstaller.DEFAULT_SEARCH_PATH),
                folderNameRegexp, settings.getRunModes());
        this.roots = folderNameFilter.getRootPaths();

        // setup default path for new configurations
        String newCfgPath = PropertiesUtil.toString(getPropertyValue(logger, ctx, cfg, JcrInstaller.PROP_NEW_CONFIG_PATH), JcrInstaller.DEFAULT_NEW_CONFIG_PATH);
        final boolean postSlash = newCfgPath.endsWith("/");
        if ( !postSlash ) {
            newCfgPath = newCfgPath.concat("/");
        }
        final boolean preSlash = newCfgPath.startsWith("/");
        if ( !preSlash ) {
            newCfgPath = this.folderNameFilter.getRootPaths()[0] + '/' + newCfgPath;
        }
        this.newConfigPath = newCfgPath;

        this.pauseScanNodePath = PropertiesUtil.toString(getPropertyValue(logger, ctx, cfg, JcrInstaller.PROP_SCAN_PROP_PATH), JcrInstaller.PAUSE_SCAN_NODE_PATH);
    }

    /** Get a property value from the old config, component context or bundle context */
    private Object getPropertyValue(final Logger logger, final ComponentContext ctx, final Dictionary<?, ?> oldConfig, final String name) {
        Object result = null;
        if ( oldConfig != null ) {
            result = oldConfig.get(name);
            if ( result != null ) {
                logger.warn("Using configuration value from obsolete configuration with PID {} for property {}." +
                            " Please merge this configuration into the configuration with the PID {}.",
                            new Object[] {JcrInstaller.OLD_PID, name, ctx.getProperties().get(Constants.SERVICE_PID)});
            }
        }
        if ( result == null ) {
            result = ctx.getBundleContext().getProperty(name);
            if (result == null) {
                result = ctx.getProperties().get(name);
            }
        }
        return result;
    }

    public String[] getRoots() {
        return this.roots;
    }

    public FolderNameFilter getFolderNameFilter() {
        return this.folderNameFilter;
    }

    public Collection <NodeConverter> getConverters() {
        return this.converters;
    }

    public int getMaxWatchedFolderDepth() {
        return maxWatchedFolderDepth;
    }

    public String getPauseScanNodePath() {
        return pauseScanNodePath;
    }

    public boolean isWriteBack() {
        return this.writeBack;
    }

    public String getNewConfigPath() {
        return this.newConfigPath;
    }

    public List<WatchedFolder> cloneWatchedFolders() {
        synchronized ( this.watchedFolders ) {
            return new ArrayList<WatchedFolder>(this.watchedFolders);
        }
    }

    /**
     * Scan watchedFolders and get installable resources
     */
    public List<InstallableResource> scanWatchedFolders() throws RepositoryException {
        final List<InstallableResource> resources = new LinkedList<InstallableResource>();
        synchronized ( this.watchedFolders ) {
            for(final WatchedFolder f : this.watchedFolders) {
                final WatchedFolder.ScanResult r = f.scan();
                logger.debug("Startup: {} provides resources {}", f, r.toAdd);
                resources.addAll(r.toAdd);
            }
        }
        return resources;
    }

    /**
     * Check all WatchedFolder, in case some were deleted
     */

    public List<String> checkForRemovedWatchedFolders(final Session session) throws RepositoryException {
        final List<String> removedResources = new LinkedList<String>();
        synchronized ( this.watchedFolders ) {
            final Iterator<WatchedFolder> i = this.watchedFolders.iterator();
            while ( i.hasNext() ) {
                final WatchedFolder wf = i.next();

                logger.debug("Item {} exists? {}", wf.getPath(), session.itemExists(wf.getPath()));
                if (!session.itemExists(wf.getPath())) {
                    logger.info("Deleting {}, path does not exist anymore", wf);
                    removedResources.addAll(wf.scan().toRemove);
                    i.remove();
                }
            }
        }
        return removedResources;
    }

    /**
     * Add WatchedFolder to our list if it doesn't exist yet.
     */
    public void addWatchedFolder(final WatchedFolder toAdd) {
        synchronized ( this.watchedFolders ) {
            WatchedFolder existing = null;
            for(WatchedFolder wf : this.watchedFolders) {
                if (wf.getPath().equals(toAdd.getPath())) {
                    existing = wf;
                    break;
                }
            }
            if (existing == null) {
                this.watchedFolders.add(toAdd);
                toAdd.start();
            }
        }
    }

    public boolean anyWatchFolderNeedsScan() {
        synchronized ( this.watchedFolders ) {
            for (final WatchedFolder wf : this.watchedFolders) {
                if (wf.needsScan()) {
                    return true;
                }
            }
        }
        return false;
    }
}
