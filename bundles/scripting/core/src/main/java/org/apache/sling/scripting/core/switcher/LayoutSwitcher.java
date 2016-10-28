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
package org.apache.sling.scripting.core.switcher;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(value = ResourceDecorator.class)
@Component
public class LayoutSwitcher implements ResourceDecorator {

	private static final Logger LOG = LoggerFactory.getLogger(LayoutSwitcher.class);

	@Reference
	private LayoutThreadLocalService threadLocalService;

	@Override
	public Resource decorate(Resource resource) {
		
		String path = resource.getPath();
		if (path.indexOf(".html") != -1 && (path.startsWith("/apps") || path.startsWith("/libs"))) {
			
			// only decorating paths like /apps/path/file.html or /libs/path/file.html
			if (LOG.isTraceEnabled()) {
				LOG.trace("decorate called for : " + resource.getPath());
				LOG.trace("thread local : " + threadLocalService.get());
			}

			String context = threadLocalService.get();

			String scriptFolder = null;

			if (context != null) {
				scriptFolder = getSlingScriptFolder(context, resource.getResourceResolver());
			} else {
				return null;
			}

			if (scriptFolder == null) {
				return null;
			}
			
			if ( resource.getPath().startsWith(scriptFolder) ) {
				// to prevent endless loop and stackoverlow
				return null;
			}

			return getIfExisting(scriptFolder, resource.getPath(), resource.getResourceResolver());
		}

		return null;
	}

	private String getSlingScriptFolder(String path, ResourceResolver rr) {
		String scriptFolder = null;
		Resource r = rr.getResource(path);
		while (r != null && scriptFolder == null) {
			Resource child = r.getChild("jcr:content");
			if (child != null) {
				scriptFolder = child.getValueMap().get("sling:scriptFolder", String.class);
			}

			r = r.getParent();
		}
		return scriptFolder;

	}

	private Resource getIfExisting(String folder, String path, ResourceResolver rr) {
		String layoutPath = folder + "/" + StringUtils.substringAfterLast(path, "/");

		return rr.getResource(layoutPath);

	}

	@Override
	public Resource decorate(Resource resource, HttpServletRequest arg1) {
		return this.decorate(resource);
	}

}
