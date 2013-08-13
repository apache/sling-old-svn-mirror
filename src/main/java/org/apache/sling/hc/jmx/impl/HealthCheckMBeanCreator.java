/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.jmx.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.hc.api.HealthCheck;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates an {@link HealthCheckMbean} for every {@link HealthCheckMBean} service */
@Component
@Reference(
        name="HealthCheck",
        referenceInterface=HealthCheck.class, 
        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, 
        policy=ReferencePolicy.DYNAMIC)
public class HealthCheckMBeanCreator {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<ServiceReference, ServiceRegistration> registeredServices;
    private BundleContext bundleContext;
    private List<ServiceReference> boundRefs = new ArrayList<ServiceReference>();
    
    @Activate
    protected void activate(ComponentContext ctx) {
        registeredServices = new HashMap<ServiceReference, ServiceRegistration>();
        
        final List<ServiceReference> toAdd = new ArrayList<ServiceReference>();
        if(!boundRefs.isEmpty()) {
            synchronized (boundRefs) {
                toAdd.addAll(boundRefs);
                boundRefs.clear();
            }
        }
        
        bundleContext = ctx.getBundleContext();
        for(ServiceReference ref : toAdd) {
            processHealthCheckRef(ref);
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        for(ServiceRegistration r : registeredServices.values()) {
            r.unregister();
        }
        registeredServices = null;
    }
    
    protected void bindHealthCheck(ServiceReference ref) {
        if(bundleContext == null) {
            // Not sure why the bundle context can still be null here, is this
            // called before activate??
            synchronized (boundRefs) {
                boundRefs.add(ref);
            }
        } else {
            processHealthCheckRef(ref);
        }
    }
    
    private void processHealthCheckRef(ServiceReference ref) {
        final HealthCheck hc = (HealthCheck)bundleContext.getService(ref);
        final HealthCheckMBean mbean = new HealthCheckMBean(hc);
        
        final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
        mbeanProps.put("jmx.objectname", "org.apache.sling.healthcheck:type=" + mbean.getJmxTypeName() + ",service=" + mbean.getName());
        final ServiceRegistration reg = bundleContext.registerService(DynamicMBean.class.getName(), mbean, mbeanProps);
        registeredServices.put(ref, reg);
        log.debug("Registered {} with properties {}", mbean, mbeanProps);
    }
    
    protected void unbindHealthCheck(ServiceReference ref) {
        if(registeredServices == null) {
            log.debug("No registeredServices, nothing to unregister");
            return;
        }
        final ServiceRegistration reg = registeredServices.remove(ref);
        if(reg == null) {
            log.warn("ServiceRegistration not found for {}", ref);
        } else {
            reg.unregister();
            log.debug("Ungegistered {}", reg);
        }
    }
}