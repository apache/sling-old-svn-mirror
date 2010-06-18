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
package org.apache.sling.osgi.installer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.InstallableResourceFactory;
import org.apache.sling.osgi.installer.InstallableResource.Type;
import org.apache.sling.osgi.installer.impl.propertyconverter.PropertyConverter;
import org.apache.sling.osgi.installer.impl.propertyconverter.PropertyValue;

/**
 * Default implementation of the installable resource factory.
 */
public class InstallableResourceFactoryImpl implements InstallableResourceFactory {

    /**
     * @see org.apache.sling.osgi.installer.InstallableResourceFactory#create(java.lang.String, java.io.InputStream, java.lang.String, org.apache.sling.osgi.installer.InstallableResource.Type, java.lang.Integer)
     */
    public InstallableResource create(final String url,
                               final InputStream is,
                               final String digest,
                               final InstallableResource.Type type,
                               final Integer priority) {
        if ( url == null ) {
            throw new IllegalArgumentException("url must not be null.");
        }
        if ( is == null ) {
            throw new IllegalArgumentException("input stream must not be null.");
        }
        final InstallableResource.Type resourceType = (type != null ? type : computeResourceType(getExtension(url)));
        if ( resourceType == InstallableResource.Type.CONFIG ) {
            try {
                return this.create(url, readDictionary(is), digest, resourceType, priority);
            } catch (IOException ignore) {
                // TODO - log this
                return null;
            }
        }

        // TODO - compute digest for bundle if digest is null - for now we throw
        if ( digest == null ) {
            throw new IllegalArgumentException("digest must not be null for a bundle.");
        }

        return new InstallableResourceImpl(url, is, digest,
                resourceType,
                (priority != null ? priority : DEFAULT_PRIORITY));
    }

    /**
     * @see org.apache.sling.osgi.installer.InstallableResourceFactory#create(java.lang.String, java.util.Dictionary, java.lang.String, org.apache.sling.osgi.installer.InstallableResource.Type, java.lang.Integer)
     */
    public InstallableResource create(final String url,
                               final Dictionary<String, Object> d,
                               final String digest,
                               final InstallableResource.Type type,
                               final Integer priority) {
        if ( url == null ) {
            throw new IllegalArgumentException("url must not be null.");
        }
        if ( d == null ) {
            throw new IllegalArgumentException("dictionary must not be null.");
        }
        try {
            return new InstallableResourceImpl(url, d,
                    (digest != null ? digest : url + ":" + DigestUtil.computeDigest(d)),
                    (type != null ? type : Type.CONFIG),
                    (priority != null ? priority : DEFAULT_PRIORITY));
        } catch (Exception ignore) {
            // TODO - log this
            return null;
        }
    }

    /** Convert InputStream to Dictionary using our extended properties format,
     *  which supports multi-value properties
     */
    private static Dictionary<String, Object> readDictionary(InputStream is) throws IOException {
        try {
            final Dictionary<String, Object> result = new Hashtable<String, Object>();
            final PropertyConverter converter = new PropertyConverter();
            final Properties p = new Properties();
            p.load(is);
            for(Map.Entry<Object, Object> e : p.entrySet()) {
                final PropertyValue v = converter.convert((String)e.getKey(), (String)e.getValue());
                result.put(v.getKey(), v.getValue());
            }
            return result;
        } finally {
            try {
                is.close();
            } catch (IOException ignore ) {
                // we ignore this
            }
        }
    }

    /** Compute the extension */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }

    private static Type computeResourceType(String extension) {
        if(extension.equals("jar")) {
            return Type.BUNDLE;
        }
        return Type.CONFIG;
    }
}
