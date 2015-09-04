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

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * Decorates resources that have been called with the {@link ResEditorResourceProvider} with the resource-editor resource type.
 * 
 */

@Component
@Service(ResourceDecorator.class)
@Properties({
 @Property(name = "service.description", value = "Decorates resources that have been called with the ResEditorResourceProvider with the resource-editor resource type."),
 @Property(name = "service.vendor", value = "The Apache Software Foundation")
})
public class ResourceProviderBasedResourceDecorator implements ResourceDecorator {

	/**
	 * @see org.apache.sling.api.resource.ResourceDecorator#decorate(org.apache.sling.api.resource.Resource,
	 *      javax.servlet.http.HttpServletRequest)
	 */
	public Resource decorate(Resource resource, HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		return getResourceEditorResourceWrapper(resource,
				pathInfo);
	}

	/**
	 * @see org.apache.sling.api.resource.ResourceDecorator#decorate(org.apache.sling.api.resource.Resource)
	 */
	public Resource decorate(Resource resource) {
		Resource result = null;
		if (resource != null) {
			ResourceMetadata resourceMetadata = resource.getResourceMetadata();
			if (resourceMetadata != null) {
				String resolutionPathInfo = resourceMetadata.getResolutionPathInfo();
				result = getResourceEditorResourceWrapper(resource,resolutionPathInfo);
			}
		}
		return result;
	}

	private Resource getResourceEditorResourceWrapper(Resource resource, String resolutionPathInfo) {
		Resource result = null;
		ResourceMetadata resourceMetadata = resource.getResourceMetadata();
		boolean isResourceEditorProviderResource = resourceMetadata != null ? resourceMetadata.containsKey(ResEditorResourceProvider.RESOURCE_EDITOR_PROVIDER_RESOURCE) : false;
		boolean isHTMLResource = resolutionPathInfo != null && resolutionPathInfo.endsWith("html"); 
		boolean isJSONResource = resolutionPathInfo != null && resolutionPathInfo.endsWith("json"); 
		if ((isHTMLResource || isJSONResource) && isResourceEditorProviderResource) {
			result = new ResourceWrapper(resource) {
				@Override
				public String getResourceType() {
					return ResEditorResourceProvider.RESEDITOR_RESOURCE_TYPE;
				}

			};
		}
		return result;
	}
}