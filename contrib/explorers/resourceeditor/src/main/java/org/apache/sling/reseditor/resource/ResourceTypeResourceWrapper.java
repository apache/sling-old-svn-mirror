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
package org.apache.sling.reseditor.resource;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * This ResourceWrapper adds a marker to the ResourceMetadata object that the {@link ResourceProviderBasedResourceDecorator}
 * uses.
 */
public class ResourceTypeResourceWrapper extends ResourceWrapper {

	public ResourceTypeResourceWrapper(Resource resource) {
		super(resource);
	}

	@Override
	public ResourceMetadata getResourceMetadata() {
		ResourceMetadata newResourceMetadata = new ResourceMetadata();
		newResourceMetadata.putAll(getResource().getResourceMetadata());
		newResourceMetadata.put(ResEditorResourceProvider.RESOURCE_EDITOR_PROVIDER_RESOURCE, null);
		return newResourceMetadata;
	}

	@Override
    public Iterator<Resource> listChildren() {
		Iterator<Resource> originalIterator = this.getResource().listChildren();
		return new ResourceIteratorWrapper(originalIterator);
	}
}
