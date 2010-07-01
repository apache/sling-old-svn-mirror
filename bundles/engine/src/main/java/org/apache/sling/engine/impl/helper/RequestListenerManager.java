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

package org.apache.sling.engine.impl.helper;

import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.request.SlingRequestListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestListenerManager  {
	
    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private ServiceTracker serviceTracker;

	public RequestListenerManager ( BundleContext context )
	{
		serviceTracker = new ServiceTracker( context, SlingRequestListener.SERVICE_NAME, null );
		serviceTracker.open();
		
	}
	
	public void sendEvent ( SlingRequestEvent event )
	{
		Object[] services = serviceTracker.getServices();
		if ( services != null )
		{
			for ( Object service : services )
			{
				if ( service instanceof SlingRequestListener )
				{
					( (SlingRequestListener) service ).onEvent( event );
				}
				else
				{
					log.error( "Implementation of service named " + SlingRequestListener.SERVICE_NAME + 
							" does not implement service interface " + SlingRequestListener.class.getName() + "." );
				}
			}
		}
	}
	

}
