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
package org.apache.sling.servlets.post.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.HtmlResponseProxy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>PostOperationProxyProvider</code> listens for legacy
 * {@link SlingPostOperation} services being registered and wraps them with a
 * proxy for the new {@link PostOperation} API and registers the procies.
 */
@Component(specVersion = "1.1", metatype = false)
public class PostOperationProxyProvider implements ServiceListener {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The service listener filter to listen for SlingPostOperation services
     */
    private static final String REFERENCE_FILTER = "(" + Constants.OBJECTCLASS
        + "=" + SlingPostOperation.SERVICE_NAME + ")";

    // maps references to the SlingPostOperation services to the registrations
    // of the PostOperation proxies for unregistration purposes
    private final Map<ServiceReference, ServiceRegistration> proxies = new IdentityHashMap<ServiceReference, ServiceRegistration>();

    // The DS component context to access the services to proxy
    private BundleContext bundleContext;

    // DS activation/deactivation

    /**
     * Activates the proxy provider component:
     * <ol>
     * <li>Keep BundleContext reference</li>
     * <li>Start listening for SlingPostOperation services</li>
     * <li>Register proxies for all existing SlingPostOperation services</li>
     * </ol>
     */
    @SuppressWarnings("unused")
    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        try {
            bundleContext.addServiceListener(this, REFERENCE_FILTER);
            final ServiceReference[] serviceReferences = bundleContext.getServiceReferences(
                SlingPostOperation.SERVICE_NAME, null);
            if (serviceReferences != null) {
                for (ServiceReference serviceReference : serviceReferences) {
                    register(serviceReference);
                }
            }
        } catch (InvalidSyntaxException ise) {
            // not expected for tested static filter
            // TODO:log !!
        }
    }

    /**
     * Deactivates the proxy provide component:
     * <ol>
     * <li>Unregister as a service listener</li>
     * <li>Unregister all proxies</li>
     * <li>Drop BundleContext reference</li>
     * </ol>
     */
    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate() {

        this.bundleContext.removeServiceListener(this);

        final ServiceReference[] serviceReferences;
        synchronized (this.proxies) {
            serviceReferences = this.proxies.keySet().toArray(
                new ServiceReference[this.proxies.size()]);
        }

        for (ServiceReference serviceReference : serviceReferences) {
            unregister(serviceReference);
        }

        this.bundleContext = null;
    }

    // ServiceEvent handling

    public void serviceChanged(ServiceEvent event) {

        /*
         * There is a slight chance for a race condition on deactivation where
         * the component may be deactivating and the bundle context reference
         * has been removed but the framework is still sending service events.
         * In this situation we don't want to handle the event any way and so we
         * can safely ignore it
         */
        if (this.bundleContext == null) {
            return;
        }

        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                register(event.getServiceReference());
                break;
            case ServiceEvent.MODIFIED:
                update(event.getServiceReference());
                break;
            case ServiceEvent.UNREGISTERING:
                unregister(event.getServiceReference());
                break;
        }
    }

    /**
     * Access SlingPostOperation service and register proxy.
     * <p>
     * Called by serviceChanged
     */
    private void register(final ServiceReference serviceReference) {
        final SlingPostOperation service = (SlingPostOperation) this.bundleContext.getService(serviceReference);
        final PostOperationProxy proxy = new PostOperationProxy(service);

        final BundleContext bundleContext = serviceReference.getBundle().getBundleContext();
        final Dictionary<String, Object> props = copyServiceProperties(serviceReference);
        final ServiceRegistration reg = bundleContext.registerService(
            PostOperation.SERVICE_NAME, proxy, props);

        log.debug("Registering {}", proxy);
        synchronized (this.proxies) {
            this.proxies.put(serviceReference, reg);
        }
    }

    /**
     * Update proxy service registration properties
     * <p>
     * Called by serviceChanged
     */
    private void update(final ServiceReference serviceReference) {
        final ServiceRegistration proxyRegistration;
        synchronized (this.proxies) {
            proxyRegistration = this.proxies.get(serviceReference);
        }

        if (proxyRegistration != null) {
            log.debug("Updating {}", proxyRegistration);
            proxyRegistration.setProperties(copyServiceProperties(serviceReference));
        }
    }

    /**
     * Unregister proxy and unget SlingPostOperation service
     * <p>
     * Called by serviceChanged
     */
    private void unregister(final ServiceReference serviceReference) {
        final ServiceRegistration proxyRegistration;
        synchronized (this.proxies) {
            proxyRegistration = this.proxies.remove(serviceReference);
        }

        if (proxyRegistration != null) {
            log.debug("Unregistering {}", proxyRegistration);
            this.bundleContext.ungetService(serviceReference);
            proxyRegistration.unregister();
        }
    }

    // Helpers

    /**
     * Creates a Dictionary for use as the service registration properties of
     * the PostOperation proxy.
     */
    private Dictionary<String, Object> copyServiceProperties(
            final ServiceReference serviceReference) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        for (String key : serviceReference.getPropertyKeys()) {
            props.put(key, serviceReference.getProperty(key));
        }
        props.put(PostOperation.PROP_OPERATION_NAME,
            serviceReference.getProperty(SlingPostOperation.PROP_OPERATION_NAME));
        props.put(Constants.SERVICE_DESCRIPTION, "Proxy for "
            + serviceReference);
        return props;
    }

    /**
     * The <code>PostOperationProxy</code> is the proxy implementing the
     * {@link PostOperation} service interface by calling the
     * {@link SlingPostOperation} service.
     */
    private class PostOperationProxy implements PostOperation {

        private final SlingPostOperation delegatee;

        PostOperationProxy(final SlingPostOperation delegatee) {
            this.delegatee = delegatee;
        }
        
        public String toString() {
            return getClass().getSimpleName() + " for " + delegatee.getClass().getName();
        }

        public void run(SlingHttpServletRequest request, PostResponse response,
                SlingPostProcessor[] processors) {
            HtmlResponse apiResponse = new HtmlResponseProxy(response);
            delegatee.run(request, apiResponse, processors);
        }
    }
}
