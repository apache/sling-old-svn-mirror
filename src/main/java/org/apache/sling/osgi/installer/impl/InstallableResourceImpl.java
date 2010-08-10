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
    private final String resourceType;

    /**
     * Create a data object.
     */
    public InstallableResourceImpl(final String url,
            final InputStream is,
            final Dictionary<String, Object> dict,
            String digest,
            final String type,
            final int priority) {
        this.url = url;
        this.digest = digest;
        this.priority = priority;
        this.resourceType = type;
        this.inputStream = is;
        this.dictionary = dict;
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
    public String getType() {
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
