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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.impl.config.ConfigurationPid;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Implementation of the registered resource
 */
public class RegisteredResourceImpl
    implements RegisteredResource, Serializable {

    private static final String ENTITY_BUNDLE_PREFIX = "bundle:";
    private static final String ENTITY_CONFIG_PREFIX = "config:";

    private static final long serialVersionUID = 3L;
    private final String id;
	private final String urlScheme;
	private final String digest;
	private final String entity;
	private final Dictionary<String, Object> dictionary;
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private boolean installable = true;
	private final File dataFile;
	private final int priority;
    private final long serialNumber;
    private static long serialNumberCounter = System.currentTimeMillis();

    private final String resourceType;

	/**
	 * Create a RegisteredResource from given data.
	 * As this data object is filled from an {@link InstallableResource}
	 * we don't have to validate values - this has already been done
	 * by the installable resource!
	 */
	public RegisteredResourceImpl(final BundleContext ctx,
	        final InstallableResource input,
	        final String scheme) throws IOException {
        this.id = input.getId();
        this.urlScheme = scheme;
		this.resourceType = input.getType();
		this.priority = input.getPriority();
        this.dictionary = copy(input.getDictionary());
        this.digest = input.getDigest();
		this.serialNumber = getNextSerialNumber();

		if (resourceType.equals(InstallableResource.TYPE_BUNDLE)) {
		    final InputStream is = input.getInputStream();
            try {
                this.dataFile = getDataFile(ctx);
                Logger.logDebug("Copying data to local storage " + this.dataFile);
                copyToLocalStorage(input.getInputStream());
                setAttributesFromManifest();
                final String name = (String)attributes.get(Constants.BUNDLE_SYMBOLICNAME);
                if (name == null) {
                    // not a bundle
                    throw new IOException("Bundle resource does not contain a bundle " + this.urlScheme + ":" + this.id);
                }
                entity = ENTITY_BUNDLE_PREFIX + name;
            } finally {
                is.close();
            }
		} else if ( resourceType.equals(InstallableResource.TYPE_CONFIG)) {
            this.dataFile = null;
            final ConfigurationPid pid = new ConfigurationPid(this.getURL());
            entity = ENTITY_CONFIG_PREFIX + pid.getCompositePid();
            attributes.put(CONFIG_PID_ATTRIBUTE, pid);
		} else {
		    throw new IOException("Unknown type " + resourceType);
		}
	}

    private static long getNextSerialNumber() {
        synchronized (RegisteredResourceImpl.class) {
            return serialNumberCounter++;
        }
    }

	@Override
	public String toString() {
	    return getClass().getSimpleName() + " " + this.getURL() + ", digest=" + this.getDigest() + ", serialNumber=" + this.getSerialNumber();
	}

	protected File getDataFile(final BundleContext bundleContext) {
		final String filename = getClass().getSimpleName() + "." + serialNumber;
		return bundleContext.getDataFile(filename);
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#cleanup()
	 */
	public void cleanup() {
	    if ( this.dataFile != null && this.dataFile.exists() ) {
		    Logger.logDebug("Deleting local storage file "
		                + dataFile.getAbsolutePath());
			dataFile.delete();
		}
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getURL()
	 */
	public String getURL() {
		return this.getScheme() + ':' + this.getId();
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
	    if (this.dataFile != null && this.dataFile.exists() ) {
	        return new BufferedInputStream(new FileInputStream(this.dataFile));
	    }
        return  null;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getDictionary()
	 */
	public Dictionary<String, Object> getDictionary() {
		return dictionary;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getDigest()
	 */
	public String getDigest() {
		return digest;
	}

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getSerialNumber()
     */
    public long getSerialNumber() {
        return this.serialNumber;
    }

    /** Copy data to local storage */
	private void copyToLocalStorage(final InputStream data) throws IOException {
		final OutputStream os = new BufferedOutputStream(new FileOutputStream(this.dataFile));
		try {
			final byte[] buffer = new byte[16384];
			int count = 0;
			while( (count = data.read(buffer, 0, buffer.length)) > 0) {
				os.write(buffer, 0, count);
			}
			os.flush();
		} finally {
			os.close();
		}
	}

	/** Copy given Dictionary */
	private Dictionary<String, Object> copy(final Dictionary<String, Object> d) {
	    if ( d == null ) {
	        return null;
	    }
	    final Dictionary<String, Object> result = new Hashtable<String, Object>();
	    final Enumeration<String> e = d.keys();
	    while(e.hasMoreElements()) {
	        final String key = e.nextElement();
            result.put(key, d.get(key));
	    }
	    return result;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getId()
	 */
	public String getId() {
	    return id;
	}

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getType()
     */
    public String getType() {
        return resourceType;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getEntityId()
     */
    public String getEntityId() {
        return entity;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getAttributes()
     */
    public Map<String, Object> getAttributes() {
		return attributes;
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

    /** Read the manifest from supplied input stream, which is closed before return */
    private Manifest getManifest(InputStream ins) throws IOException {
        Manifest result = null;

        JarInputStream jis = null;
        try {
            jis = new JarInputStream(ins);
            result= jis.getManifest();

        } finally {

            // close the jar stream or the inputstream, if the jar
            // stream is set, we don't need to close the input stream
            // since closing the jar stream closes the input stream
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException ignore) {
                }
            } else {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

        return result;
    }

    private void setAttributesFromManifest() throws IOException {
    	final Manifest m = getManifest(getInputStream());
    	if(m == null) {
            throw new IOException("Cannot get manifest of bundle resource");
    	}

    	final String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        if(sn == null) {
            throw new IOException("Manifest does not supply " + Constants.BUNDLE_SYMBOLICNAME);
        }

    	final String v = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        if(v == null) {
            throw new IOException("Manifest does not supply " + Constants.BUNDLE_VERSION);
        }

        attributes.put(Constants.BUNDLE_SYMBOLICNAME, sn);
        attributes.put(Constants.BUNDLE_VERSION, v.toString());
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getScheme()
     */
    public String getScheme() {
        return urlScheme;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.RegisteredResource#getPriority()
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final RegisteredResource b) {
        return compare(this, b);
    }

    public static int compare(final RegisteredResource a, final RegisteredResource b) {
        final boolean aBundle = a.getType().equals(InstallableResource.TYPE_BUNDLE);
        final boolean bBundle = b.getType().equals(InstallableResource.TYPE_BUNDLE);

        if (aBundle && bBundle) {
            return compareBundles(a, b);
        } else if (!aBundle && !bBundle){
            return compareConfig(a, b);
        } else if (aBundle) {
            return 1;
        } else {
            return -1;
        }
    }

    private static int compareBundles(final RegisteredResource a, final RegisteredResource b) {
        boolean isSnapshot = false;
        int result = 0;

        // Order first by symbolic name
        final String nameA = (String)a.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final String nameB = (String)b.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        if(nameA != null && nameB != null) {
            result = nameA.compareTo(nameB);
        }

        // Then by version
        if(result == 0) {
            final Version va = new Version((String)a.getAttributes().get(Constants.BUNDLE_VERSION));
            final Version vb = new Version((String)b.getAttributes().get(Constants.BUNDLE_VERSION));
            isSnapshot = va.toString().contains("SNAPSHOT");
            // higher version has more priority, must come first so invert comparison
            result = vb.compareTo(va);
        }

        // Then by priority, higher values first
        if(result == 0) {
            if(a.getPriority() < b.getPriority()) {
                result = 1;
            } else if(a.getPriority() > b.getPriority()) {
                result = -1;
            }
        }

        if(result == 0 && isSnapshot) {
            // For snapshots, compare serial numbers so that snapshots registered
            // later get priority
            if(a.getSerialNumber() < b.getSerialNumber()) {
                result = 1;
            } else if(a.getSerialNumber() > b.getSerialNumber()) {
                result = -1;
            }
        }

        return result;
    }

    private static int compareConfig(final RegisteredResource a, final RegisteredResource b) {
        int result = 0;

        // First compare by pid
        final ConfigurationPid pA = (ConfigurationPid)a.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE);
        final ConfigurationPid pB = (ConfigurationPid)b.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE);
        if(pA != null && pA.getCompositePid() != null && pB != null && pB.getCompositePid() != null) {
            result = pA.getCompositePid().compareTo(pB.getCompositePid());
        }

        // Then by priority, higher values first
        if(result == 0) {
            if(a.getPriority() < b.getPriority()) {
                result = 1;
            } else if( a.getPriority() > b.getPriority()) {
                result = -1;
            }
        }

        return result;
    }
}