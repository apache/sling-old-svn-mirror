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

import java.io.File;

import org.osgi.framework.BundleContext;

/**
 * Utility class for all file handling.
 */
public class FileUtil {

    /**
     * The name of the bundle context property defining the location for the
     * installer files (value is "sling.installer.dir").
     */
    private static final String CONFIG_DIR = "sling.installer.dir";

    /**
     * The default configuration data directory if no location is configured
     * (value is "installer").
     */
    private static final String DEFAULT_DIR = "installer";

    private final File directory;

    /**
     * Create a file util instance and detect the installer directory.
     */
    public FileUtil( final BundleContext bundleContext ) {
        String location = bundleContext.getProperty(CONFIG_DIR);

        // no configured location, use the config dir in the bundle persistent
        // area
        if ( location == null ) {
            final File locationFile = bundleContext.getDataFile( DEFAULT_DIR );
            if ( locationFile != null ) {
                location = locationFile.getAbsolutePath();
            }
        }

        // fall back to the current working directory if the platform does
        // not support filesystem based data area
        if ( location == null ) {
            location = System.getProperty( "user.dir" ) + File.separatorChar + DEFAULT_DIR;
        }

        // ensure the file is absolute
        File locationFile = new File( location );
        if ( !locationFile.isAbsolute() ) {
            final File bundleLocationFile = bundleContext.getDataFile( locationFile.getPath() );
            if ( bundleLocationFile != null ) {
                locationFile = bundleLocationFile;
            }

            // ensure the file object is an absolute file object
            locationFile = locationFile.getAbsoluteFile();
        }

        // check the location
        if ( !locationFile.isDirectory() ) {
            if ( locationFile.exists() ) {
                throw new IllegalArgumentException( location + " is not a directory" );
            }

            if ( !locationFile.mkdirs() ) {
                throw new IllegalArgumentException( "Cannot create directory " + location );
            }
        }

        this.directory = locationFile;
    }

    /**
     * Return the installer directory.
     */
    public File getDirectory() {
        return this.directory;
    }

    /**
     * Return a file with the given name in the installer directory.
     * @param fileName The file name
     */
    public File getDataFile(final String fileName) {
        return new File(this.directory, fileName);
    }

    /** Serial number to create unique file names in the data storage. */
    private static long serialNumberCounter = System.currentTimeMillis();

    private static long getNextSerialNumber() {
        synchronized (RegisteredResourceImpl.class) {
            return serialNumberCounter++;
        }
    }

    /** Create a new unique data file. */
    public File createNewDataFile(final String hint) {
        final String filename = hint + "-resource-" + getNextSerialNumber() + ".ser";
        return this.getDataFile(filename);
    }
}