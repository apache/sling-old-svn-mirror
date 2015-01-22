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

import java.util.Arrays;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

public class PropertyResourceMock implements Resource {

	private final String name;

	private final Resource parent;

	private final String value;

	private final String[] values;

	public PropertyResourceMock(Resource parent, String name, String value) {
		this.parent = parent;
		this.name = name;
		this.value = value;
		this.values = null;
	}

	public PropertyResourceMock(Resource parent, String name, String[] values) {
		this.parent = parent;
		this.name = name;
		this.value = null;
		this.values = values;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if (type.isAssignableFrom(String.class)) {
			return (AdapterType) value;
		} else if (type.isAssignableFrom(String[].class)) {
			return (AdapterType) (value == null ? values : new String[] { value });
		} else {
			return null;
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
	public String getName() {
		return name;
	}

	@Override
	public Resource getParent() {
		return parent;
	}

	@Override
	public Iterator<Resource> listChildren() {
		return Arrays.<Resource> asList().iterator();
	}

	@Override
	public Resource getChild(String relPath) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getResourceType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getResourceSuperType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isResourceType(String resourceType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResourceMetadata getResourceMetadata() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResourceResolver getResourceResolver() {
		throw new UnsupportedOperationException();
	}

}
