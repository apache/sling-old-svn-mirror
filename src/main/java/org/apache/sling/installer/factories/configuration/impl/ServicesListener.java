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
package org.apache.sling.installer.factories.configuration.impl;

import java.util.Hashtable;

import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and starts/stops the scanners based on the availability of the
 * services.
 */
public class ServicesListener {

    /** Vendor of all registered services. */
    public static final String VENDOR = "The Apache Software Foundation";

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the change list handler. */
    private final Listener changeHandlerListener;

    /** The listener for the configuration admin. */
    private final Listener configAdminListener;

    /** Registration the service. */
    private ServiceRegistration configTaskCreatorRegistration;

    private ConfigTaskCreator configTaskCreator;

    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.changeHandlerListener = new Listener(ResourceChangeListener.class.getName());
        this.configAdminListener = new Listener(ConfigurationAdmin.class.getName());
        this.changeHandlerListener.start();
        this.configAdminListener.start();
    }

    public synchronized void notifyChange() {
        // check if all services are available
        final ResourceChangeListener listener = (ResourceChangeListener)this.changeHandlerListener.getService();
        final ConfigurationAdmin configAdmin = (ConfigurationAdmin)this.configAdminListener.getService();

        if ( configAdmin != null && listener != null ) {
            if ( configTaskCreator == null ) {
                final Hashtable<String, String> props = new Hashtable<String, String>();
                props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Configuration Install Task Factory");
                props.put(Constants.SERVICE_VENDOR, VENDOR);

                this.configTaskCreator = new ConfigTaskCreator(listener, configAdmin);
                // start and register osgi installer service
                final String [] serviceInterfaces = {
                        InstallTaskFactory.class.getName(),
                        ConfigurationListener.class.getName(),
                        ResourceTransformer.class.getName()
                };
                configTaskCreatorRegistration = this.bundleContext.registerService(serviceInterfaces, configTaskCreator, props);
            }
        } else {
            this.stop();
        }
    }

    private void stop() {
        // unregister
        if ( this.configTaskCreatorRegistration != null ) {
            this.configTaskCreatorRegistration.unregister();
            this.configTaskCreatorRegistration = null;
        }
        this.configTaskCreator = null;
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        this.changeHandlerListener.deactivate();
        this.configAdminListener.deactivate();
        this.stop();
    }

    protected final class Listener implements ServiceListener {

        private final String serviceName;

        private ServiceReference reference;
        private Object service;

        public Listener(final String serviceName) {
            this.serviceName = serviceName;
        }

        public void start() {
            this.retainService();
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + serviceName + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
        }

        public void deactivate() {
            bundleContext.removeServiceListener(this);
        }

        public synchronized Object getService() {
            return this.service;
        }
        private synchronized void retainService() {
            if ( this.reference == null ) {
                this.reference = bundleContext.getServiceReference(this.serviceName);
                if ( this.reference != null ) {
                    this.service = bundleContext.getService(this.reference);
                    if ( this.service == null ) {
                        this.reference = null;
                    } else {
                        notifyChange();
                    }
                }
            }
        }

        private synchronized void releaseService() {
            if ( this.reference != null ) {
                this.service = null;
                bundleContext.ungetService(this.reference);
                this.reference = null;
                notifyChange();
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED ) {
                this.retainService();
            } else if ( event.getType() == ServiceEvent.UNREGISTERING ) {
                this.releaseService();
            }
        }
    }
}
