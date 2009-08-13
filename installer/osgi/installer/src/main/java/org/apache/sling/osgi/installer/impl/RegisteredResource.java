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
import java.util.Dictionary;
import java.util.Hashtable;
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
	
	public static final String DIGEST_TYPE = "MD5";
	
	public RegisteredResource(BundleContext ctx, InstallableResource input) throws IOException {
		url = input.getUrl();
		
		try {
			if(input.getDictionary() == null) {
				dictionary = null;
				if(input.getInputStream() == null) {
					throw new IllegalArgumentException("input provides no Dictionary and no InputStream:" + input);
				} else {
					dataFile = getDataFile(ctx);
					digest = copyToLocalStorage(input.getInputStream(), dataFile);
				}
			} else {
				// TODO Copy dictionary
				dataFile = null;
				dictionary = input.getDictionary();
				digest = computeDigest(dictionary);
			}
			
    	} catch(NoSuchAlgorithmException nse) {
    		throw new IOException("NoSuchAlgorithmException:" + DIGEST_TYPE);
    		
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
    
    /** Copy data to local storage and return digest */
	private String copyToLocalStorage(InputStream data, File f) throws IOException, NoSuchAlgorithmException {
        final MessageDigest d = MessageDigest.getInstance(DIGEST_TYPE);
		final OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
		try {
			final byte[] buffer = new byte[16384];
			int count = 0;
			while( (count = data.read(buffer, 0, buffer.length)) > 0) {
				os.write(buffer, 0, count);
				d.update(buffer, 0, count);
			}
			os.flush();
		} finally {
			if(os != null) {
				os.close();
			}
		}
		return digestToString(d);
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
}
