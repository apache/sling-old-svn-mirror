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
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Mock RegisteredResource that simulates a bundle
 *
 */
public class MockBundleResource implements TaskResource, Comparable<MockBundleResource> {

    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private final Map<String, Object> tempAttributes = new HashMap<String, Object>();
	private ResourceState state = ResourceState.INSTALL;
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
    public int compareTo(MockBundleResource o) {
        return RegisteredResourceImpl.compare(this, o);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getState()
     */
    public ResourceState getState() {
        return state;
    }

    /**
     * Set the state
     */
    public void setState(ResourceState s) {
        this.state = s;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getAttribute(java.lang.String)
     */
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String key, Object value) {
        if ( value == null ) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getTemporaryAttribute(java.lang.String)
     */
    public Object getTemporaryAttribute(String key) {
        return this.tempAttributes.get(key);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#setTemporaryAttribute(java.lang.String, java.lang.Object)
     */
    public void setTemporaryAttribute(String key, Object value) {
        if ( value == null ) {
            this.tempAttributes.remove(key);
        } else {
            this.tempAttributes.put(key, value);
        }
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getVersion()
     */
    public Version getVersion() {
        final String vInfo = (String)this.getAttribute(Constants.BUNDLE_VERSION);
        return (vInfo == null ? null : new Version(vInfo));
    }

    public RegisteredResourceImpl getRegisteredResourceImpl() throws IOException {
        final InstallableResource is = new InstallableResource((String)this.attributes.get(Constants.BUNDLE_SYMBOLICNAME),
                null,
                new Hashtable<String, Object>(),
                this.getDigest(),
                this.getType(),
                this.getPriority());
        final InternalResource ir = InternalResource.create(this.getScheme(), is);
        RegisteredResourceImpl rr = RegisteredResourceImpl.create(ir);
        for(final Map.Entry<String, Object> e : this.attributes.entrySet()) {
            rr.setAttribute(e.getKey(), e.getValue());
        }
        final TransformationResult tr = new TransformationResult();
        tr.setId((String)this.attributes.get(Constants.BUNDLE_SYMBOLICNAME));
        tr.setResourceType(this.getType());
        tr.setVersion(this.getVersion());
        rr = (RegisteredResourceImpl)rr.clone(tr);
        rr.setState(this.getState());

        return rr;
    }
}
