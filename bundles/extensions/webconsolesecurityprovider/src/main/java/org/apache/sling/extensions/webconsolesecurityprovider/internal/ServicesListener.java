package org.apache.sling.extensions.webconsolesecurityprovider.internal;
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


import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>ServicesListener</code> listens for the required services
 * and registers the security provider when required services are available
 */
public class ServicesListener {

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the repository. */
    private final Listener repositoryListener;

    /** The listener for the authentication support. */
    private final Listener authSupportListener;

    private final SlingWebConsoleSecurityProvider provider = new SlingWebConsoleSecurityProvider();

    private final SlingWebConsoleSecurityProvider2 provider2 = new SlingWebConsoleSecurityProvider2();

    private enum State {
        NONE,
        PROVIDER,
        PROVIDER2
    };

    /** State */
    private volatile State registrationState = State.NONE;

    /** The registration for the provider */
    private ServiceRegistration providerReg;

    /** The registration for the provider2 */
    private ServiceRegistration provider2Reg;


    /**
     * Start listeners
     */
    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.authSupportListener = new Listener(AuthenticationSupport.class.getName());
        this.repositoryListener = new Listener(Repository.class.getName());
        this.authSupportListener.start();
        this.repositoryListener.start();
    }

    /**
     * Notify of service changes from the listeners.
     */
    public synchronized void notifyChange() {
        // check if all services are available
        final AuthenticationSupport authSupport = (AuthenticationSupport)this.authSupportListener.getService();
        final Repository repository = (Repository)this.repositoryListener.getService();
        if ( registrationState == State.NONE ) {
            if ( authSupport != null ) {
                registerProvider2(authSupport);
            } else if ( repository != null ) {
                registerProvider(repository);
            }
        } else if ( registrationState == State.PROVIDER ) {
            if ( authSupport != null ) {
                registerProvider2(authSupport);
                unregisterProvider();
            } else if ( repository == null ) {
                unregisterProvider();
                this.registrationState = State.NONE;
            } else {
                this.provider.setService(repository);
            }
        } else {
            if ( authSupport == null ) {
                if ( repository != null ) {
                    registerProvider(repository);
                } else {
                    this.registrationState = State.NONE;
                }
                unregisterProvider2();
            } else {
                this.provider2.setService(authSupport);
            }
        }
    }

    private void unregisterProvider2() {
        if ( this.provider2Reg != null ) {
            this.provider2Reg.unregister();
            this.provider2Reg = null;
        }
        this.provider2.setService(null);
    }

    private void unregisterProvider() {
        if ( this.providerReg != null ) {
            this.providerReg.unregister();
            this.providerReg = null;
        }
        this.provider.setService(null);
    }

    private void registerProvider2(final AuthenticationSupport authSupport) {
        this.provider2.setService(authSupport);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, SlingWebConsoleSecurityProvider.class.getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Web Console Security Provider 2");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.provider2Reg = this.bundleContext.registerService(
            new String[] {ManagedService.class.getName(), WebConsoleSecurityProvider.class.getName()}, this.provider2, props);
        this.registrationState = State.PROVIDER2;
    }

    private void registerProvider(final Repository repository) {
        this.provider.setService(repository);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, SlingWebConsoleSecurityProvider.class.getName());
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Web Console Security Provider");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.providerReg = this.bundleContext.registerService(
            new String[] {ManagedService.class.getName(), WebConsoleSecurityProvider.class.getName()}, this.provider, props);
        this.registrationState = State.PROVIDER;
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        this.repositoryListener.deactivate();
        this.authSupportListener.deactivate();
        this.unregisterProvider();
        this.unregisterProvider2();
    }

    /**
     * Helper class listening for service events for a defined service.
     */
    protected final class Listener implements ServiceListener {

        /** The name of the service. */
        private final String serviceName;

        /** The service reference. */
        private volatile ServiceReference reference;

        /** The service. */
        private volatile Object service;

        /**
         * Constructor
         */
        public Listener(final String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * Start the listener.
         * First register a service listener and then check for the service.
         */
        public void start() {
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + serviceName + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
            final ServiceReference ref = bundleContext.getServiceReference(serviceName);
            if ( ref != null ) {
                this.retainService(ref);
            }
        }

        /**
         * Unregister the listener.
         */
        public void deactivate() {
            bundleContext.removeServiceListener(this);
        }

        /**
         * Return the service (if available)
         */
        public synchronized Object getService() {
            return this.service;
        }

        /**
         * Try to get the service and notify the change.
         */
        private synchronized void retainService(final ServiceReference ref) {
            boolean hadService = this.service != null;
            boolean getService = this.reference == null;
            if ( !getService ) {
                final int result = this.reference.compareTo(ref);
                if ( result < 0 ) {
                    bundleContext.ungetService(this.reference);
                    this.service = null;
                    getService = true;
                }
            }
            if ( getService ) {
                this.reference = ref;
                this.service = bundleContext.getService(this.reference);
                if ( this.service == null ) {
                    this.reference = null;
                } else {
                    notifyChange();
                }
            }
            if ( hadService && this.service == null ) {
                notifyChange();
            }
        }

        /**
         * Try to release the service and notify the change.
         */
        private synchronized void releaseService(final ServiceReference ref) {
            if ( this.reference != null && this.reference.compareTo(ref) == 0) {
                this.service = null;
                bundleContext.ungetService(this.reference);
                this.reference = null;
                notifyChange();
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        public void serviceChanged(final ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                this.retainService(event.getServiceReference());
            } else if ( event.getType() == ServiceEvent.UNREGISTERING ) {
                this.releaseService(event.getServiceReference());
            } else if ( event.getType() == ServiceEvent.MODIFIED ) {
                notifyChange();
            }
        }
    }
}
