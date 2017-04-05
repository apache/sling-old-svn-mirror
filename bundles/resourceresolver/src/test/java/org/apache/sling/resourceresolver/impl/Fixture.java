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
package org.apache.sling.resourceresolver.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The <tt>Fixture</tt> holds reusable, shared, test setup code
 */
public class Fixture { 

    private final BundleContext bc;
    private final Map<ResourceProviderInfo, ServiceRegistration> providerRegistrations = new HashMap<ResourceProviderInfo, ServiceRegistration>();
    
    public Fixture(BundleContext bc) {
        this.bc = bc;
    }

    public ResourceProviderInfo registerResourceProvider(ResourceProvider<?> rp, String root, AuthType authType) throws InvalidSyntaxException {
        
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(ResourceProvider.PROPERTY_ROOT, root);
        props.put(ResourceProvider.PROPERTY_AUTHENTICATE, authType.name());
        props.put(ResourceProvider.PROPERTY_MODIFIABLE, Boolean.TRUE.toString());
        
        ServiceRegistration registration = bc.registerService(ResourceProvider.class.getName(), rp, props);
        
        ServiceReference sr = bc.getServiceReferences(ResourceProvider.class.getName(),
                "(" + ResourceProvider.PROPERTY_ROOT + "=" + root + ")")[0];
        
        ResourceProviderInfo providerInfo = new ResourceProviderInfo(sr);

        providerRegistrations.put(providerInfo, registration);
        
        return providerInfo;
    }
    
    public void unregisterResourceProvider(ResourceProviderInfo info) {
        
        ServiceRegistration registration = providerRegistrations.remove(info);
        if ( registration == null ) {
            throw new IllegalArgumentException("No " + ServiceRegistration.class.getSimpleName() + " found for " + info);
        }
        
        registration.unregister();
    }
}