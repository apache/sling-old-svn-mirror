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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.installer.api.InstallableResource;

/**
 * Internal resource is a private data object which wraps
 * an installable resource and is used to create a registered
 * resource.
 *
 * An internal resource has always:
 * - a resource type
 * - a digest
 *
 */
public class InternalResource extends InstallableResource {

    /**
     * Create an internal resource.
     * @throws IOException if something is wrong
     */
    public static InternalResource create(
            final String scheme,
            final InstallableResource resource)
    throws IOException {
        // installable resource has an id, a priority and either
        // an input stream or a dictionary
        InputStream is = resource.getInputStream();
        Dictionary<String, Object> dict = resource.getDictionary();
        String type = resource.getType();
        // Handle deprecated types and map them to new types
        if ( InstallableResource.TYPE_BUNDLE.equals(type) ) {
            type = InstallableResource.TYPE_FILE;
        } else if ( InstallableResource.TYPE_CONFIG.equals(type) ) {
            type = InstallableResource.TYPE_PROPERTIES;
        }

        if ( is != null &&
             (InstallableResource.TYPE_PROPERTIES.equals(type) ||
              type == null && isConfigExtension(resource.getId()))) {
            dict = readDictionary(is, getExtension(resource.getId()));
            if ( dict == null ) {
                throw new IOException("Unable to read dictionary from input stream: " + resource.getId());
            }
            is = null;
        }

        File dataFile = null;
        final String digest;
        if ( is == null ) {
            // if input stream is null, properties is expected!
            type = (type != null ? type : InstallableResource.TYPE_PROPERTIES);
            digest = (resource.getDigest() != null && resource.getDigest().length() > 0
                      ? resource.getDigest() : resource.getId() + ":" + FileDataStore.computeDigest(dict));
        } else {
            final String url = scheme + ':' + resource.getId();
            // if input stream is not null, file is expected!
            dataFile = FileDataStore.SHARED.createNewDataFile(is,
                    url,
                    resource.getDigest(),
                    resource.getType());
            type = (type != null ? type : InstallableResource.TYPE_FILE);
            if (resource.getDigest() != null && resource.getDigest().length() > 0) {
                digest = resource.getDigest();
            } else {
                digest = FileDataStore.computeDigest(dataFile);
                FileDataStore.SHARED.updateDigestCache(url, digest);
            }
        }
        return new InternalResource(scheme,
                resource.getId(),
                is,
                dict,
                type,
                digest,
                resource.getPriority(),
                dataFile);
    }

    /** The unique resource url. */
    private final String url;

    /** The data file (if copied) */
    private File dataFile;

    public InternalResource(
            final String scheme,
            final String id,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final String type,
            final String digest,
            final Integer priority,
            final File dataFile) {
        super(id, is, dict, digest, type, priority);
        this.url = scheme + ':' + id;
        this.dataFile = dataFile;
    }

    /** The unique url of the resource. */
    public String getURL() {
        return this.url;
    }

    /**
     * Copy given Dictionary
     */
    public Dictionary<String, Object> getPrivateCopyOfDictionary() {
        final Dictionary<String, Object> d = this.getDictionary();
        if ( d == null ) {
            return null;
        }

        final Dictionary<String, Object> result = new Hashtable<String, Object>();
        final Enumeration<String> e = d.keys();
        while(e.hasMoreElements()) {
            final String key = e.nextElement();
            result.put(key, d.get(key));
        }
        return result;
    }

    /**
     * Copy the given file and return it.
     */
    public File getPrivateCopyOfFile() throws IOException {
        return this.dataFile;
    }

    /**
     * Read dictionary from an input stream.
     * We use the same logic as Apache Felix FileInstall here:
     * - *.cfg files are treated as property files
     * - *.config files are handled by the Apache Felix ConfigAdmin file reader
     * @param is
     * @param extension
     * @return
     * @throws IOException
     */
    private static Dictionary<String, Object> readDictionary(
            final InputStream is, final String extension) {
        final Hashtable<String, Object> ht = new Hashtable<String, Object>();
        final InputStream in = new BufferedInputStream(is);
        try {
            if ( !extension.equals("config") ) {
                final Properties p = new Properties();
                in.mark(1);
                boolean isXml = in.read() == '<';
                in.reset();
                if (isXml) {
                    p.loadFromXML(in);
                } else {
                    p.load(in);
                }
                final Enumeration<Object> i = p.keys();
                while ( i.hasMoreElements() ) {
                    final Object key = i.nextElement();
                    ht.put(key.toString(), p.get(key));
                }
            } else {
                @SuppressWarnings("unchecked")
                final Dictionary<String, Object> config = ConfigurationHandler.read(in);
                final Enumeration<String> i = config.keys();
                while ( i.hasMoreElements() ) {
                    final String key = i.nextElement();
                    ht.put(key, config.get(key));
                }
            }
        } catch ( IOException ignore ) {
            return null;
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        return ht;
    }

    private static boolean isConfigExtension(final String url) {
        final String ext = getExtension(url);
        return "config".equals(ext) || "properties".equals(ext) || "cfg".equals(ext);
    }

    /**
     * Compute the extension
     */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }
}