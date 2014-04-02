package org.apache.sling.jcr.resource.internal;
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


import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support object for the different observation implementations
 */
public class ObservationListenerSupport  {

    /** Logger */
    private final Logger logger = LoggerFactory.getLogger(ObservationListenerSupport.class);

    private final ServiceTracker eventAdminTracker;

    private ServiceReference resourceResolverFactoryReference;

    /** The admin resource resolver. */
    private ResourceResolver resourceResolver;

    private final BundleContext bundleContext;

    private final Session session;

    public ObservationListenerSupport(final BundleContext bundleContext, final SlingRepository repository)
    throws RepositoryException {
        this.bundleContext = bundleContext;

        this.eventAdminTracker = new ServiceTracker(bundleContext, EventAdmin.class.getName(), null);
        this.eventAdminTracker.open();

        this.session = repository.loginAdministrative(null);
    }

    /**
     * Dispose this support object.
     */
    public void dispose() {
        if ( this.resourceResolver != null ) {
            this.resourceResolver.close();
            this.resourceResolver = null;
        }

        if ( this.resourceResolverFactoryReference != null ) {
            this.bundleContext.ungetService(this.resourceResolverFactoryReference);
            this.resourceResolverFactoryReference = null;
        }
        this.eventAdminTracker.close();

        this.session.logout();
    }

    public Session getSession() {
        return this.session;
    }

    public EventAdmin getEventAdmin() {
        return (EventAdmin) this.eventAdminTracker.getService();
    }

    /**
     * Get a resource resolver.
     * We don't need any syncing as this is called from the process OSGi thread.
     */
    public ResourceResolver getResourceResolver() {
        if ( this.resourceResolver == null ) {
            final ServiceReference ref = this.bundleContext.getServiceReference(ResourceResolverFactory.class.getName());
            if ( ref != null ) {
                final ResourceResolverFactory factory = (ResourceResolverFactory) this.bundleContext.getService(ref);
                if ( factory != null ) {
                    final Map<String, Object> authInfo = new HashMap<String, Object>();
                    authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, this.session);
                    try {
                        this.resourceResolver = factory.getResourceResolver(authInfo);
                        this.resourceResolverFactoryReference = ref;
                    } catch (final LoginException le) {
                        logger.error("Unable to get administrative resource resolver.", le);
                        this.bundleContext.ungetService(ref);
                    }
                }
            }
        }
        if ( this.resourceResolver != null ) {
            this.resourceResolver.refresh();
        }
        return this.resourceResolver;
    }
}
