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
package org.apache.sling.reseditor;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;

/**
 * Overrules the resource resolver to let the Sling Resource Editor render servlets that
 * have been registered by path.
 * 
 * E.g. the login servlet is registered by path using the URL
 * /system/sling/login. When calling /system/sling/login.reseditor.html the
 * servlet would usually be called to render the request. To render this
 * resource with the Sling Resource Editor instead, this ResourceDecorator removes the 
 * servlet resource type for requests that use the 'reseditor' selector in 
 * the path.
 * 
 */
public class SelectorBasedResourceDecorator implements ResourceDecorator {

	private static final String RESEDITOR_RESOURCE_TYPE = "reseditor";
	private static final String RESEDITOR_SELECTOR = "reseditor";

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
		if (resolutionPathInfo != null && resolutionPathInfo.endsWith("." + RESEDITOR_SELECTOR + ".html")) {
			result = new ResourceWrapper(resource) {
				@Override
				public String getResourceType() {
					/*
					 * It overwrites the resource types to avoid that the servlet 
					 * resource types have a higher priority then the
					 * Resource Editor's html.jsp.
					 */
					return RESEDITOR_RESOURCE_TYPE;
				}

			};
		}
		return result;
	}
}