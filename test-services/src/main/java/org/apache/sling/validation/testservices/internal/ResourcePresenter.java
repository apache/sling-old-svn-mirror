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
package org.apache.sling.validation.testservices.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.validation.testservices.ResourcePresence;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ResourceChangeListener.class,
    immediate = true,
    property = {
        "resource.paths=/apps/sling/validation"
    }
)
public class ResourcePresenter implements ResourceChangeListener {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ServiceRegistration<ResourcePresence> model;

    private DefaultResourcePresence resourcePresence = new DefaultResourcePresence("/apps/sling/validation/models/model1");

    private BundleContext bundleContext;

    private final Logger logger = LoggerFactory.getLogger(ResourcePresenter.class);

    @Activate
    public void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        try (final ResourceResolver resourceResolver = getServiceResourceResolver()) {
            final Resource validation = resourceResolver.getResource("/apps/sling/validation/models/model1");
            if (validation != null) {
                registerResourcePresence();
            }
        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate(final BundleContext bundleContext) {
        unregisterResourcePresence();
    }

    @Override
    public void onChange(@Nonnull List<ResourceChange> list) {
        for (ResourceChange resourceChange : list) {
            logger.info("resource change at {}: {}", resourceChange.getPath(), resourceChange.getType());
            if (resourcePresence.getPath().equals(resourceChange.getPath())) {
                switch (resourceChange.getType()) {
                    case ADDED:
                        registerResourcePresence();
                        break;
                    case REMOVED:
                        unregisterResourcePresence();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private ResourceResolver getServiceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(null);
    }

    private void registerResourcePresence() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("path", resourcePresence.getPath());
        this.model = bundleContext.registerService(ResourcePresence.class, resourcePresence, properties);
        logger.info("resource presence for {} registered", resourcePresence.getPath());
    }

    private void unregisterResourcePresence() {
        if (model != null) {
            model.unregister();
            logger.info("resource presence for {} unregistered", resourcePresence.getPath());
        }
    }

    private class DefaultResourcePresence implements ResourcePresence {

        private final String path;

        DefaultResourcePresence(final String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

}
