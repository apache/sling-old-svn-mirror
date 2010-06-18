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

import java.io.InputStream;
import java.util.Dictionary;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.InstallableResourceFactory;

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
        if ( priority == null ) {
            return new InstallableResourceImpl(url, is, digest);
        }
        return new InstallableResourceImpl(url, is, digest, priority);
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
        if ( priority == null ) {
            return new InstallableResourceImpl(url, d);
        }
        return new InstallableResourceImpl(url, d, priority);
    }
}
