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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(
        service = ResourceDecorator.class
        
)
@Designate(
	    ocd = LayoutSwitcherConfiguation.class
	)
public class LayoutSwitcher implements ResourceDecorator {

	private static final Logger LOG = LoggerFactory.getLogger(LayoutSwitcher.class);

	private LayoutSwitcherConfiguation config;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY)
	private volatile ResourceResolverFactory resourceResolverFactory;
	
	@Activate
	public void activate(final LayoutSwitcherConfiguation config) {
		this.config = config;
	}
	
	
	private boolean isPathConfigured(String path) {
		return StringUtils.isNotBlank(getSlingScriptFolder(path));

	}
	
	@Override
	public Resource decorate(Resource resource) {

		String path = resource.getPath();
		
		if ( ArrayUtils.isEmpty(config.configPaths()) || !isPathConfigured(path) ) {
			return null;
		} 
		
		if ( resource instanceof LayoutResource) {
			return null;
		}
				
		String scriptFolder = getSlingScriptFolder(path);

		String componentName = resource.getResourceType();
		if (componentName != null ) {
			componentName = StringUtils.substringAfterLast(componentName, "/");
		}
			
		if ( StringUtils.isNotBlank( scriptFolder) && StringUtils.isNotBlank(componentName)) {
		
			String resourceType = scriptFolder + "/" + componentName;
			
			if (resourceTypeExist(resourceType) ) {
				LayoutResource lr = new LayoutResource(resource);
				lr.setSlingResourceType(scriptFolder + "/" + componentName);
				
				LOG.trace("returning rt {}", lr.getSlingResourceType());
				
				return lr;
				
			}
		}
		return null;
		
	}
	
	private boolean resourceTypeExist(String resourceType) {
		
		ResourceResolver resolver = null;
		try {
			resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
			return resolver.getResource(resourceType) != null;
		} catch (Exception e) {
			return false;
		} finally {
			if ( resolver != null && resolver.isLive()) {
				resolver.close();
			}
		}
		
		
	}


	private String getSlingScriptFolder(String path) {
		for( String configPath : config.configPaths()) {
			String contentPath = StringUtils.substringBefore(configPath, ":");
			if ( path.startsWith(contentPath)) {
				String redrectionPath = StringUtils.substringAfter(configPath, ":");
				LOG.trace("Returning redirection path {}",redrectionPath);
				return redrectionPath;
			}
		}
		return StringUtils.EMPTY;

	}

	@Override
	public Resource decorate(Resource resource, HttpServletRequest arg1) {
		return this.decorate(resource);
	}

}
