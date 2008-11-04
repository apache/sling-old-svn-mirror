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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Dictionary;

import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;

/** InstallableData that wraps a Dictionary */
class ConfigInstallableData implements InstallableData {

	private final Dictionary<String, Object> data;
	private final String digest;
	
	ConfigInstallableData(Dictionary<String, Object> data) throws Exception {
		this.data = data;
		digest = computeDigest(data);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ", digest=" + digest;
	}
	
	@SuppressWarnings("unchecked")
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if(type.equals(Dictionary.class)) {
			return (AdapterType)data;
		}
		return null;
	}

	public String getDigest() {
		return digest;
	}
	
	/** Digest is needed to detect changes in data */
	static String computeDigest(Dictionary<String, Object> data) throws Exception {
		final String digestType = "MD5";
		final MessageDigest d = MessageDigest.getInstance(digestType);
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(data);
		bos.flush();
		d.update(bos.toByteArray());
		// convert to readable string (http://www.javalobby.org/java/forums/t84420.html)
		final BigInteger bigInt = new BigInteger(1, d.digest());
		return new String(bigInt.toString(16));
	}
}