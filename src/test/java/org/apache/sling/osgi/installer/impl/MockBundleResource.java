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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Mock RegisteredResource that simulates a bundle */
public class MockBundleResource implements RegisteredResource {

	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private boolean installable = true;
	private final String digest;
	
	MockBundleResource(String symbolicName, String version) {
		attributes.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		attributes.put(Constants.BUNDLE_VERSION, new Version(version));
		digest = symbolicName + "." + version;
	}
	
	public void cleanup() {
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Dictionary<String, Object> getDictionary() {
		return null;
	}

	public String getDigest() {
		return digest;
	}

	public String getEntityId() {
		return null;
	}

	public InputStream getInputStream() throws IOException {
		return null;
	}

	public ResourceType getResourceType() {
		return RegisteredResource.ResourceType.BUNDLE;
	}

	public String getUrl() {
		return null;
	}

	public String getURL() {
		return null;
	}
	
    public String getUrlScheme() {
        return null;
    }

    public boolean isInstallable() {
        return installable;
    }

    public void setInstallable(boolean installable) {
        this.installable = installable;
    }
}
