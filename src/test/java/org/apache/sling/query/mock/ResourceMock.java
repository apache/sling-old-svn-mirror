/*-
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

package org.apache.sling.query.mock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceMock implements Resource {

	private final String name;

	private final Map<String, Resource> children;

	private final Resource parent;

	public ResourceMock(Resource parent, String name) {
		this.name = name;
		this.parent = parent;
		this.children = new LinkedHashMap<String, Resource>();
	}

	public void addChild(Resource resource) {
		children.put(resource.getName(), resource);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Resource getParent() {
		return parent;
	}

	@Override
	public Iterator<Resource> listChildren() {
		List<Resource> nonProperties = new ArrayList<Resource>();
		for (Resource r : children.values()) {
			if (!(r instanceof PropertyResourceMock)) {
				nonProperties.add(r);
			}
		}
		return nonProperties.iterator();
	}

	@Override
	public Resource getChild(String relPath) {
		if (StringUtils.contains(relPath, '/')) {
			String firstPart = StringUtils.substringBefore(relPath, "/");
			String rest = StringUtils.substringAfter(relPath, "/");
			if (children.containsKey(firstPart)) {
				return children.get(firstPart).getChild(rest);
			}
		} else if (children.containsKey(relPath)) {
			return children.get(relPath);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if (type.isAssignableFrom(Map.class)) {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for (Entry<String, Resource> e : children.entrySet()) {
				Resource o = e.getValue();
				String[] stringArray = o.adaptTo(String[].class);
				String stringValue = o.adaptTo(String.class);
				if (stringValue != null) {
					map.put(e.getKey(), stringValue);
				} else if (stringArray != null) {
					map.put(e.getKey(), stringArray);
				}
			}
			return (AdapterType) map;
		} else {
			return null;
		}
	}

	@Override
	public boolean isResourceType(String resourceType) {
		return StringUtils.isNotBlank(resourceType)
				&& (resourceType.equals(getPropertyAsString("sling:resourceType")) || resourceType
						.equals(getPropertyAsString("jcr:primaryType")));
	}

	@Override
	public String getResourceType() {
		if (children.containsKey("sling:resourceType")) {
			return getPropertyAsString("sling:resourceType");
		} else {
			return getPropertyAsString("jcr:primaryType");
		}
	}

	@Override
	public String getPath() {
		if (parent == null) {
			return "";
		} else {
			return String.format("%s/%s", parent.getPath(), name);
		}
	}

	@Override
	public String getResourceSuperType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResourceMetadata getResourceMetadata() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResourceResolver getResourceResolver() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("ResourceMock[%s]", name);
	}

	private String getPropertyAsString(String name) {
		if (children.containsKey(name)) {
			return children.get(name).adaptTo(String.class);
		}
		return null;
	}

}
