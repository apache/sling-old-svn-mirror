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
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.InstallableResourceFactory;

/**
 * Default implementation of the installable resource factory.
 */
public class InstallableResourceFactoryImpl implements InstallableResourceFactory {

    /**
     * @see org.apache.sling.osgi.installer.InstallableResourceFactory#create(java.lang.String, java.io.InputStream, java.util.Dictionary, java.lang.String, java.lang.String, java.lang.Integer)
     */
    public InstallableResource create(final String url,
                               final InputStream is,
                               final Dictionary<String, Object> d,
                               final String digest,
                               final String type,
                               final Integer priority)
    throws IOException {
        if ( url == null ) {
            throw new IllegalArgumentException("url must not be null.");
        }
        if ( is == null ) {
            // if input stream is null, config through dictionary is expected!
            if ( d == null ) {
                throw new IllegalArgumentException("dictionary must not be null.");
            }
            try {
                return new InstallableResourceImpl(url, null, d,
                        (digest != null ? digest : url + ":" + DigestUtil.computeDigest(d)),
                        (type != null ? type : InstallableResource.TYPE_CONFIG),
                        (priority != null ? priority : DEFAULT_PRIORITY));
            } catch ( final NoSuchAlgorithmException nsae) {
                throw (IOException)new IOException("Digest not found.").initCause(nsae);
            }
        }
        // TODO - compute digest for bundle if digest is null - for now we throw
        if ( digest == null ) {
            throw new IllegalArgumentException("digest must not be null for a bundle.");
        }

        final String resourceType = (type != null ? type : computeResourceType(getExtension(url)));
        if ( resourceType.equals(InstallableResource.TYPE_CONFIG ) ) {
            throw new IOException("Resource type config not supported for input streams: " + url);
        }

        return new InstallableResourceImpl(url, is, d, digest,
                resourceType,
                (priority != null ? priority : DEFAULT_PRIORITY));
    }

    /** Compute the extension */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }

    private static String computeResourceType(String extension) {
        if (extension.equals("jar")) {
            return InstallableResource.TYPE_BUNDLE;
        }
        return InstallableResource.TYPE_CONFIG;
    }
}
