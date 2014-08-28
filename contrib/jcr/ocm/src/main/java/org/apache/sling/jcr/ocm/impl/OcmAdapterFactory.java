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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.NestableRuntimeException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.ocm.DefaultMappedObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

class OcmAdapterFactory implements AdapterFactory {

    private static final String CLASS_OBJECT = Object.class.getName();
    
    private final ObjectContentManagerFactoryImpl factory;
    
    private Dictionary<String, Object> registrationProperties;
    
    private ServiceRegistration registration;
    
    OcmAdapterFactory(ObjectContentManagerFactoryImpl factory, BundleContext bundleContext, String[] mappedClasses) {
        this.factory = factory;
        
        mappedClasses = ensureClassObject(mappedClasses);

        registrationProperties = new Hashtable<String, Object>();
        registrationProperties.put(ADAPTABLE_CLASSES, Resource.class.getName());
        registrationProperties.put(ADAPTER_CLASSES, mappedClasses);
        registrationProperties.put(Constants.SERVICE_DESCRIPTION, "Sling OCM Adapter Factory");
        registrationProperties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        
        registration = bundleContext.registerService(SERVICE_NAME, this, registrationProperties);
    }
    
    void updateAdapterClasses(String[] mappedClasses) {
        // set the new set of mapped classes
        mappedClasses = ensureClassObject(mappedClasses);
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
    
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {
        
        // must work, we only support Resource
        Resource res = (Resource) adaptable;
        
        // the resource must be node based, otherwise return null
        Node node = res.adaptTo(Node.class);
        if (node == null) {
            return null;
        }
        
        try {
            Session session = node.getSession();
            ObjectContentManager ocm = factory.getObjectContentManager(session);
            
            // default mapping for Object.class
            if (type.getName().equals(CLASS_OBJECT)) {
                // unchecked cast
                try {
                    return (AdapterType) ocm.getObject(res.getPath());
                } catch (JcrMappingException jme) {
                    // no default mapping, try DefaultMappedObject
                    type = (Class<AdapterType>) DefaultMappedObject.class;
                }
            }
            
            // unchecked cast
            return (AdapterType) ocm.getObject(type, res.getPath());
        } catch (RepositoryException re) {
            // TODO: should log
        } catch (NestableRuntimeException nre) {
            // TODO: should log (OCM mapping failed)
        }
        
        // fall back to no mapping
        return null;
    }

    private String[] ensureClassObject(String[] mappedClasses) {
        for (int i=0; i < mappedClasses.length; i++) {
            if (CLASS_OBJECT.equals(mappedClasses[i])) {
                return mappedClasses;
            }
        }
        
        String[] extended = new String[mappedClasses.length+1];
        System.arraycopy(mappedClasses, 0, extended, 0, mappedClasses.length);
        extended[extended.length-1] = CLASS_OBJECT;
        return extended;
    }
}
