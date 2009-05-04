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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.osgi.framework.BundleContext;

/** Wraps InstallableData instances that provide an InputStream
 * 	so that their data comes from our BundleContext's storage.
 * 
 * 	Needed as InstallableData will be provided by a separate bundle,
 *  with InputStreams that might not be available anymore once
 *  the data is actually installed, for example if that data comes
 *  from a JCR repository that's deactivated due to bundle updates.
 */
class InstallableDataWrapper implements InstallableData {
	
	private InstallableData wrapped;
	private final File dataFile;
	private static int fileCounter;
	
	InstallableDataWrapper(InstallableData d, BundleContext bc) throws IOException {
		wrapped = d;
		
		// If d adapts to an input stream, save its content in
		// our BundleContext storage
		final InputStream is = wrapped.adaptTo(InputStream.class);
		if(is == null) {
			dataFile = null;
		} else {
			OutputStream os = null;
			try {
				String filename = getClass().getName();
				synchronized (getClass()) {
					filename += "." + (++fileCounter);
				}
				dataFile = bc.getDataFile(filename);
				
				os = new BufferedOutputStream(new FileOutputStream(dataFile));
				final byte[] buffer = new byte[16384];
				int count = 0;
				while((count = is.read(buffer, 0, buffer.length)) > 0) {
					os.write(buffer, 0, count);
				}
				os.flush();
			} finally {
				is.close();
				if(os != null) {
					os.close();
				}
			}
		}
	}

	public int getBundleStartLevel() {
		return wrapped.getBundleStartLevel();
	}
	
	/** Adapt the underlying data to the provided type.
	 *	@return null if cannot be adapted */
	@SuppressWarnings("unchecked")
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		// If we saved out content to a data file, use that
		if(InputStream.class.equals(type) && dataFile != null) {
			try {
				return (AdapterType)(new BufferedInputStream(new FileInputStream(dataFile)));
			} catch(IOException ioe) {
				throw new IllegalStateException("Unable to open data file " + dataFile.getAbsolutePath());
			}
		} else {
			return wrapped.adaptTo(type);
		}
	}

	public String getDigest() {
		return wrapped.getDigest();
	}
	
	void cleanup() {
		if(dataFile != null) {
			dataFile.delete();
		}
		wrapped = null;
	}

}
