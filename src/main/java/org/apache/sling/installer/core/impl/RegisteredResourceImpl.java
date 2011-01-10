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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Implementation of the registered resource
 */
public class RegisteredResourceImpl
    implements RegisteredResource, Serializable {

    /** Use own serial version ID as we control serialization. */
    private static final long serialVersionUID = 6L;

    /** Serialization version. */
    private static final int VERSION = 1;

    /** The resource url. */
    private final String url;

    /** The installer scheme. */
	private final String urlScheme;

	/** The digest for the resource. */
	private final String digest;

	/** The entity id. */
	private String entity;

	/** The dictionary for configurations. */
	private final Dictionary<String, Object> dictionary;

	/** Additional attributes. */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	private File dataFile;

	private final int priority;

    private String resourceType;

    /** The current state of this resource. */
    private State state = State.INSTALL;

    /** Temporary attributes. */
    private transient Map<String, Object> temporaryAttributes;

    /**
     * Serialize the object
     * - write version id
     * - serialize each entry in the resources list
     * @param out Object output stream
     * @throws IOException
     */
    private void writeObject(final java.io.ObjectOutputStream out)
    throws IOException {
        out.writeInt(VERSION);
        out.writeObject(url);
        out.writeObject(urlScheme);
        out.writeObject(digest);
        out.writeObject(entity);
        out.writeObject(dictionary);
        out.writeObject(attributes);
        out.writeObject(dataFile);
        out.writeObject(resourceType);
        out.writeInt(priority);
        out.writeObject(state);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize each entry in the resources list
     */
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version != VERSION ) {
            throw new ClassNotFoundException(this.getClass().getName());
        }
        Util.setField(this, "url", in.readObject());
        Util.setField(this, "urlScheme", in.readObject());
        Util.setField(this, "digest", in.readObject());
        Util.setField(this, "entity", in.readObject());
        Util.setField(this, "dictionary", in.readObject());
        Util.setField(this, "attributes", in.readObject());
        Util.setField(this, "dataFile", in.readObject());
        Util.setField(this, "resourceType", in.readObject());
        Util.setField(this, "priority", in.readInt());
        this.state = (State) in.readObject();
    }

    /**
     * Try to create a registered resource.
     */
    public static RegisteredResourceImpl create(
            final InternalResource input)
    throws IOException {
        final int schemePos = input.getURL().indexOf(':');
        return new RegisteredResourceImpl(input.getId(),
                input.getPrivateCopyOfFile(),
                input.getPrivateCopyOfDictionary(),
                input.getType(),
                input.getDigest(),
                input.getPriority(),
                input.getURL().substring(0, schemePos));
    }

	/**
	 * Create a RegisteredResource from given data.
	 * As this data object is filled from an {@link #create(BundleContext, InstallableResource, String)}
	 * we don't have to validate values - this has already been done
	 * The only exception is the digest!
	 */
	private RegisteredResourceImpl(final String id,
	        final File file,
	        final Dictionary<String, Object> dict,
	        final String type,
	        final String digest,
	        final int priority,
	        final String scheme) {
        this.url = scheme + ':' + id;
        this.dataFile = file;
        this.dictionary = dict;
        this.resourceType = type;
        this.digest = digest;
        this.priority = priority;
        this.urlScheme = scheme;
	}

	@Override
	public String toString() {
	    return "RegisteredResource(url=" + this.getURL() +
	        ", entity=" + this.getEntityId() +
	        ", state=" + this.state +
	        ", digest=" + this.getDigest() + ")";
	}

	/**
	 * Clean up used data files.
	 */
	public void cleanup() {
	    if ( this.dataFile != null && this.dataFile.exists() ) {
			dataFile.delete();
		}
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getURL()
	 */
	public String getURL() {
		return this.url;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
	    if (this.dataFile != null && this.dataFile.exists() ) {
	        return new BufferedInputStream(new FileInputStream(this.dataFile));
	    }
        return null;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getDictionary()
	 */
	public Dictionary<String, Object> getDictionary() {
		return dictionary;
	}

	/**
	 * @see org.apache.sling.installer.api.tasks.RegisteredResource#getDigest()
	 */
	public String getDigest() {
		return digest;
	}

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getType()
     */
    public String getType() {
        return resourceType;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getEntityId()
     */
    public String getEntityId() {
        return entity;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getAttributes()
     */
    public Map<String, Object> getAttributes() {
		return attributes;
	}

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getScheme()
     */
    public String getScheme() {
        return urlScheme;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getPriority()
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getState()
     */
    public State getState() {
        return this.state;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#setState(org.apache.sling.installer.api.tasks.RegisteredResource.State)
     */
    public void setState(State s) {
        this.state = s;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }
        if ( ! (obj instanceof RegisteredResource) ) {
            return false;
        }
        if ( this.entity == null ) {
            return this.getURL().equals(((RegisteredResource)obj).getURL());
        }
        return compareTo((RegisteredResource)obj) == 0;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.getURL().hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final RegisteredResource b) {
        return compare(this, b);
    }

    /**
     * Compare resources.
     * First we compare the entity id - the entity id contains the resource type
     * together with an entity identifier for the to be installed resource like
     * the symbolic name of a bundle, the pid for a configuration etc.
     */
    public static int compare(final RegisteredResource a, final RegisteredResource b) {
        // check entity id first
        int result = a.getEntityId().compareTo(b.getEntityId());
        if ( result == 0 ) {
            if (a.getType().equals(InstallableResource.TYPE_BUNDLE)) {
                // we need a special comparison for bundles
                result = compareBundles(a, b);
            } else {
                // all other types: check prio and then digest
                result = Integer.valueOf(b.getPriority()).compareTo(a.getPriority());

                // check digest
                if ( result == 0 ) {
                    result = a.getDigest().compareTo(b.getDigest());
                }
            }
        }
        if ( result == 0 ) {
            result = a.getURL().compareTo(b.getURL());
        }
        return result;
    }

    /**
     * Bundles are compared differently than other resource types:
     * - higher versions have always priority - regardless of the priority attribute!
     * - priority matters only if version is same
     * - if the version is a snapshot version, the serial number and the digest are used
     *   in addition
     */
    private static int compareBundles(final RegisteredResource a, final RegisteredResource b) {
        boolean isSnapshot = false;
        int result = 0;

        // Order by version
        final Version va = new Version((String)a.getAttributes().get(Constants.BUNDLE_VERSION));
        final Version vb = new Version((String)b.getAttributes().get(Constants.BUNDLE_VERSION));
        isSnapshot = va.toString().contains("SNAPSHOT");
        // higher version has more priority, must come first so invert comparison
        result = vb.compareTo(va);

        // Then by priority, higher values first
        if (result == 0) {
            result = Integer.valueOf(b.getPriority()).compareTo(a.getPriority());
        }

        if (result == 0 && isSnapshot) {
            result = a.getDigest().compareTo(b.getDigest());
        }

        return result;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#getTemporaryAttribute(java.lang.String)
     */
    public Object getTemporaryAttribute(final String key) {
        if ( this.temporaryAttributes != null ) {
            return this.temporaryAttributes.get(key);
        }
        return null;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.RegisteredResource#setTemporaryAttributee(java.lang.String, java.lang.Object)
     */
    public void setTemporaryAttributee(final String key, final Object value) {
        if ( this.temporaryAttributes == null ) {
            this.temporaryAttributes = new HashMap<String, Object>();
        }
        if ( value == null ) {
            this.temporaryAttributes.remove(key);
        } else {
            this.temporaryAttributes.put(key, value);
        }
    }

    /**
     * Update this resource from the result.
     * Currently only the input stream and resource type is updated.
     * @param tr Transformation result
     */
    public void update(final TransformationResult tr)
    throws IOException {
        final InputStream is = tr.getInputStream();
        if ( tr.getResourceType() != null ) {
            this.resourceType = tr.getResourceType();
            if ( tr.getId() != null ) {
                this.entity = this.resourceType + ':' + tr.getId();
            } else {
                if ( !InstallableResource.TYPE_FILE.equals(this.getType())
                      && !InstallableResource.TYPE_PROPERTIES.equals(this.getType()) ) {

                    String lastIdPart = this.getURL();
                    final int slashPos = lastIdPart.lastIndexOf('/');
                    if ( slashPos != -1 ) {
                        lastIdPart = lastIdPart.substring(slashPos + 1);
                    }
                    this.entity = this.resourceType + ':' + lastIdPart;
                }
            }
        }
        if ( is != null ) {
            try {
                final File newDataFile = FileUtil.SHARED.createNewDataFile(this.getType());
                FileUtil.SHARED.copyToLocalStorage(is, newDataFile);
                this.cleanup();
                this.dataFile = newDataFile;
            } finally {
                try {
                    is.close();
                } catch (final IOException ignore) {}
            }
        }
    }
}