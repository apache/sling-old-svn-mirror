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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an {@link HealthCheckMbean} for every {@link HealthCheckMBean} service
 *
 */
@Component
public class HealthCheckMBeanCreator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<ServiceReference, Registration> registeredServices = new HashMap<ServiceReference, Registration>();

    private final Map<String, List<ServiceReference>> sortedRegistrations = new HashMap<String, List<ServiceReference>>();

    private ServiceTracker hcTracker;

    @Activate
    protected void activate(final BundleContext btx) {
        this.hcTracker = new ServiceTracker(btx, HealthCheck.class.getName(), null) {

            @Override
            public Object addingService(final ServiceReference reference) {
                return registerHCMBean(btx, reference);
            }

            @Override
            public void modifiedService(final ServiceReference reference,
                    final Object service) {
                unregisterHCMBean(btx, reference);
                registerHCMBean(btx, reference);
            }

            @Override
            public void removedService(final ServiceReference reference,
                    final Object service) {
                unregisterHCMBean(btx, reference);
            }

        };
        this.hcTracker.open();
    }

    @Deactivate
    protected void deactivate() {
        if ( this.hcTracker != null ) {
            this.hcTracker.close();
            this.hcTracker = null;
        }
    }

    /**
     * Register an mbean for a health check service.
     * The mbean is only registered if
     * - the service has an mbean registration property
     * - if there is no other service with the same name but a higher service ranking
     *
     * @param bundleContext The bundle context
     * @param reference     The service reference to the health check service
     * @return The registered mbean or <code>null</code>
     */
    private synchronized Object registerHCMBean(final BundleContext bundleContext, final ServiceReference reference) {
        final Registration reg = Registration.getRegistration(bundleContext, reference);
        if ( reg != null ) {
            this.registeredServices.put(reference, reg);

            List<ServiceReference> registered = this.sortedRegistrations.get(reg.name);
            if ( registered == null ) {
                registered = new ArrayList<ServiceReference>();
                this.sortedRegistrations.put(reg.name, registered);
            }
            registered.add(reference);
            // sort orders the references with lowest ranking first
            // we want the highest!
            Collections.sort(registered);
            final int lastIndex = registered.size() - 1;
            if ( registered.get(lastIndex).equals(reference) ) {
                if ( registered.size() > 1 ) {
                    final ServiceReference prevRef = registered.get(lastIndex - 1);
                    final Registration prevReg = this.registeredServices.get(prevRef);
                    prevReg.unregister(this.logger);
                }
                reg.register(this.logger, bundleContext);
            }
        }
        return reg;
    }

    private synchronized void unregisterHCMBean(final BundleContext bundleContext, final ServiceReference ref) {
        final Registration reg = registeredServices.remove(ref);
        if ( reg != null ) {
            final boolean registerFirst = reg.unregister(this.logger);
            final List<ServiceReference> registered = this.sortedRegistrations.get(reg.name);
            registered.remove(ref);
            if ( registered.size() == 0 ) {
                this.sortedRegistrations.remove(reg.name);
            } else if ( registerFirst ) {
                final ServiceReference newRef = registered.get(0);
                final Registration newReg = this.registeredServices.get(newRef);
                newReg.register(this.logger, bundleContext);
            }
            bundleContext.ungetService(ref);
        }
    }

    private static final class Registration {
        public final String name;
        public final HealthCheckMBean mbean;
        private ServiceRegistration registration;

        public Registration(final String name, final HealthCheckMBean mbean) {
            this.name = name;
            this.mbean = mbean;
        }

        public static Registration getRegistration(final BundleContext bundleContext, final ServiceReference ref) {
            final Object nameObj = ref.getProperty(HealthCheck.MBEAN_NAME);
            if ( nameObj != null ) {
                final HealthCheck service = (HealthCheck) bundleContext.getService(ref);
                if ( service != null ) {
                    final HealthCheckMBean mbean = new HealthCheckMBean(ref, service);

                    return new Registration(nameObj.toString().replace(',', '.'), mbean);
                }
            }
            return null;
        }

        public void register(final Logger logger, final BundleContext btx) {
            final StringBuilder sb = new StringBuilder(HealthCheckMBean.JMX_DOMAIN);
            sb.append(":type=");
            sb.append(HealthCheckMBean.JMX_TYPE_NAME);
            sb.append(",name=");
            sb.append(this.name);
            final String objectName = sb.toString();

            final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
            mbeanProps.put("jmx.objectname", objectName);
            this.registration = btx.registerService(DynamicMBean.class.getName(), this.mbean, mbeanProps);

            logger.debug("Registered health check mbean {} as {}", this.mbean, objectName);
        }

        public boolean unregister(final Logger logger) {
            if ( this.registration != null ) {
                this.registration.unregister();
                this.registration = null;
                logger.debug("Unregistered health check mbean {}", this.mbean);
                return true;
            }
            return false;
        }
    }
}
