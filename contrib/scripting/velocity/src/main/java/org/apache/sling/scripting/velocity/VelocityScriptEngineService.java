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
package org.apache.sling.scripting.velocity;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.BundleContext;

@Component(name = "org.apache.sling.scripting.velocity.VelocityScriptEngineService")
@Service( value = VelocityScriptEngineService.class )
public class VelocityScriptEngineService {
	
	@Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver resourceResolver;
    
    public static final String PROP_RESOURCE_LOADER_USER = "velocityresourceloader.user"; 
    
    @Property( name = PROP_RESOURCE_LOADER_USER )
    public static final String DEFAULT_RESOURCE_LOADER_USER = null;
    
    public ResourceResolver getResourceResolver ()
    {
    	return resourceResolver;
    }
    
    @SuppressWarnings("unused")
    @Activate
    private void activate(final BundleContext bundleContext,
            final Map<String, Object> properties) throws LoginException {
		if ( resourceResolverFactory != null )
		{
			Map<String, Object> authInfo = new HashMap<String, Object>();
	        // Use the user which is configured to access the resources if available
			Object prop = properties.get(PROP_RESOURCE_LOADER_USER);
	        final String resourceLoaderUser = ( prop != null ) ? prop.toString() : null;
	        if (resourceLoaderUser != null && resourceLoaderUser.length() > 0) {
	            authInfo.put(ResourceResolverFactory.USER_IMPERSONATION, resourceLoaderUser);
	        }
			
			resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver( authInfo );
		}
    }

    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate(final BundleContext bundleContext) {
    }
    

}
