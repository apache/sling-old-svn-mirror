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
package org.apache.sling.jcr.ocm.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.osgi.commons.AdapterFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

class OcmAdapterFactory implements AdapterFactory {

    private Dictionary<String, Object> registrationProperties;
    
    private ServiceRegistration registration;
    
    OcmAdapterFactory(BundleContext bundleContext, String[] mappedClasses) {
        
        registrationProperties = new Hashtable<String, Object>();
        registrationProperties.put(ADAPTABLE_CLASSES, Resource.class.getName());
        registrationProperties.put(ADAPTER_CLASSES, mappedClasses);
        registrationProperties.put(Constants.SERVICE_DESCRIPTION, "Sling OCM Adapter Factory");
        registrationProperties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        
        registration = bundleContext.registerService(SERVICE_NAME, this, registrationProperties);
    }
    
    void updateAdapterClasses(String[] mappedClasses) {
        // set the new set of mapped classes
        registrationProperties.put(ADAPTER_CLASSES, mappedClasses);
        
        // update the registration to have the factory registry updated
        registration.setProperties(registrationProperties);
    }
    
    void dispose() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
    
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {
        // TODO Auto-generated method stub
        return null;
    }

}
