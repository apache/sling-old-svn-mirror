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
package org.apache.sling.osgi.installer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.cm.file.ConfigurationHandler;


/**
 * A piece of data that can be installed by the {@link OsgiInstaller}
 * Currently the OSGi installer supports bundles and configurations.
 *
 */
public class InstallableResource {

    /**
     * The type for a bundle - in this case {@link #getInputStream} must
     * return an input stream to the bundle. {@link #getDictionary()} might
     * return additional information.
     */
    public static final String TYPE_BUNDLE = "bundle";

    /**
     * The type for a configuration - in this case {@link #getDictionary()}
     * must return a dictionary with the configuration.
     */
    public static final String TYPE_CONFIG = "config";

    /**
     * Optional parameter in the dictionary if a bundle is installed. If this
     * is set with a valid start level, the bundle is installed in that start level.
     */
    public static final String BUNDLE_START_LEVEL = "bundle.startlevel";

    /** Default resource priority */
    public static final int DEFAULT_PRIORITY = 100;

    private final String id;
    private final String digest;
    private final InputStream inputStream;
    private final Dictionary<String, Object> dictionary;
    private final int priority;
    private final String resourceType;

    /**
     * Create a data object - this is a simple constructor just using the
     * values as they are provided.
     * @throws IllegalArgumentException if something is wrong
     */
    public InstallableResource(final String id,
            InputStream is,
            Dictionary<String, Object> dict,
            String digest,
            String type,
            final Integer priority) {
        if ( id == null ) {
            throw new IllegalArgumentException("id must not be null.");
        }
        if ( is == null ) {
            // if input stream is null, config through dictionary is expected!
            if ( dict == null ) {
                throw new IllegalArgumentException("dictionary must not be null (or input stream must not be null).");
            }
            type = (type != null ? type : InstallableResource.TYPE_CONFIG);
        }
        final String resourceType = (type != null ? type : computeResourceType(getExtension(id)));
        if ( resourceType == null ) {
            throw new IllegalArgumentException("Resource type must not be null");
        }
        if ( is != null && resourceType.equals(InstallableResource.TYPE_CONFIG ) ) {
            dict = readDictionary(is, getExtension(id));
            if ( dict == null ) {
                throw new IllegalArgumentException("Unable to read dictionary from input stream: " + id);
            }
            is = null;
        }
        if ( resourceType.equals(InstallableResource.TYPE_CONFIG) ) {
            digest = (digest != null ? digest : id + ":" + computeDigest(dict));
        }

        // TODO - compute digest if digest is null - for now we throw
        if ( digest == null || digest.length() == 0 ) {
            throw new IllegalArgumentException("digest must not be null");
        }

        this.id = id;
        this.inputStream = is;
        this.dictionary = dict;
        this.digest = digest;
        this.priority = (priority != null ? priority : DEFAULT_PRIORITY);
        this.resourceType = resourceType;
    }

    /**
     * Return this data's id. It is opaque for the {@link OsgiInstaller}
     * but should uniquely identify the resource within the namespace of
     * the used installation mechanism.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Return the type of this resource.
     * @return The resource type.
     */
    public String getType() {
        return this.resourceType;
    }

    /**
     * Return an input stream with the data of this resource.
     * Null if resource contains a configuration instead. Caller is responsible for
     * closing the stream.
     * If this resource is of type CONFIG it must not return an input stream and
     * if this resource is of type BUNDLE it must return an input stream!
     * @return The input stream or null.
     */
    public InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * Return this resource's dictionary.
     * Null if resource contains an InputStream instead. If this resource is of
     * type CONFIG it must return a dictionary and if this resource is of type BUNDLE
     * it might return a dictionary!
     * @return The resource's dictionary or null.
     */
    public Dictionary<String, Object> getDictionary() {
        return this.dictionary;
    }

    /**
     * Return this resource's digest. Not necessarily an actual md5 or other digest of the
     * data, can be any string that changes if the data changes.
     */
    public String getDigest() {
        return this.digest;
    }

    /**
     * Return the priority of this resource. Priorities are used to decide which
     * resource to install when several are registered for the same OSGi entity
     * (bundle, config, etc.)
     */
    public int getPriority() {
        return this.priority;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", priority=" + priority + ", id=" + id;
    }

    /**
     * Compute the extension
     */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }

    /**
     * Compute the resource type
     */
    private static String computeResourceType(String extension) {
        if (extension.equals("jar")) {
            return InstallableResource.TYPE_BUNDLE;
        }
        if ( extension.equals("cfg")
             || extension.equals("config")
             || extension.equals("xml")
             || extension.equals("properties")) {
            return InstallableResource.TYPE_CONFIG;
        }
        return extension;
    }

    /** convert digest to readable string (http://www.javalobby.org/java/forums/t84420.html) */
    private static String digestToString(MessageDigest d) {
        final BigInteger bigInt = new BigInteger(1, d.digest());
        return new String(bigInt.toString(16));
    }

    /** Digest is needed to detect changes in data, and must not depend on dictionary ordering */
    private static String computeDigest(Dictionary<String, Object> data) {
        try {
            final MessageDigest d = MessageDigest.getInstance("MD5");
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);

            final SortedSet<String> sortedKeys = new TreeSet<String>();
            if(data != null) {
                for(Enumeration<String> e = data.keys(); e.hasMoreElements(); ) {
                    final String key = e.nextElement();
                    sortedKeys.add(key);
                }
            }
            for(String key : sortedKeys) {
                oos.writeObject(key);
                oos.writeObject(data.get(key));
            }

            bos.flush();
            d.update(bos.toByteArray());
            return digestToString(d);
        } catch (Exception ignore) {
            return data.toString();
        }
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
}
