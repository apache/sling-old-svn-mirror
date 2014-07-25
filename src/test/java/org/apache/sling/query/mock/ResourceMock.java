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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceMock implements Resource {

	private final String name;

	private final Map<String, Resource> children;

	private final Map<String, String> properties;

	private final Resource parent;

	public ResourceMock(Resource parent, String name) {
		this.name = name;
		this.parent = parent;
		this.children = new LinkedHashMap<String, Resource>();
		this.properties = new LinkedHashMap<String, String>();
	}

	public void setProperty(String name, String value) {
		properties.put(name, value);
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
		return children.values().iterator();
	}

	@Override
	public Resource getChild(String relPath) {
		if (StringUtils.contains(relPath, '/')) {
			String firstPart = StringUtils.substringBefore(relPath, "/");
			String rest = StringUtils.substringAfter(relPath, "/");
			if (children.containsKey(firstPart)) {
				return children.get(firstPart).getChild(rest);
			}
		} else {
			if (children.containsKey(relPath)) {
				return children.get(relPath);
			} else if (properties.containsKey(relPath)) {
				return new StringResourceMock(this, relPath, properties.get(relPath));
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if (type.isAssignableFrom(Map.class)) {
			return (AdapterType) properties;
		} else {
			return null;
		}
	}

	@Override
	public boolean isResourceType(String resourceType) {
		return StringUtils.isNotBlank(resourceType)
				&& (resourceType.equals(properties.get("sling:resourceType")) || resourceType
						.equals(properties.get("jcr:primaryType")));
	}

	@Override
	public String getResourceType() {
		if (properties.containsKey("sling:resourceType")) {
			return properties.get("sling:resourceType");
		} else {
			return properties.get("jcr:primaryType");
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

}
