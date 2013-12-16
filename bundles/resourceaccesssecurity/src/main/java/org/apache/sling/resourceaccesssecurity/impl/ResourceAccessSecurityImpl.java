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
package org.apache.sling.resourceaccesssecurity.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate.GateResult;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@Component(name = "org.apache.sling.api.security.ResourceAccessSecurity")
@Service(value = { ResourceAccessSecurity.class })
@Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling ResourceAccessSecurity")
public class ResourceAccessSecurityImpl implements ResourceAccessSecurity {

    private ResourceAccessGateTracker resourceAccessGateTracker;

    // ---------- SCR Integration ---------------------------------------------

    /** Activates this component, called by SCR before registering as a service */
    @Activate
    protected void activate(final ComponentContext componentContext) {
        resourceAccessGateTracker = new ResourceAccessGateTracker(
                componentContext.getBundleContext());
        resourceAccessGateTracker.open();

    }

    /**
     * Deativates this component (called by SCR to take out of service)
     */
    @Deactivate
    protected void deactivate() {
        resourceAccessGateTracker.close();
    }

    private List<ResourceAccessGateHandler> getMatchingResourceAccessGateHandlers(
            final String path, final ResourceAccessGate.Operation operation) {
        //
        // TODO: maybe caching some frequent paths with read operation would be
        // a good idea
        //
        final List<ResourceAccessGateHandler> handlers = resourceAccessGateTracker.getResourceAccessGateHandlers();
        List<ResourceAccessGateHandler> returnValue = handlers;

        if (handlers.size() > 0) {
            returnValue = new ArrayList<ResourceAccessGateHandler>();

            for (ResourceAccessGateHandler resourceAccessGateHandler : handlers) {
                if (resourceAccessGateHandler.matches(path, operation)) {
                    returnValue.add(resourceAccessGateHandler);
                }
            }
        }

        return returnValue;
    }

    @Override
    public Resource getReadableResource(final Resource resource) {
        Resource returnValue = resource;
        ResourceResolver resResolver = resource.getResourceResolver();

        List<ResourceAccessGateHandler> accessGateHandlers = getMatchingResourceAccessGateHandlers(
                resource.getPath(), ResourceAccessGate.Operation.READ);

        GateResult finalGateResult = null;
        boolean canReadAllValues = false;
        List<ResourceAccessGate> accessGatesForValues = null;

        for (ResourceAccessGateHandler resourceAccessGateHandler : accessGateHandlers) {
            GateResult gateResult = resourceAccessGateHandler
                    .getResourceAccessGate().canRead(resource);
            if (!canReadAllValues && gateResult == GateResult.GRANTED) {
                if (resourceAccessGateHandler.getResourceAccessGate()
                        .canReadAllValues(resource)) {
                    canReadAllValues = true;
                    accessGatesForValues = null;
                } else {
                    if (accessGatesForValues == null) {
                        accessGatesForValues = new ArrayList<ResourceAccessGate>();
                    }
                    accessGatesForValues.add(resourceAccessGateHandler
                            .getResourceAccessGate());
                }
            }
            if (finalGateResult == null) {
                finalGateResult = gateResult;
            } else if (finalGateResult == GateResult.DENIED) {
                finalGateResult = gateResult;
            }
            if (resourceAccessGateHandler
                    .isFinalOperation(ResourceAccessGate.Operation.READ)) {
                break;
            }
        }

        // return NonExistingResource if access is denied or no
        // ResourceAccessGate is present
        if (finalGateResult == null || finalGateResult == GateResult.DENIED) {
            returnValue = new NonExistingResource(resResolver,
                    resource.getPath());
        } else if (finalGateResult == GateResult.DONTCARE) {
            returnValue = resource;
        }
        // wrap Resource if read access is not or partly (values) not granted
        else if (!canReadAllValues) {
            returnValue = new AccessGateResourceWrapper(resource,
                    accessGatesForValues);
        }

        return returnValue;
    }

    @Override
    public boolean canCreate(String absPathName,
            ResourceResolver resourceResolver) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canUpdate(Resource resource) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canDelete(Resource resource) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canExecute(Resource resource) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canReadValue(Resource resource, String valueName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canSetValue(Resource resource, String valueName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canDeleteValue(Resource resource, String valueName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String transformQuery(String query, String language,
            ResourceResolver resourceResolver) throws AccessSecurityException {
        return query;
    }

}
