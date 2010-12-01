/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.velocity;

import java.io.InputStream;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class SlingResourceLoader extends ResourceLoader {

    private VelocityScriptEngineService velocityScriptEngineService;

    private ResourceResolver resourceResolver;

    @Override
	public long getLastModified(Resource arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public InputStream getResourceStream(String absPath )
			throws ResourceNotFoundException {
		InputStream returnValue = null;
			
		if ( absPath != null && absPath.startsWith( "/" ) && resourceResolver != null )
		{
			org.apache.sling.api.resource.Resource resource = resourceResolver.resolve( absPath );
			if ( resource != null )
			{
				returnValue = resource.adaptTo( InputStream.class );
			}
		}
		
		return returnValue;
	}

	@Override
	public void init(ExtendedProperties arg0) {
		Bundle bundle = FrameworkUtil.getBundle( VelocityScriptEngineService.class );
		BundleContext bundleContext = bundle.getBundleContext();
		ServiceReference serviceRef = bundleContext.getServiceReference( VelocityScriptEngineService.class.getName() );
		
		if ( serviceRef != null )
		{
			velocityScriptEngineService = (VelocityScriptEngineService) bundleContext.getService( serviceRef );
			if ( velocityScriptEngineService != null )
			{
				resourceResolver = velocityScriptEngineService.getResourceResolver();
			}
		}
	}

	@Override
	public boolean isSourceModified(Resource arg0) {
		return true;
	}
	


}
