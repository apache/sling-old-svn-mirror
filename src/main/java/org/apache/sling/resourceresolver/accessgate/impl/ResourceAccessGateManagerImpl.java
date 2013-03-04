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
package org.apache.sling.resourceresolver.accessgate.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceAccessGate;
import org.apache.sling.resourceresolver.accessgate.ResourceAccessGateHandler;
import org.apache.sling.resourceresolver.accessgate.ResourceAccessGateManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@Component(
        name = "org.apache.sling.resourceresolver.accessgate.ResourceAccessGateManager",
        immediate = true )
@Service( value={ResourceAccessGateManager.class})
   @Properties({
       @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling ResourceAccessGateManager"),
       @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
   })
public class ResourceAccessGateManagerImpl implements ResourceAccessGateManager {
    
    private ResourceAccessGateTracker resourceAccessGateTracker;

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    @Activate
    protected void activate(final ComponentContext componentContext) {
        resourceAccessGateTracker = new ResourceAccessGateTracker( componentContext.getBundleContext() );
        resourceAccessGateTracker.open();
        
    }

    /**
     * Deativates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        resourceAccessGateTracker.close();
    }
    
    public List<ResourceAccessGateHandler> getMatchingResourceAccessGateHandlers ( String path, ResourceAccessGate.Operation operation ) {
        /* TODO: maybe caching some frequent paths with read operation would be a good idea */
        List<ResourceAccessGateHandler> returnValue = resourceAccessGateTracker.getResourceAccessGateHandlers();
        
        if ( returnValue.size() > 0 ) {
            returnValue = new ArrayList<ResourceAccessGateHandler>();
            
            for (ResourceAccessGateHandler resourceAccessGateHandler : resourceAccessGateTracker.getResourceAccessGateHandlers() ) {
                if ( resourceAccessGateHandler.matches(path, operation) ) {
                    returnValue.add(resourceAccessGateHandler);
                }
            }
        }
        
        return returnValue;
    }
    
    public boolean areResourceAccessGatesRegistered () {
        return (resourceAccessGateTracker.size() > 0 );
    }

}
