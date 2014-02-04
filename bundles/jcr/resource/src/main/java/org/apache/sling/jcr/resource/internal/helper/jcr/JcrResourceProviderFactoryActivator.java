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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.JcrResourceListener;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceProviderFactoryActivator</code> activates the <code>JcrResourceProviderFactory</code> service.
 */
@Component(name = "org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProviderFactory")
@Properties({
        @Property(name = ResourceProviderFactory.PROPERTY_REQUIRED, boolValue = true),
        @Property(name = ResourceProvider.ROOTS, value = "/"),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling JCR Resource Provider Factory"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
        @Property(name = QueriableResourceProvider.LANGUAGES, value = {Query.XPATH, Query.SQL, Query.JCR_SQL2})
})
public class JcrResourceProviderFactoryActivator {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPOSITORY_REFERENCE_NAME = "repository";

    private final FactoryRegistration NO_REGISTRATION = new FactoryRegistration();

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference(name = REPOSITORY_REFERENCE_NAME, referenceInterface = SlingRepository.class)
    private volatile ServiceReference repositoryReference;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC)
    private ServiceUserMapper serviceUserMapper;

    private FactoryRegistration registration = NO_REGISTRATION;

    @Activate
    protected void activate(final ComponentContext context)
            throws RepositoryException {
        registerFactory(context);
    }

    @Deactivate
    protected void deactivate() {
        unRegisterFactory();
    }

    @SuppressWarnings("unused")
    private void bindRepository(final ServiceReference ref) {
        repositoryReference = ref;
    }

    @SuppressWarnings("unused")
    private void unbindRepository(final ServiceReference ref) {
        if (repositoryReference == ref) {
            repositoryReference = null;
        }
    }

    private void unRegisterFactory() {
        final FactoryRegistration local;
        synchronized (this) {
            if (registration != NO_REGISTRATION) {
                local = registration;
                registration = NO_REGISTRATION;
            } else {
                local = null;
            }
        }
        if (local != null) {
            local.deactivate();
        }
    }

    private void registerFactory(final ComponentContext context)
            throws RepositoryException {
        final FactoryRegistration local;
        synchronized (this) {
            if (registration == NO_REGISTRATION) {
                registration = new FactoryRegistration();
                local = registration;
            } else {
                local = null;
            }
        }
        if (local != null) {
            local.activate(context);
        }
    }

    private final class FactoryRegistration {

        ServiceRegistration serviceRegistration;

        JcrResourceListener resourceListener;

        void activate(final ComponentContext context)
            throws RepositoryException {

            final SlingRepository repository = (SlingRepository) context.locateService(REPOSITORY_REFERENCE_NAME,
                    repositoryReference);

            if (repository == null) {
                // concurrent unregistration of SlingRepository service
                // don't care, this component is going to be deactivated
                // so we just stop working
                log.warn("activate: Activation failed because SlingRepository may have been unregistered concurrently");
                throw new RepositoryException("Failed to locate repository");
            }

            final Dictionary properties = clone(context.getProperties());

            final ServiceFactory serviceFactory = new ServiceFactory() {
                public ResourceProviderFactory getService(Bundle bundle, ServiceRegistration serviceRegistration) {
                    return new JcrResourceProviderFactory(dynamicClassLoaderManager,
                            repositoryReference, repository, serviceUserMapper, bundle);
                }
                public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o) {
                    // nothing to do
                }
            };

            serviceRegistration = context.getBundleContext().registerService(ResourceProviderFactory.class.getName(),
                    serviceFactory, properties);

            final String root = PropertiesUtil.toString(context.getProperties().get(ResourceProvider.ROOTS), "/");
            resourceListener = new JcrResourceListener(root, null, repository, context.getBundleContext());
        }

        void deactivate() {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
            if (resourceListener != null) {
                resourceListener.deactivate();
            }
        }

        Dictionary<String, Object> clone(Dictionary<?, ?> properties) {
            final Hashtable<String, Object> clone = new Hashtable<String, Object>();
            for (Enumeration keys = properties.keys() ; keys.hasMoreElements() ; ) {
                String key = keys.nextElement().toString();
                clone.put(key, properties.get(key));
            }
            return clone;
        }
    }
}
