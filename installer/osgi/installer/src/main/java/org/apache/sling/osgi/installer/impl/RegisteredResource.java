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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.impl.propertyconverter.PropertyConverter;
import org.apache.sling.osgi.installer.impl.propertyconverter.PropertyValue;
import org.osgi.framework.BundleContext;

/** A resource that's been registered in the OSGi controller.
 * 	Data can be either an InputStream or a Dictionary, and we store
 *  it locally to avoid holding up to classes or data from our 
 *  clients, in case those disappear while we're installing stuff. 
 */
public class RegisteredResource {
	private final String url;
	private final String digest;
	private final File dataFile;
	private final Dictionary<String, Object> dictionary;
	private static long fileNumber;
	
	static enum State {
	    NEW,
	    ACTIVE,
        INSTALLED,
	    IGNORED,
	    REMOVED,
	}
    private State desiredState = State.ACTIVE;
    private State actualState = State.NEW;
    
    static enum ResourceType {
        BUNDLE,
        CONFIG
    }
    
    private final ResourceType resourceType;
	
	public static final String DIGEST_TYPE = "MD5";
	
	/** Create a RegisteredResource from given data. If the data's extension
	 *  maps to a configuration and the data provides an input stream, it is
	 *  converted to a Dictionary 
	 */
	public RegisteredResource(BundleContext ctx, InstallableResource input) throws IOException {
	    
	    try {
    		url = input.getUrl();
    		resourceType = computeResourceType(input.getExtension());
    		
    		if(resourceType == ResourceType.BUNDLE) {
                if(input.getInputStream() == null) {
                    throw new IllegalArgumentException("InputStream is required for BUNDLE resource type: " + input);
                }
                dictionary = null;
                dataFile = getDataFile(ctx);
                copyToLocalStorage(input.getInputStream(), dataFile);
                digest = input.getDigest();
                if(digest == null || digest.length() == 0) {
                    throw new IllegalArgumentException(
                            "Digest must be supplied for BUNDLE resource type: " + input);
                }
    		} else {
                dataFile = null;
                if(input.getInputStream() == null) {
                    // config provided as a Dictionary
                    dictionary = copy(input.getDictionary());
                } else {
                    dictionary = readDictionary(input.getInputStream()); 
                }
                try {
                    digest = computeDigest(dictionary);
                } catch(NoSuchAlgorithmException nse) {
                    throw new IOException("NoSuchAlgorithmException:" + DIGEST_TYPE);
                }
    		}
    	} finally {
    		if(input.getInputStream() != null) {
    			input.getInputStream().close();
    		}
    	}
	}
	
	protected File getDataFile(BundleContext ctx) throws IOException {
		String filename = null;
		synchronized (getClass()) {
			filename = getClass().getSimpleName() + "." + fileNumber++;
		}
		return ctx.getDataFile(filename);
	}
	
	public void cleanup() {
		if(dataFile != null && dataFile.exists()) {
			dataFile.delete();
		}
	}
	
	public String getURL() {
		return url;
	}
	
	public InputStream getInputStream() throws IOException {
		if(dataFile == null) {
			return null;
		}
		return new BufferedInputStream(new FileInputStream(dataFile));
	}
	
	public Dictionary<String, Object> getDictionary() {
		return dictionary;
	}
	
	public String getDigest() {
		return digest;
	}
	
    /** Digest is needed to detect changes in data 
     * @throws  */
    static String computeDigest(Dictionary<String, Object> data) throws IOException, NoSuchAlgorithmException {
        final MessageDigest d = MessageDigest.getInstance(DIGEST_TYPE);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        bos.flush();
        d.update(bos.toByteArray());
        return digestToString(d);
    }

    /** convert digest to readable string (http://www.javalobby.org/java/forums/t84420.html) */
    private static String digestToString(MessageDigest d) {
        final BigInteger bigInt = new BigInteger(1, d.digest());
        return new String(bigInt.toString(16));
    }
    
    /** Copy data to local storage */
	private void copyToLocalStorage(InputStream data, File f) throws IOException {
		final OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
		try {
			final byte[] buffer = new byte[16384];
			int count = 0;
			while( (count = data.read(buffer, 0, buffer.length)) > 0) {
				os.write(buffer, 0, count);
			}
			os.flush();
		} finally {
			if(os != null) {
				os.close();
			}
		}
	}
	
	/** Convert InputStream to Dictionary using our extended properties format,
	 * 	which supports multi-value properties 
	 */
	static Dictionary<String, Object> readDictionary(InputStream is) throws IOException {
		final Dictionary<String, Object> result = new Hashtable<String, Object>();
		final PropertyConverter converter = new PropertyConverter();
		final Properties p = new Properties();
        p.load(is);
        for(Map.Entry<Object, Object> e : p.entrySet()) {
            final PropertyValue v = converter.convert((String)e.getKey(), (String)e.getValue());
            result.put(v.getKey(), v.getValue());
        }
        return result;
	}
	
	/** Copy given Dictionary, sorting keys */
	static Dictionary<String, Object> copy(Dictionary<String, Object> d) {
	    final Dictionary<String, Object> result = new Hashtable<String, Object>();
	    final List<String> keys = new ArrayList<String>();
	    final Enumeration<String> e = d.keys();
	    while(e.hasMoreElements()) {
	        keys.add(e.nextElement());
	    }
	    Collections.sort(keys);
	    for(String key : keys) {
	        result.put(key, d.get(key));
	    }
	    return result;
	}
	
	public String getUrl() {
	    return url;
	}

    public State getDesiredState() {
        return desiredState;
    }

    public void setDesiredState(State desiredState) {
        this.desiredState = desiredState;
    }

    public State getActualState() {
        return actualState;
    }

    public void setActualState(State actualState) {
        this.actualState = actualState;
    }
    
    public ResourceType getResourceType() {
        return resourceType;
    }
    
    static ResourceType computeResourceType(String extension) {
        if(extension.equals("jar")) {
            return ResourceType.BUNDLE;
        } else {
            return ResourceType.CONFIG;
        }
    }
}
