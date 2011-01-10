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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.osgi.framework.Constants;

/** Mock RegisteredResource that simulates a bundle */
public class MockBundleResource implements RegisteredResource {

    private static final long serialVersionUID = 1L;
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private final Map<String, Object> tempAttributes = new HashMap<String, Object>();
	private State state = State.INSTALL;
	private final String digest;
	private final int priority;

    public MockBundleResource(String symbolicName, String version) {
        this(symbolicName, version, InstallableResource.DEFAULT_PRIORITY);
    }

    public MockBundleResource(String symbolicName, String version, int priority) {
		attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		attributes.put(Constants.BUNDLE_VERSION, version);
		digest = symbolicName + "." + version;
		this.priority = priority;
	}

    public MockBundleResource(String symbolicName, String version, int priority, String digest) {
        attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        attributes.put(Constants.BUNDLE_VERSION, version);
        this.digest = digest;
        this.priority = priority;
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
	 * Clean up
	 */
	public void cleanup() {
	    // nothing to do
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getAttributes()
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getDictionary()
	 */
	public Dictionary<String, Object> getDictionary() {
		return null;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getDigest()
	 */
	public String getDigest() {
		return digest;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getEntityId()
	 */
	public String getEntityId() {
		return "bundle:" + this.attributes.get(Constants.BUNDLE_SYMBOLICNAME);
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		return null;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getType()
	 */
	public String getType() {
		return InstallableResource.TYPE_BUNDLE;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getURL()
	 */
	public String getURL() {
		return this.getScheme() + ":" + this.attributes.get(Constants.BUNDLE_SYMBOLICNAME) + "-" + this.attributes.get(Constants.BUNDLE_VERSION);
	}

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getScheme()
     */
    public String getScheme() {
        return "test";
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getPriority()
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RegisteredResource o) {
        return RegisteredResourceImpl.compare(this, o);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getState()
     */
    public State getState() {
        return state;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#setState(org.apache.sling.installer.api.tasks.RegisteredResource.State)
     */
    public void setState(State s) {
        this.state = s;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getTemporaryAttribute(java.lang.String)
     */
    public Object getTemporaryAttribute(String key) {
        return this.tempAttributes.get(key);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#setTemporaryAttributee(java.lang.String, java.lang.Object)
     */
    public void setTemporaryAttributee(String key, Object value) {
        if ( value == null ) {
            this.tempAttributes.remove(key);
        } else {
            this.tempAttributes.put(key, value);
        }
    }
}
