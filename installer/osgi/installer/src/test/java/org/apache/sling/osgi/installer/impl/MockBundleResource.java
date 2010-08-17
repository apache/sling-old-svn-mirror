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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.osgi.installer.InstallableResource;
import org.osgi.framework.Constants;

/** Mock RegisteredResource that simulates a bundle */
public class MockBundleResource implements RegisteredResource {

    private static final long serialVersionUID = 1L;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
	private boolean installable = true;
	private final String digest;
	private final int priority;
	private final long serialNumber;
	private static long serialNumberCounter = System.currentTimeMillis();

    public MockBundleResource(String symbolicName, String version) {
        this(symbolicName, version, InstallableResource.DEFAULT_PRIORITY);
    }

    public MockBundleResource(String symbolicName, String version, int priority) {
		attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		attributes.put(Constants.BUNDLE_VERSION, version);
		digest = symbolicName + "." + version;
		this.priority = priority;
		serialNumber = getNextSerialNumber();
	}

    public MockBundleResource(String symbolicName, String version, int priority, String digest) {
        attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        attributes.put(Constants.BUNDLE_VERSION, version);
        this.digest = digest;
        this.priority = priority;
        serialNumber = getNextSerialNumber();
    }

    private static long getNextSerialNumber() {
        synchronized (MockBundleResource.class) {
            return serialNumberCounter++;
        }
    }

	@Override
	public String toString() {
	    return getClass().getSimpleName()
	    + ", n=" + attributes.get(Constants.BUNDLE_SYMBOLICNAME)
        + ", v= " + attributes.get(Constants.BUNDLE_VERSION)
        + ", d=" + digest
        + ", p=" + priority
        ;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#cleanup()
	 */
	public void cleanup() {
	    // nothing to do
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getAttributes()
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getDictionary()
	 */
	public Dictionary<String, Object> getDictionary() {
		return null;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getDigest()
	 */
	public String getDigest() {
		return digest;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getEntityId()
	 */
	public String getEntityId() {
		return null;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return null;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getType()
	 */
	public String getType() {
		return InstallableResource.TYPE_BUNDLE;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getId()
	 */
	public String getId() {
		return null;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getURL()
	 */
	public String getURL() {
		return null;
	}

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getScheme()
     */
    public String getScheme() {
        return null;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#isInstallable()
     */
    public boolean isInstallable() {
        return installable;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#setInstallable(boolean)
     */
    public void setInstallable(boolean installable) {
        this.installable = installable;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getPriority()
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getSerialNumber()
     */
    public long getSerialNumber() {
        return serialNumber;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RegisteredResource o) {
        return RegisteredResourceImpl.compare(this, o);
    }
}
