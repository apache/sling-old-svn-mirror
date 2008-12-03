/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jcrinstall.integrationtest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import aQute.bnd.main.bnd;

/** Generate a clone of an OSGi bundle, with its
 * 	own Bundle-Name and Bundle-SymbolicName.
 */
public class BundleCloner {
	private final bnd bnd = new bnd();

	/** Create a clone of supplied bundle */
	public void cloneBundle(File bundle, File output, String name, String symbolicName) throws Exception {
		int options = 0;
		
		Properties props = new Properties();
		props.put("Bundle-Name", name);
		props.put("Bundle-SymbolicName", symbolicName);
		File properties = File.createTempFile(getClass().getSimpleName(), "properties");
		final OutputStream out = new FileOutputStream(properties);
		
		File classpath[] = null;
		try {
			props.store(out, getClass().getSimpleName());
			bnd.doWrap(properties, bundle, output, classpath, options, null);
		} finally {
			if(out != null) {
				out.close();
			}
			if(properties != null) {
				properties.delete();
			}
		}
	}
}
