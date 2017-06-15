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
package org.apache.sling.resource.presence.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.resource.presence.ResourcePresence;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(
    ocd = ResourcePresenterConfiguration.class,
    factory = true
)
public class ResourcePresenter {

    private String path;

    private BundleContext bundleContext;

    private ServiceRegistration<ResourcePresence> presenceRegistration;

    private ServiceRegistration<ResourceChangeListener> listenerRegistration;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ServiceUserMapped serviceUserMapped;

    private final Logger logger = LoggerFactory.getLogger(ResourcePresenter.class);

    @Activate
    public void activate(final ResourcePresenterConfiguration configuration, final BundleContext bundleContext) {
        logger.info("activating resource presenter for {}", configuration.path());
        path = configuration.path();
        this.bundleContext = bundleContext;
        try (final ResourceResolver resourceResolver = getServiceResourceResolver()) {
            final Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                registerResourcePresence();
            }
            registerResourceChangeListener();
        } catch (LoginException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate() {
        logger.info("deactivating resource presenter for {}", path);
        unregisterResourceChangeListener();
        unregisterResourcePresence();
        bundleContext = null;
    }

    private ResourcePresence resourcePresence() {
        return () -> path;
    }

    private ResourceChangeListener resourceChangeListener() {
        return resourceChanges -> {
            for (final ResourceChange resourceChange : resourceChanges) {
                if (path.equals(resourceChange.getPath())) {
                    final ChangeType type = resourceChange.getType();
                    logger.info("change for {} observed: {}", path, type);
                    if (type == ChangeType.ADDED) {
                        unregisterResourcePresence();
                        registerResourcePresence();
                    } else if (type == ChangeType.REMOVED) {
                        unregisterResourcePresence();
                    }
                }
            }
        };
    }

    private void registerResourcePresence() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("path", path);
        presenceRegistration = bundleContext.registerService(ResourcePresence.class, resourcePresence(), properties);
        logger.info("resource presence for {} registered", path);
    }

    private void unregisterResourcePresence() {
        if (presenceRegistration != null) {
            presenceRegistration.unregister();
            presenceRegistration = null;
            logger.info("resource presence for {} unregistered", path);
        }
    }

    private void registerResourceChangeListener() {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ResourceChangeListener.PATHS, path);
        listenerRegistration = bundleContext.registerService(ResourceChangeListener.class, resourceChangeListener(), properties);
        logger.info("resource change listener for {} registered", path);
    }

    private void unregisterResourceChangeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.unregister();
            listenerRegistration = null;
            logger.info("resource change listener for {} unregistered", path);
        }
    }

    private ResourceResolver getServiceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(null);
    }

}
