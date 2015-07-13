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
package org.apache.sling.resourceresolver.impl.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.runtime.dto.FailureReason;
import org.apache.sling.api.resource.runtime.dto.ResourceProviderDTO;
import org.apache.sling.api.resource.runtime.dto.ResourceProviderFailureDTO;
import org.apache.sling.api.resource.runtime.dto.RuntimeDTO;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = ResourceProviderTracker.class)
public class ResourceProviderTracker {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<ServiceReference, ResourceProviderInfo> infos = new ConcurrentHashMap<ServiceReference, ResourceProviderInfo>();

    private volatile BundleContext bundleContext;

    private volatile ServiceTracker tracker;

    private final Map<String, List<ResourceProviderHandler>> handlers = new HashMap<String, List<ResourceProviderHandler>>();

    private final Map<ResourceProviderInfo, FailureReason> invalidProviders = new HashMap<ResourceProviderInfo, FailureReason>();

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.tracker = new ServiceTracker(bundleContext,
                ResourceProvider.class.getName(),
                new ServiceTrackerCustomizer() {

            @Override
            public void removedService(final ServiceReference reference, final Object service) {
                final ServiceReference ref = (ServiceReference)service;
                final ResourceProviderInfo info = infos.remove(ref);
                if ( info != null ) {
                    unregister(info);
                }
            }

            @Override
            public void modifiedService(final ServiceReference reference, final Object service) {
                removedService(reference, service);
                addingService(reference);
            }

            @Override
            public Object addingService(final ServiceReference reference) {
                final ResourceProviderInfo info = new ResourceProviderInfo(reference);
                infos.put(reference, info);
                register(info);
                return reference;
            }
        });
    }

    @Deactivate
    protected void deactivate() {
        if ( this.tracker != null ) {
            this.tracker.close();
            this.tracker = null;
        }
        this.infos.clear();
    }

    private void register(final ResourceProviderInfo info) {
        if ( info.isValid() ) {
           logger.debug("Registering new resource provider {}", info);
           synchronized ( this.handlers ) {
               List<ResourceProviderHandler> infos = this.handlers.get(info.getPath());
               if ( infos == null ) {
                   infos = new ArrayList<ResourceProviderHandler>();
                   this.handlers.put(info.getPath(), infos);
               }
               final ResourceProviderHandler handler = new ResourceProviderHandler(bundleContext, info);
               infos.add(handler);
               Collections.sort(infos);
               if ( infos.get(0) == handler ) {
                   if ( !this.activate(handler) ) {
                       infos.remove(handler);
                       if ( infos.isEmpty() ) {
                           this.handlers.remove(info.getPath());
                       }
                   } else {
                       if ( infos.size() > 1 ) {
                           this.deactivate(infos.get(1));
                       }
                   }
               }
           }
        } else {
            logger.debug("Ignoring invalid resource provider {}", info);
            synchronized ( this.invalidProviders ) {
                this.invalidProviders.put(info, FailureReason.invalid);
            }
        }
    }

    private void unregister(final ResourceProviderInfo info) {
        if ( info.isValid() ) {
            logger.debug("Unregistering resource provider {}", info);
            final List<ResourceProviderHandler> infos = this.handlers.get(info.getPath());
            if ( infos != null ) {
                boolean activate = false;
                if ( infos.get(0).getInfo() == info ) {
                    activate = true;
                    this.deactivate(infos.get(0));
                }
                if ( infos.remove(info) ) {
                    if ( infos.isEmpty() ) {
                        this.handlers.remove(info.getPath());
                    } else {
                        while ( activate ) {
                            if ( !this.activate(infos.get(0)) ) {
                                infos.remove(0);
                                activate = !this.handlers.isEmpty();
                                if ( !activate ) {
                                    this.handlers.remove(info.getPath());
                                }
                            }
                        }
                    }
                }
            }

        } else {
            logger.debug("Unregistering invalid resource provider {}", info);
            synchronized ( this.invalidProviders ) {
                this.invalidProviders.remove(info);
            }
        }
    }

    private void deactivate(final ResourceProviderHandler handler) {
        handler.deactivate();
        logger.debug("Deactivated resource provider {}", handler.getInfo());
    }

    private boolean activate(final ResourceProviderHandler handler) {
        if ( handler.getResourceProvider() == null ) {
            logger.debug("Activating resource provider {} failed", handler.getInfo());
            synchronized ( this.invalidProviders ) {
                this.invalidProviders.put(handler.getInfo(), FailureReason.service_not_gettable);
            }
            return false;
        }
        logger.debug("Activated resource provider {}", handler.getInfo());
        return true;
    }

    public void fill(final RuntimeDTO dto) {
        final List<ResourceProviderDTO> dtos = new ArrayList<ResourceProviderDTO>();
        final List<ResourceProviderFailureDTO> failures = new ArrayList<ResourceProviderFailureDTO>();

        synchronized ( this.handlers ) {
            for(final List<ResourceProviderHandler> handlers : this.handlers.values()) {
                boolean isFirst = true;
                for(final ResourceProviderHandler h : handlers) {
                    final ResourceProviderDTO d;
                    if ( isFirst ) {
                        d = new ResourceProviderDTO();
                        dtos.add(d);
                        isFirst = false;
                    } else {
                        d = new ResourceProviderFailureDTO();
                        ((ResourceProviderFailureDTO)d).reason = FailureReason.shadowed;
                        failures.add((ResourceProviderFailureDTO)d);
                    }
                    fill(d, h.getInfo());
                }
            }
        }
        synchronized ( this.invalidProviders ) {
            for(final Map.Entry<ResourceProviderInfo, FailureReason> entry : this.invalidProviders.entrySet()) {
                final ResourceProviderFailureDTO d = new ResourceProviderFailureDTO();
                fill(d, entry.getKey());
                d.reason = entry.getValue();
            }
        }
        dto.providers = dtos.toArray(new ResourceProviderDTO[dtos.size()]);
        dto.failedProviders = failures.toArray(new ResourceProviderFailureDTO[failures.size()]);
    }

    private void fill(final ResourceProviderDTO d, final ResourceProviderInfo info) {
        d.authType = info.getAuthType();
        d.modifiable = info.getModifiable();
        d.name = info.getName();
        d.path = info.getPath();
        d.serviceId = (Long)info.getServiceReference().getProperty(Constants.SERVICE_ID);
        d.useResourceAccessSecurity = info.getUseResourceAccessSecurity();
    }
}
