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
package org.apache.sling.osgi.installer;

import java.io.InputStream;
import java.util.Dictionary;

/** A piece of data that can be installed by the OSGi controller.
 * 	Wraps either a Dictionary or an InputStream.
 *  Extension is used to decide which type of data (bundle, config, etc.). 
 */
public class InstallableResource {
	private final String url;
	private final String extension;
	private final InputStream inputStream;
	private final Dictionary<String, Object> dictionary;
	
	/** Create an empty data object, used when removing resources */
	public InstallableResource(String url) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = null;
		this.dictionary = null;
	}
	
	/** Create a data object that wraps an InputStream */
	public InstallableResource(String url, InputStream is) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = is;
		this.dictionary = null;
	}
	
	/** Create a data object that wraps a Dictionary */
	public InstallableResource(String url, Dictionary<String, Object> d) {
		this.url = url;
		this.extension = getExtension(url);
		this.inputStream = null;
		this.dictionary = d;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ", url=" + url;
	}
	
	/** Compute the extension */ 
	private static String getExtension(String url) {
		final int pos = url.lastIndexOf('.');
		return (pos < 0 ? "" : url.substring(pos+1));
	}
	
	/** Return this data's URL. It is opaque for the {@link OsgiController}
	 * 	but the scheme must be the one used in the 
	 * 	{@link OsgiController#registerResources} call.
	 */
	public String getURL() {
		return url;
	}

	public String getUrl() {
		return url;
	}

	public String getExtension() {
		return extension;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public Dictionary<String, Object> getDictionary() {
		return dictionary;
	}
}
