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
package org.apache.sling.installer.file.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Installer</code> is the service calling the
 * OSGi installer
 *
 */
public class Installer implements FileChangesListener {

    /** The scheme we use to register our resources. */
    private static final String SCHEME_PREFIX = "fileinstall";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The OSGi installer service. */
    private final OsgiInstaller installer;

    /** The scheme to use. */
    private final String scheme;

    public Installer(final OsgiInstaller installer,
            final String id) {
        this.scheme = SCHEME_PREFIX + id;
        this.installer = installer;
    }

    /**
     * @see org.apache.sling.installer.file.impl.FileChangesListener#initialSet(java.util.List)
     */
    public void initialSet(final List<File> files) {
        logger.debug("Initial set for {}", this.scheme);
        final List<InstallableResource> resources = new ArrayList<InstallableResource>();
        for(final File f : files) {
            logger.debug("Initial file {}", f);
            final InstallableResource resource = this.createResource(f);
            if ( resource != null ) {
                resources.add(resource);
            }
        }
        this.installer.registerResources(this.scheme, resources.toArray(new InstallableResource[resources.size()]));
    }

    /**
     * @see org.apache.sling.installer.file.impl.FileChangesListener#updated(java.util.List, java.util.List, java.util.List)
     */
    public void updated(List<File> added, List<File> changed, List<File> removed) {
        final List<InstallableResource> updated;
        if ( (added != null && added.size() > 0) || (changed != null && changed.size() > 0) ) {
            updated = new ArrayList<InstallableResource>();
            if ( added != null ) {
                for(final File f : added) {
                    logger.debug("Added file {}", f);
                    final InstallableResource resource = this.createResource(f);
                    if ( resource != null ) {
                        updated.add(resource);
                    }
                }
            }
            if ( changed != null ) {
                for(final File f : changed) {
                    logger.debug("Changed file {}", f);
                    final InstallableResource resource = this.createResource(f);
                    if ( resource != null ) {
                        updated.add(resource);
                    }
                }
            }
        } else {
            updated = null;
        }
        final String[] removedUrls;
        if ( removed != null && removed.size() > 0 ) {
            removedUrls = new String[removed.size()];
            int index = 0;
            for(final File f : removed) {
                removedUrls[index] = f.getAbsolutePath();
                logger.debug("Removed file {}", removedUrls[index]);
                index++;
            }
        } else {
            removedUrls = null;
        }
        if ( updated != null || removedUrls != null ) {
            this.installer.updateResources(this.scheme,
                    updated == null ? null : updated.toArray(new InstallableResource[updated.size()]), removedUrls);
        }
    }

    private InstallableResource createResource(final File file) {
        try {
            final InputStream is = new FileInputStream(file);
            final String digest = String.valueOf(file.lastModified());
            // if this is a bundle check for start level directory!
            Dictionary<String, Object> dict = null;
            if ( file.getName().endsWith(".jar") || file.getName().endsWith(".war") ) {
                final String parentName = file.getParentFile().getName();
                try {
                    final int startLevel = Integer.valueOf(parentName);
                    if ( startLevel > 0 ) {
                        dict = new Hashtable<String, Object>();
                        dict.put(InstallableResource.BUNDLE_START_LEVEL, startLevel);
                    }
                } catch (NumberFormatException nfe) {
                    // ignore this
                }
            }
            return new InstallableResource(file.getAbsolutePath(), is, dict, digest,
                null, null);
        } catch (IOException io) {
            logger.error("Unable to read file " + file, io);
        }
        return null;
    }
}
