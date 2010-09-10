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

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.slf4j.LoggerFactory;

/**
 * The <code>Installer</code> is the service calling the
 * OSGi installer
 *
 * TODO - We should collect all changes from a scan and send
 * them to the installer in a batch
 */
public class Installer implements FileChangesListener {

    private static final String SCHEME_PREFIX = "fileinstall";

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
     * @see org.apache.sling.installer.file.impl.FileChangesListener#added(java.io.File)
     */
    public void added(final File file) {
        LoggerFactory.getLogger(this.getClass()).info("Added file {}", file);
        final InstallableResource resource = this.createResource(file);
        this.installer.updateResources(this.scheme, new InstallableResource[] {resource}, null);
    }

    /**
     * @see org.apache.sling.installer.file.impl.FileChangesListener#changed(java.io.File)
     */
    public void changed(final File file) {
        LoggerFactory.getLogger(this.getClass()).info("Changed file {}", file);
        final InstallableResource resource = this.createResource(file);
        this.installer.updateResources(this.scheme, new InstallableResource[] {resource}, null);
    }

    /**
     * @see org.apache.sling.installer.file.impl.FileChangesListener#initialSet(java.util.List)
     */
    public void initialSet(final List<File> files) {
        LoggerFactory.getLogger(this.getClass()).info("Initial set for {}", this.scheme);
        final List<InstallableResource> resources = new ArrayList<InstallableResource>();
        for(final File f : files) {
            LoggerFactory.getLogger(this.getClass()).info("File {}", f);
            final InstallableResource resource = this.createResource(f);
            if ( resource != null ) {
                resources.add(resource);
            }
        }
        this.installer.registerResources(this.scheme, resources.toArray(new InstallableResource[resources.size()]));
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
            // ignore this for now (TODO)
        }
        return null;
    }
    /**
     * @see org.apache.sling.installer.file.impl.FileChangesListener#removed(java.io.File)
     */
    public void removed(final File file) {
        LoggerFactory.getLogger(this.getClass()).info("Removed file {}", file);
        this.installer.updateResources(this.scheme, null, new String[] {file.getAbsolutePath()});
    }
}
