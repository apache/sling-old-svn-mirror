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
import org.apache.sling.osgi.installer.OsgiInstaller;

/**
 * A piece of data that can be installed by the {@link OsgiInstaller}
 * Currently the OSGi installer supports bundles and configurations.
 */
public class InstallableResourceImpl implements InstallableResource {

    private final String url;
    private final String digest;
    private final InputStream inputStream;
    private final Dictionary<String, Object> dictionary;
    private final int priority;
    private final Type resourceType;

    /** Create a data object that wraps an InputStream
     *  @param url unique URL of the supplied data, must start with the scheme used
     *     {@link OsgiInstaller#registerResources} call
     *  @param is the resource contents
     *  @param digest must be supplied by client. Does not need to be an actual digest
     *     of the contents, but must change if the contents change. Having this supplied
     *     by the client avoids having to compute real digests to find out if a resource
     *     has changed, which can be expensive.
     */
    public InstallableResourceImpl(String url, InputStream is, String digest,
            final Type type,
            final int priority) {
        this.url = url;
        this.digest = digest;
        this.priority = priority;
        this.resourceType = type;
//        if ( this.resourceType == Type.CONFIG ) {
//            this.dictionary = null;
//            this.inputStream = null;
//        } else {
            this.inputStream = is;
            this.dictionary = null;
//        }
    }

    /** Create a data object that wraps a Dictionary. Digest will be computed
     *  by the installer in this case, as configuration dictionaries are
     *  usually small so computing a real digest to find out if they changed
     *  is ok.
     *
     *  @param url unique URL of the supplied data, must start with the scheme used
     *     {@link OsgiInstaller#registerResources} call
     */
    public InstallableResourceImpl(final String url, final Dictionary<String, Object> d,
            final String digest,
            final Type type,
            final int priority) {
        this.url = url;
        this.inputStream = null;
        this.resourceType = type;
        this.dictionary = d;
        this.digest = digest;
        this.priority = priority;
    }

    /**
     * @see org.apache.sling.osgi.installer.InstallableResource#getUrl()
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * @see org.apache.sling.osgi.installer.InstallableResource#getType()
     */
    public Type getType() {
        return this.resourceType;
    }

    /**
     * @see org.apache.sling.osgi.installer.InstallableResource#getInputStream()
     */
    public InputStream getInputStream() {
        return this.inputStream;
    }

	/**
	 * @see org.apache.sling.osgi.installer.InstallableResource#getDictionary()
	 */
	public Dictionary<String, Object> getDictionary() {
	    return this.dictionary;
	}

    /**
     * @see org.apache.sling.osgi.installer.InstallableResource#getDigest()
     */
    public String getDigest() {
        return this.digest;
    }

    /**
     * @see org.apache.sling.osgi.installer.InstallableResource#getPriority()
     */
    public int getPriority() {
        return this.priority;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", priority=" + priority + ", url=" + url;
    }
}
