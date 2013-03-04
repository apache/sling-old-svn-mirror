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
package org.apache.sling.resourceresolver.accessgate.impl;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceAccessGate;
import org.apache.sling.api.resource.ResourceAccessGate.GateResult;
import org.apache.sling.resourceresolver.accessgate.ResourceAccessGateHandler;
import org.apache.sling.resourceresolver.accessgate.ResourceAccessGateManager;

@References( {
    @Reference(name = "ResourceAccessGateManager", referenceInterface = ResourceAccessGateManager.class, cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "setResourceAccessGateManager", unbind = "unsetResourceAccessGateManager") })

public class AccessGateResourceDecorator implements ResourceDecorator {
    
    private ResourceAccessGateManagerTracker resAccessGateManagerTracker;
    
    public AccessGateResourceDecorator ( ResourceAccessGateManagerTracker resAccessGateManagerTracker ) {
        this.resAccessGateManagerTracker = resAccessGateManagerTracker;
    }

    @Override
    public Resource decorate(Resource resource) {
        Resource returnValue = resource;
        ResourceResolver resResolver = resource.getResourceResolver();
        String user = resResolver.getUserID();
        ResourceAccessGateManager resourceAccessGateManager = resAccessGateManagerTracker.getResourceAccessGateManager();
        
        if ( resourceAccessGateManager != null ) {
            List<ResourceAccessGateHandler> accessGateHandlers =
                    resourceAccessGateManager.getMatchingResourceAccessGateHandlers( resource.getPath(), ResourceAccessGate.Operation.READ );
            
            GateResult finalGateResult = null;
            boolean canReadAllValues = false;
            List<ResourceAccessGate> accessGatesForValues = null;
            
            for (ResourceAccessGateHandler resourceAccessGateHandler : accessGateHandlers) {
                GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canRead(resource, user);
                if ( !canReadAllValues && gateResult == GateResult.GRANTED ) {
                    if ( resourceAccessGateHandler.getResourceAccessGate().canReadAllValues(resource, user) ) {
                        canReadAllValues = true;
                        accessGatesForValues = null;
                    }
                    else {
                        if ( accessGatesForValues == null ) {
                            accessGatesForValues = new ArrayList<ResourceAccessGate>();
                        }
                        accessGatesForValues.add( resourceAccessGateHandler.getResourceAccessGate() );
                    }
                }
                if ( finalGateResult == null ) {
                    finalGateResult = gateResult;
                }
                else if ( finalGateResult == GateResult.DENIED ){
                    finalGateResult = gateResult;
                }
                if ( resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.READ) ) {
                    break;
                }
            }
            
            // wrap Resource if read access is not or partly (values) not granted 
            if ( finalGateResult == GateResult.DENIED ) {
                returnValue = new NonExistingResource( resResolver, resource.getPath() );
            }
            else if ( finalGateResult == GateResult.DONTCARE ) {
                returnValue = resource;
            }
            else if ( !canReadAllValues ) {
                returnValue = new AccessGateResourceWrapper( resource, accessGatesForValues );
            }
        }
        
        return returnValue;
    }

    @Override
    public Resource decorate(Resource resource, HttpServletRequest request) {
        return decorate( resource );
    }
    
}
