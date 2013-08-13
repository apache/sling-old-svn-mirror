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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.DynamicMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.hc.api.HealthCheck;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an {@link HealthCheckMbean} for every {@link HealthCheckMBean} service
 *
 * TODO: What happens if two mbeans want to use the same object name (type and name).
 *       We need to handle this and maybe use service ranking to resolve the conflict
 * */
@Component
public class HealthCheckMBeanCreator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<ServiceReference, ServiceRegistration> registeredServices = new HashMap<ServiceReference, ServiceRegistration>();

    private ServiceTracker hcTracker;

    @Activate
    protected void activate(final BundleContext btx) {
        this.hcTracker = new ServiceTracker(btx, HealthCheck.class.getName(), new ServiceTrackerCustomizer() {

            @Override
            public synchronized void removedService(final ServiceReference reference, final Object service) {
                btx.ungetService(reference);
                unregisterHCMBean(reference);
            }

            @Override
            public synchronized void modifiedService(final ServiceReference reference, final Object service) {
                unregisterHCMBean(reference);
                registerHCMBean(btx, reference, (HealthCheck)service);
            }

            @Override
            public synchronized Object addingService(final ServiceReference reference) {
                final HealthCheck hc = (HealthCheck) btx.getService(reference);

                if ( hc != null ) {
                    registerHCMBean(btx, reference, hc);
                }
                return hc;
            }
        });
        this.hcTracker.open();
    }

    @Deactivate
    protected void deactivate() {
        if ( this.hcTracker != null ) {
            this.hcTracker.close();
            this.hcTracker = null;
        }
    }

    private void registerHCMBean(final BundleContext bundleContext, final ServiceReference ref, final HealthCheck hc) {
        final HealthCheckMBean mbean = new HealthCheckMBean(hc);

        final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
        mbeanProps.put("jmx.objectname", "org.apache.sling.healthcheck:type=" + mbean.getJmxTypeName() + ",service=" + mbean.getName());

        final ServiceRegistration reg = bundleContext.registerService(DynamicMBean.class.getName(), mbean, mbeanProps);
        registeredServices.put(ref, reg);
        log.debug("Registered {} with properties {}", mbean, mbeanProps);
    }

    private void unregisterHCMBean(final ServiceReference ref) {
        final ServiceRegistration reg = registeredServices.remove(ref);
        if ( reg != null ) {
            reg.unregister();
            log.debug("Ungegistered {}", ref);
        }
    }
}