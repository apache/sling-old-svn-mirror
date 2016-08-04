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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;

/**
 * Utility class for all file handling.
 */
public class FileDataStore {

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

    /** Public instance - to avoid passing a reference to this service to each data object. */
    public static FileDataStore SHARED;

    /** Cache for url to digest mapping. */
    private final Map<String, CacheEntry> digestCache = new HashMap<String, CacheEntry>();

    private static final class CacheEntry {
        public final File file;
        public final String digest;

        public CacheEntry(final File file, final String digest) {
            this.file = file;
            this.digest = digest;
        }
    }

    /**
     * Create a file util instance and detect the installer directory.
     */
    public FileDataStore( final BundleContext bundleContext ) {
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
        SHARED = this;
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

    /**
     * Create a new unique data file.
     */
    public File createNewDataFile(final InputStream stream,
            final String url,
            final String digest,
            final String hint)
    throws IOException {
        // check if we already have this data
        if ( digest != null ) {
            synchronized ( this.digestCache ) {
                final CacheEntry storedDigest = this.digestCache.get(url);
                if ( storedDigest != null && storedDigest.digest.equals(digest) ) {
                    return storedDigest.file;
                }
            }
        }
        final int pos = url.lastIndexOf('/');
        final String name = url.substring(pos + 1);
        final String filename = (hint == null ? "rsrc" : hint) + '-' + name + '-' + getNextSerialNumber() + ".ser";

        //replace special characters from the filename that are not allowed by the OS
        final String filename2 = filename.replaceAll("[\\*\"/\\\\\\[\\]\\:\\;\\|\\=\\,]+", "_"); // Windows

        final File file = this.getDataFile(filename2);

        this.copyToLocalStorage(stream, file);

        if ( digest != null ) {
            synchronized ( this.digestCache ) {
                this.digestCache.put(url, new CacheEntry(file, digest));
            }
        }

        return file;
    }

    public void updateDigestCache(final String url, final File file, final String digest) {
        synchronized ( this.digestCache ) {
            this.digestCache.put(url, new CacheEntry(file, digest));
        }
    }

    /**
     * Copy data to local storage.
     */
    protected void copyToLocalStorage(final InputStream data,
            final File dataFile) throws IOException {
        final OutputStream os = new BufferedOutputStream(new FileOutputStream(dataFile));
        try {
            final byte[] buffer = new byte[16384];
            int count = 0;
            while( (count = data.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, count);
            }
            os.flush();
        } finally {
            os.close();
        }
    }

    public File createNewDataFile(final String hint, final InputStream stream)
    throws IOException {
        final String filename = (hint == null ? "unknown" : hint) + "-resource-" + getNextSerialNumber() + ".ser";
        final File file = this.getDataFile(filename);

        this.copyToLocalStorage(stream, file);

        return file;
    }

    public void removeFromDigestCache(final String url, final String digest) {
        synchronized ( this.digestCache ) {
            final CacheEntry entry = this.digestCache.get(url);
            if ( entry != null && entry.digest.equals(digest) ) {
                this.digestCache.remove(url);
            }
        }
    }

    /** Digest is needed to detect changes in data */
    public static String computeDigest(final File data) throws IOException {
        try {
            final InputStream is = new FileInputStream(data);
            try {
                final MessageDigest d = MessageDigest.getInstance("MD5");

                final byte[] buffer = new byte[8192];
                int count = 0;
                while( (count = is.read(buffer, 0, buffer.length)) > 0) {
                    d.update(buffer, 0, count);
                }
                return digestToString(d);
            } finally {
                is.close();
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ignore) {
            return data.toString();
        }
    }

    /** convert digest to readable string (http://www.javalobby.org/java/forums/t84420.html) */
    private static String digestToString(MessageDigest d) {
        final BigInteger bigInt = new BigInteger(1, d.digest());
        return new String(bigInt.toString(16));
    }

    /** Digest is needed to detect changes in data, and must not depend on dictionary ordering */
    public static String computeDigest(Dictionary<String, Object> data) {
        try {
            final MessageDigest d = MessageDigest.getInstance("MD5");
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);

            final SortedSet<String> sortedKeys = new TreeSet<String>();
            if (data != null) {
                for(Enumeration<String> e = data.keys(); e.hasMoreElements(); ) {
                    final String key = e.nextElement();
                    sortedKeys.add(key);
                }
            }
            for(final String key : sortedKeys) {
                oos.writeObject(key);
                final Object val = data.get(key);
                if ( val instanceof Number ) {
                    oos.writeObject(String.valueOf(val));
                } else {
                    oos.writeObject(val);
                }
            }

            oos.flush();
            d.update(bos.toByteArray());
            return digestToString(d);
        } catch (Exception ignore) {
            return data.toString();
        }
    }
}