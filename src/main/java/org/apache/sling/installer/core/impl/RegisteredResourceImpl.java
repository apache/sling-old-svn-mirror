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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Implementation of the registered resource
 */
public class RegisteredResourceImpl
    implements TaskResource, Serializable, Comparable<RegisteredResourceImpl> {

    /** Use own serial version ID as we control serialization. */
    private static final long serialVersionUID = 6L;

    /** Serialization version. */
    private static final int VERSION = 3;

    /** The resource url. */
    private String url;

    /** The installer scheme. */
	private String urlScheme;

	/** The digest for the resource. */
	private String digest;

	/** The entity id. */
	private String entity;

	/** The dictionary for configurations. */
	private final Dictionary<String, Object> dictionary;

	/** Additional attributes. */
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	private String dataUri;

	private File dataFile;

	private int priority;

    private String resourceType;

    /** The current state of this resource. */
    private ResourceState state = ResourceState.INSTALL;

    /** Temporary attributes. */
    private transient Map<String, Object> temporaryAttributes;

    private boolean cleanedUp = false;

    /** When was the last status change? */
    private long lastChange = -1;

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
        out.writeObject(state.toString());
        out.writeLong(this.lastChange);
        out.writeObject(this.dataUri);
    }

    /**
     * Deserialize the object
     * - read version id
     * - deserialize each entry in the resources list
     */
    private void readObject(final java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        final int version = in.readInt();
        if ( version < 1 || version > VERSION ) {
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
        this.state = ResourceState.valueOf((String) in.readObject());
        if ( version > 1 ) {
            this.lastChange = in.readLong();
        } else {
            this.lastChange = 0;
        }
        if ( version > 2 ) {
            this.dataUri = (String)in.readObject();
        } else if ( InstallableResource.TYPE_CONFIG.equals(this.resourceType) && this.dictionary != null ) {
            // update digest calculated by older versions
            final String updatedDigest = FileDataStore.computeDigest(this.dictionary);
            if ( !updatedDigest.equals(this.digest) ) {
                this.digest = updatedDigest;
            }
        }
        // update file location
        if ( this.dataFile != null ) {
            this.dataFile = FileDataStore.SHARED.getDataFile(this.dataFile.getName());
        }
    }

    /**
     * Try to create a registered resource.
     */
    public static RegisteredResourceImpl create(
            final InternalResource input)
    throws IOException {
        final int schemePos = input.getURL().indexOf(':');
        return new RegisteredResourceImpl(input.getId(),
                input.getResourceUri(),
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
	        final String resourceUri,
	        final File file,
	        final Dictionary<String, Object> dict,
	        final String type,
	        final String digest,
	        final int priority,
	        final String scheme) {
        this.url = scheme + ':' + id;
        this.dataUri = resourceUri;
        this.dataFile = file;
        this.dictionary = dict;
        this.resourceType = type;
        this.digest = digest;
        this.priority = priority;
        this.urlScheme = scheme;
	}

	@Override
	public String toString() {
	    final StringBuilder sb = new StringBuilder();
	    if ( this.getEntityId() == null ) {
	        sb.append("RegisteredResource");
	    } else {
	        sb.append("TaskResource");
	    }
	    sb.append("(url=");
	    sb.append(this.getURL());

	    if ( this.getEntityId() != null ) {
	        sb.append(", entity=");
	        sb.append(this.getEntityId());
	        sb.append(", state=");
	        sb.append(this.state);
            if ( this.attributes.size() > 0 ) {
                sb.append(", attributes=[");
                boolean first = true;
                for(final Map.Entry<String, Object> entry : this.attributes.entrySet()) {
                    if ( !first ) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                }
                sb.append("]");
            }
	    }
	    sb.append(", digest=");
	    sb.append(this.getDigest());
	    sb.append(')');
	    return sb.toString();
	}

	public boolean hasDataFile() {
	    return this.dataFile != null;
	}

	/**
	 * Remove the data file
	 */
	private void removeDataFile() {
        if ( this.dataFile != null && this.dataFile.exists() ) {
            dataFile.delete();
        }
        this.dataUri = null;
	}

	/**
	 * Clean up used data files.
	 */
	public void cleanup() {
	    if ( !cleanedUp ) {
	        cleanedUp = true;
	        this.removeDataFile();
	        FileDataStore.SHARED.removeFromDigestCache(this.url, this.digest);
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
	    if ( this.dataUri != null ) {
	        try {
    	        final URI uri = new URI(this.dataUri);
    	        return uri.toURL().openStream();
	        } catch (final URISyntaxException use) {
	            throw (IOException)new IOException().initCause(use);
	        }
	    }
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
     * @see org.apache.sling.installer.api.tasks.TaskResource#getAttribute(java.lang.String)
     */
    public Object getAttribute(final String key) {
        return this.attributes.get(key);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(final String key, final Object value) {
        if ( value == null ) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
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
     * @see org.apache.sling.installer.api.tasks.TaskResource#getState()
     */
    public ResourceState getState() {
        return this.state;
    }

    /**
     * Set the state for the resource.
     */
    public void setState(ResourceState s) {
        this.lastChange = System.currentTimeMillis();
        this.state = s;
    }

    /**
     * When did the last change happen?
     * @return -1 if no change , 0 if unknown, > 0 otherwise
     */
    public long getLastChange() {
        return this.lastChange;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }
        if ( ! (obj instanceof RegisteredResourceImpl) ) {
            return false;
        }
        if ( this.entity == null ) {
            return this.getURL().equals(((RegisteredResourceImpl)obj).getURL());
        }
        return compareTo((RegisteredResourceImpl)obj) == 0;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getURL().hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final RegisteredResourceImpl b) {
        return compare(this, b);
    }

    /**
     * Compare two resources where we know that they point to the same entity and have the
     * same URL!
     */
    public static boolean isSameResource(final RegisteredResourceImpl a, final RegisteredResourceImpl b) {
        // check if the artifacts have a version
        final Version va = a.getVersion();
        final Version vb = b.getVersion();
        if ( va != null && vb != null ) {
            // Compare version
            if ( !vb.equals(va) ) {
                return false;
            }
            final boolean isSnapshot = va.toString().contains("SNAPSHOT");
            if ( !isSnapshot ) {
                return true;
            }
        }
        // we just compare the digest
        return a.getDigest().equals(b.getDigest());
    }

    /**
     * Compare resources.
     * First we compare the entity id - the entity id contains the resource type
     * together with an entity identifier for the to be installed resource like
     * the symbolic name of a bundle, the pid for a configuration etc.
     */
    public static int compare(final TaskResource a, final TaskResource b) {
        // check entity id first
        int result = a.getEntityId().compareTo(b.getEntityId());
        boolean hasVersion = false;
        if ( result == 0 ) {
            // compare versions
            boolean isSnapshot = false;

            // Order by version
            final Version va = a.getVersion();
            final Version vb = b.getVersion();

            if ( va != null && vb != null ) {
                hasVersion = true;
                isSnapshot = va.toString().contains("SNAPSHOT");
                // higher version has more priority, must come first so invert comparison
                result = vb.compareTo(va);
            }

            // Then by priority, higher values first
            if (result == 0) {
                result = Integer.valueOf(b.getPriority()).compareTo(a.getPriority());
            }

            if (result == 0 && isSnapshot) {
                // higher digest has more priority, must come first so invert comparison
                result = b.getDigest().compareTo(a.getDigest());
            }
        }

        if ( result == 0 && a.getState() != b.getState() ) {
            if ( a.getState() == ResourceState.INSTALLED ) {
                return -1;
            } else if ( b.getState() == ResourceState.INSTALLED ) {
                return 1;
            } else if ( a.getState() == ResourceState.INSTALL ) {
                return -1;
            } else if ( b.getState() == ResourceState.INSTALL ) {
                return 1;
            }
        }
        if ( result == 0 ) {
            // finally use url and then digest
            result = a.getURL().compareTo(b.getURL());
            if ( result == 0 && !hasVersion ) {
                // higher digest has more priority, must come first so invert comparison
                result = b.getDigest().compareTo(a.getDigest());
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getTemporaryAttribute(java.lang.String)
     */
    public Object getTemporaryAttribute(final String key) {
        if ( this.temporaryAttributes != null ) {
            return this.temporaryAttributes.get(key);
        }
        return null;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#setTemporaryAttribute(java.lang.String, java.lang.Object)
     */
    public void setTemporaryAttribute(final String key, final Object value) {
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
    private void update(final TransformationResult tr)
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
                final File newDataFile = FileDataStore.SHARED.createNewDataFile(this.getType(), is);
                this.removeDataFile();
                this.dataFile = newDataFile;
            } finally {
                try {
                    is.close();
                } catch (final IOException ignore) {}
            }
        }
        if ( tr.getAttributes() != null ) {
            this.attributes.putAll(tr.getAttributes());
        }
        if ( tr.getVersion() != null ) {
            this.attributes.put(Constants.BUNDLE_VERSION, tr.getVersion().toString());
        }
    }

    /**
     * Update the resource uri - if provided.
     */
    public void update(final InternalResource rsrc) {
        if ( rsrc.getResourceUri() != null ) {
            FileDataStore.SHARED.removeFromDigestCache(this.url, this.digest);
            this.removeDataFile();
            this.dataUri = rsrc.getResourceUri();
            if ( this.dictionary != null ) {
                this.dictionary.put(InstallableResource.RESOURCE_URI_HINT, rsrc.getResourceUri());
            }
        } else if ( rsrc.getPrivateCopyOfFile() != null ) {
            final boolean update = this.dataFile == null || !this.dataFile.getName().equals(rsrc.getPrivateCopyOfFile().getName());
            if ( update ) {
                if ( this.dictionary != null ) {
                    this.dictionary.remove(InstallableResource.RESOURCE_URI_HINT);
                }
                if ( this.dataFile != null ) {
                    this.removeDataFile();
                }
                this.dataFile = rsrc.getPrivateCopyOfFile();
                FileDataStore.SHARED.updateDigestCache(this.url, this.digest);
            }
        }
    }

    /**
     * Create a new resource with updated information
     */
    public TaskResource clone(TransformationResult transformationResult)
    throws IOException {
        final int schemePos = this.url.indexOf(':');
        final RegisteredResourceImpl rr = new RegisteredResourceImpl(
                this.url.substring(schemePos + 1),
                this.dataUri,
                this.dataFile,
                this.dictionary,
                this.resourceType,
                this.digest,
                this.priority,
                this.urlScheme);
        rr.attributes.putAll(this.attributes);
        rr.update(transformationResult);

        return rr;
    }

    public void update(final File file,
            final Dictionary<String, Object> dict,
            final String digest,
            final int priority,
            final String url) {
        this.removeDataFile();
        if ( file != null ) {
            this.dataFile = file;
        } else {
            while ( !this.dictionary.isEmpty() ) {
                this.dictionary.remove(this.dictionary.keys().nextElement());
            }
            final Enumeration<String> keys = dict.keys();
            while ( keys.hasMoreElements() ) {
                final String key = keys.nextElement();
                this.dictionary.put(key, dict.get(key));
            }
        }
        this.digest = digest;
        this.priority = priority;
        this.url = url;
        final int pos = url.indexOf(':');
        this.urlScheme = url.substring(0, pos);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.TaskResource#getVersion()
     */
    public Version getVersion() {
        final String vInfo = (String)this.getAttribute(Constants.BUNDLE_VERSION);
        return (vInfo == null ? null : new Version(vInfo));
    }
}