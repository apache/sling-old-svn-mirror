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
package org.apache.sling.testing.resourceresolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;

public class MockResourceResolverBuilder {
	private final Map<String, File> jsonFilesToLoad;
	private ResourceResolver resolver;
	private final MockResourceResolverFactory mockResolverFactory;

	public MockResourceResolverBuilder() {
		jsonFilesToLoad = new HashMap<String, File>();
		mockResolverFactory = new MockResourceResolverFactory();
	}

	public MockResourceResolverBuilder addResourcesToLoad(
			final String basePath, final URL fileURL)
			throws URISyntaxException, FileNotFoundException {
		if (fileURL == null) {
			throw new FileNotFoundException("Cannot load a null file path.");
		}
		final File targetFile = new File(fileURL.toURI());
		if (!targetFile.exists()) {
			throw new FileNotFoundException(targetFile.getAbsolutePath()
					+ " does not exist.");
		}
		jsonFilesToLoad.put(basePath, targetFile);
		return this;
	}

	public ResourceResolver load() throws IOException, JSONException,
			LoginException {
		this.resolver = mockResolverFactory.getResourceResolver(null);
		Resource checkResource = null;
		File targetFile = null;
		String fileData;
		// The mock builder always creates a root resource for us.
		Resource parentResource;
		for (Entry<String, File> entry : jsonFilesToLoad.entrySet()) {
			parentResource = this.resolver.getResource("/");
			targetFile = entry.getValue();
			fileData = FileUtils.readFileToString(targetFile);

			JSONTokener jsonTokenizer = new JSONTokener(fileData);
			// This assumes the thing being loaded starts as an object.
			JSONObject rootObj = (JSONObject) jsonTokenizer.nextValue();

			// Create all the parents needed, up till the last one as
			// parseObject will create the deepest child.
			StringTokenizer nameTokenizer = new StringTokenizer(entry.getKey(),
					"/");
			String currentToken;
			while (nameTokenizer.hasMoreTokens()) {
				currentToken = nameTokenizer.nextToken();
				// Don't create the last child because parseObject will do that for us.
				if (nameTokenizer.hasMoreTokens()) {
					// Because we can load multiple trees never overwrite one
					// resource with another when creating dummys.
					checkResource = parentResource.getChild(currentToken);
					if (checkResource != null) {
						parentResource = checkResource;
						continue;
					}
					parentResource = this.resolver.create(parentResource,
							currentToken, Collections.<String, Object> emptyMap());					
				}
			}
			parseObject(entry.getKey(), this.resolver, rootObj);
		}
		// Was trying to reset the Mock to just forget the interactions and not
		// the stubbing but it doesn't work:
		// https://code.google.com/p/mockito/issues/detail?id=316
		// Mockito.reset(Mockito.ignoreStubs(resolver));
		return this.resolver;

	}

	private void parseObject(final String currentPath,
			final ResourceResolver rr, final JSONObject jsonObj)
			throws JSONException, PersistenceException {
		final Resource parent = rr.getResource(ResourceUtil
				.getParent(currentPath));
		final String resourceName = ResourceUtil.getName(currentPath);

		Iterator<String> keyIterator = jsonObj.keys();
		String key;
		Map<String, JSONObject> pendingChildren = new HashMap<String, JSONObject>();
		Map<String, Object> properties = new HashMap<String, Object>();
		// This loops all the properties of the resource, N could be child
		// resources.
		while (keyIterator.hasNext()) {
			key = keyIterator.next();
			Object property = jsonObj.get(key);
			if (property instanceof JSONObject) {
				// Handle nested resource
				pendingChildren.put(currentPath + "/" + key,
						(JSONObject) property);
			} else if (property instanceof JSONArray) {
				// handle array project object
				JSONArray array = (JSONArray) property;
				Object[] values = new Object[array.length()];
				for (int i = 0; i < array.length(); i++) {
					values[i] = array.get(i);
				}
				properties.put(key, values);
			} else {
				properties.put(key, property);
			}
		}
		rr.create(parent, resourceName, properties);
		for (Entry<String, JSONObject> childEntry : pendingChildren.entrySet()) {
			parseObject(childEntry.getKey(), rr, childEntry.getValue());
		}
	}

}
