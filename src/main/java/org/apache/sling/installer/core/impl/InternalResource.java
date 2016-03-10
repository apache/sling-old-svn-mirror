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
import java.net.URI;
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

        // Handle deprecated types and map them to new types
        String type = resource.getType();
        if ( InstallableResource.TYPE_BUNDLE.equals(type) ) {
            type = InstallableResource.TYPE_FILE;
        } else if ( InstallableResource.TYPE_CONFIG.equals(type) ) {
            type = InstallableResource.TYPE_PROPERTIES;
        }

        // check for optional uri (only if type is file and digest is available)
        final String resourceUri = (dict != null
                                    && (type == null || InstallableResource.TYPE_FILE.equals(type))
                                    && resource.getDigest() != null
                                    && resource.getDigest().length() > 0) ?
                              (String)dict.get(InstallableResource.RESOURCE_URI_HINT) : null;
        // check if resourceUri is accessible
        boolean useResourceUri = resourceUri != null;
        if ( resourceUri != null ) {
            InputStream resourceUriIS = null;
            try {
                final URI uri = new URI(resourceUri);
                resourceUriIS = uri.toURL().openStream();
                // everything fine
            } catch (final Exception use) {
                useResourceUri = false;
            } finally {
                if ( resourceUriIS != null ) {
                    try {
                        resourceUriIS.close();
                    } catch (final IOException ignore) {
                        // ignore
                    }
                }
            }
        }

        if ( is != null &&
             (InstallableResource.TYPE_PROPERTIES.equals(type) ||
              ((type == null || InstallableResource.TYPE_FILE.equals(type)) && isConfigExtension(resource.getId())))) {
            try {
                dict = readDictionary(is, getExtension(resource.getId()));
            } catch (final IOException ioe) {
                throw (IOException)new IOException("Unable to read dictionary from input stream: " + resource.getId()).initCause(ioe);
            }
            is = null;
            useResourceUri = false;
        }

        File dataFile = null;
        final String digest;
        if ( is == null ) {
            // if input stream is null, properties is expected!
            type = (type != null ? type : InstallableResource.TYPE_PROPERTIES);
            // we always compute a digest
            digest = FileDataStore.computeDigest(dict);
        } else {
            type = (type != null ? type : InstallableResource.TYPE_FILE);
            if ( resourceUri != null && useResourceUri ) {
                digest = resource.getDigest();
            } else {
                final String url = scheme + ':' + resource.getId();
                // if input stream is not null, file is expected!
                dataFile = FileDataStore.SHARED.createNewDataFile(is,
                        url,
                        resource.getDigest(),
                        resource.getType());
                if (resource.getDigest() != null && resource.getDigest().length() > 0) {
                    digest = resource.getDigest();
                } else {
                    digest = FileDataStore.computeDigest(dataFile);
                    FileDataStore.SHARED.updateDigestCache(url, dataFile, digest);
                }
            }
        }
        return new InternalResource(scheme,
                resource.getId(),
                is,
                dict,
                type,
                digest,
                resource.getPriority(),
                dataFile,
                useResourceUri ? resourceUri : null);
    }

    /** The unique resource url. */
    private final String url;

    /** The data file (if copied) */
    private File dataFile;

    /** The resource uri */
    private final String resourceUri;

    public InternalResource(
            final String scheme,
            final String id,
            final InputStream is,
            final Dictionary<String, Object> dict,
            final String type,
            final String digest,
            final Integer priority,
            final File dataFile,
            final String resourceUri) {
        super(id, is, dict, digest, type, priority);
        this.url = scheme + ':' + id;
        this.dataFile = dataFile;
        this.resourceUri = resourceUri;
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
    public File getPrivateCopyOfFile() {
        return this.dataFile;
    }

    /**
     * Set the data file.
     */
    public void setPrivateCopyOfFile(final File file) {
        this.dataFile = file;
    }

    /**
     * Return the resource uri (or null)
     */
    public String getResourceUri() {
        return this.resourceUri;
    }

    /**
     * Read dictionary from an input stream.
     * We use the same logic as Apache Felix FileInstall here:
     * - *.cfg files are treated as property files
     * - *.config files are handled by the Apache Felix ConfigAdmin file reader
     * @param is
     * @param extension
     * @throws IOException
     */
    private static Dictionary<String, Object> readDictionary(
            final InputStream is, final String extension)
    throws IOException {
        final Hashtable<String, Object> ht = new Hashtable<String, Object>();
        final BufferedInputStream in = new BufferedInputStream(is);
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
                // check for initial comment line
                in.mark(256);
                final int firstChar = in.read();
                if ( firstChar == '#' ) {
                    int b;
                    while ((b = in.read()) != '\n' ) {
                        if ( b == -1 ) {
                            throw new IOException("Unable to read configuration.");
                        }
                    }
                } else {
                    in.reset();
                }
                @SuppressWarnings("unchecked")
                final Dictionary<String, Object> config = ConfigurationHandler.read(in);
                final Enumeration<String> i = config.keys();
                while ( i.hasMoreElements() ) {
                    final String key = i.nextElement();
                    ht.put(key, config.get(key));
                }
            }
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