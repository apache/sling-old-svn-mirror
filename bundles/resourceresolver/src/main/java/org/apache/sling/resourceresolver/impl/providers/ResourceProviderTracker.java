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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.api.resource.runtime.dto.FailureReason;
import org.apache.sling.api.resource.runtime.dto.ResourceProviderDTO;
import org.apache.sling.api.resource.runtime.dto.ResourceProviderFailureDTO;
import org.apache.sling.api.resource.runtime.dto.RuntimeDTO;
import org.apache.sling.resourceresolver.impl.legacy.LegacyResourceProviderWhiteboard;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service keeps track of all resource providers.
 */
public class ResourceProviderTracker implements ResourceProviderStorageProvider {

    public interface ObservationReporterGenerator {

        ObservationReporter create(final Path path, final PathSet excludes);

        ObservationReporter createProviderReporter();
    }

    public interface ChangeListener {

        void providerAdded();

        void providerRemoved(String pid, boolean stateful, boolean used);
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<ServiceReference, ResourceProviderInfo> infos = new ConcurrentHashMap<ServiceReference, ResourceProviderInfo>();

    private volatile BundleContext bundleContext;

    private volatile ServiceTracker tracker;

    private final Map<String, List<ResourceProviderHandler>> handlers = new HashMap<String, List<ResourceProviderHandler>>();

    private final Map<ResourceProviderInfo, FailureReason> invalidProviders = new ConcurrentHashMap<ResourceProviderInfo, FailureReason>();

    private volatile EventAdmin eventAdmin;

    private volatile ObservationReporterGenerator reporterGenerator;

    private volatile ResourceProviderStorage storage;

    private volatile ObservationReporter providerReporter;

    private volatile ChangeListener listener;

    public void activate(final BundleContext bundleContext, final EventAdmin eventAdmin, final ChangeListener listener) {
        this.bundleContext = bundleContext;
        this.eventAdmin = eventAdmin;
        this.listener = listener;
        this.tracker = new ServiceTracker(bundleContext,
                ResourceProvider.class.getName(),
                new ServiceTrackerCustomizer() {

            @Override
            public void removedService(final ServiceReference reference, final Object service) {
                final ServiceReference ref = (ServiceReference)service;
                final ResourceProviderInfo info = infos.remove(ref);
                if ( info != null ) {
                    unregister(info, (String)ref.getProperty(Constants.SERVICE_PID));
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
        this.tracker.open();
    }

    public void deactivate() {
        this.listener = null;
        this.eventAdmin = null;
        this.providerReporter = null;
        if ( this.tracker != null ) {
            this.tracker.close();
            this.tracker = null;
        }
        this.infos.clear();
        this.handlers.clear();
        this.invalidProviders.clear();
    }

    public void setObservationReporterGenerator(final ObservationReporterGenerator generator) {
        this.providerReporter = generator.createProviderReporter();
        synchronized ( this.handlers ) {
            this.reporterGenerator = generator;
            for (List<ResourceProviderHandler> list : handlers.values()) {
                final ResourceProviderHandler h  = list.get(0);
                if (h != null) {
                    updateProviderContext(h);
                    h.update();
                }
            }
        }
    }

    /**
     * Try to register a new resource provider.
     * @param info The resource provider info.
     */
    private void register(final ResourceProviderInfo info) {
        if ( info.isValid() ) {
           logger.debug("Registering new resource provider {}", info);
           final List<ProviderEvent> events = new ArrayList<ResourceProviderTracker.ProviderEvent>();
           boolean providerAdded = false;
           ResourceProviderHandler deactivateHandler = null;
           long deactivateHandlerCount = 0;

           synchronized ( this.handlers ) {
               List<ResourceProviderHandler> matchingHandlers = this.handlers.get(info.getPath());
               if ( matchingHandlers == null ) {
                   matchingHandlers = new ArrayList<ResourceProviderHandler>();
                   this.handlers.put(info.getPath(), matchingHandlers);
               }
               final ResourceProviderHandler handler = new ResourceProviderHandler(bundleContext, info);
               matchingHandlers.add(handler);
               Collections.sort(matchingHandlers);
               if ( matchingHandlers.get(0) == handler ) {
                   if ( !this.activate(handler) ) {
                       matchingHandlers.remove(handler);
                       if ( matchingHandlers.isEmpty() ) {
                           this.handlers.remove(info.getPath());
                       }
                   } else {
                       providerAdded = true;
                       events.add(new ProviderEvent(true, info));
                       if ( matchingHandlers.size() > 1 ) {
                           deactivateHandler = matchingHandlers.get(1);
                           deactivateHandlerCount = deactivateHandler.getActivationCount();
                       }
                       storage = null;
                   }
               }
           }
           final ChangeListener cl = this.listener;
           if ( providerAdded && cl != null ) {
               cl.providerAdded();
           }
           // we have to check for deactivated handlers
           if ( deactivateHandler != null ) {
               final ResourceProviderInfo handlerInfo = deactivateHandler.getInfo();
               if ( cl != null ) {
                   cl.providerRemoved((String)handlerInfo.getServiceReference().getProperty(Constants.SERVICE_PID),
                               handlerInfo.getAuthType() != AuthType.no,
                                       deactivateHandler.isUsed());
               }
               synchronized ( this.handlers ) {
                   if ( deactivateHandlerCount == deactivateHandler.getActivationCount() ) {
                       this.deactivate(deactivateHandler);
                       events.add(new ProviderEvent(false, handlerInfo));
                       storage = null;
                   }
               }
           }
           this.postEvents(events);
        } else {
            logger.warn("Ignoring invalid resource provider {}", info);
            this.invalidProviders.put(info, FailureReason.invalid);
        }
    }

    /**
     * Unregister a resource provider.
     * @param info The resource provider info.
     */
    private void unregister(final ResourceProviderInfo info, final String pid) {
        final boolean isInvalid;
        synchronized ( this.invalidProviders ) {
            isInvalid = this.invalidProviders.remove(info) != null;
        }

        if ( !isInvalid ) {
            logger.debug("Unregistering resource provider {}", info);
            final List<ProviderEvent> events = new ArrayList<ResourceProviderTracker.ProviderEvent>();
            ResourceProviderHandler deactivateHandler = null;
            synchronized (this.handlers) {
                final List<ResourceProviderHandler> matchingHandlers = this.handlers.get(info.getPath());
                if ( matchingHandlers != null ) {
                    final ResourceProviderHandler firstHandler = matchingHandlers.get(0);
                    if ( firstHandler.getInfo() == info ) {
                        deactivateHandler = firstHandler;

                    }
                }
            }
            if ( deactivateHandler != null ) {
                final ChangeListener cl = this.listener;
                if ( cl != null ) {
                    cl.providerRemoved(pid,
                            info.getAuthType() != AuthType.no,
                                    deactivateHandler.isUsed());
                }
                boolean providerAdded = false;
                synchronized ( this.handlers ) {
                    this.deactivate(deactivateHandler);
                    storage = null;

                    final List<ResourceProviderHandler> matchingHandlers = this.handlers.get(info.getPath());
                    final ResourceProviderHandler firstHandler = matchingHandlers.get(0);
                    boolean doActivateNext = firstHandler.getInfo() == info;

                    if (removeHandlerByInfo(info, matchingHandlers)) {
                        while (doActivateNext && !matchingHandlers.isEmpty()) {
                            if (this.activate(matchingHandlers.get(0))) {
                                doActivateNext = false;
                                events.add(new ProviderEvent(true, matchingHandlers.get(0).getInfo()));
                                providerAdded = true;
                            } else {
                                matchingHandlers.remove(0);
                            }
                        }
                    }
                    if (matchingHandlers.isEmpty()) {
                        this.handlers.remove(info.getPath());
                    }
                }
                if ( providerAdded && cl != null ) {
                    if ( cl != null ) {
                        cl.providerAdded();
                    }
                }
                events.add(new ProviderEvent(false,info));

            }
            this.postEvents(events);
        } else {
            logger.debug("Unregistering invalid resource provider {}", info);
        }
    }

    /**
     * Search the info in the list of handlers.
     * @param info The provider info
     * @param infos The list of handlers
     * @return {@code true} if the info got removed.
     */
    private boolean removeHandlerByInfo(final ResourceProviderInfo info, final List<ResourceProviderHandler> infos) {
        Iterator<ResourceProviderHandler> it = infos.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            final ResourceProviderHandler h = it.next();
            if (h.getInfo() == info) {
                it.remove();
                h.dispose();
                removed = true;
                break;
            }
        }
        return removed;
    }

    /**
     * Activate a resource provider
     * @param handler The provider handler
     */
    private boolean activate(final ResourceProviderHandler handler) {
        updateProviderContext(handler);
        if ( !handler.activate() ) {
            logger.warn("Activating resource provider {} failed", handler.getInfo());
            this.invalidProviders.put(handler.getInfo(), FailureReason.service_not_gettable);

            return false;
        }
        logger.debug("Activated resource provider {}", handler.getInfo());
        return true;
    }

    /**
     * Deactivate a resource provider
     * @param handler The provider handler
     */
    private void deactivate(final ResourceProviderHandler handler) {
        handler.deactivate();
        logger.debug("Deactivated resource provider {}", handler.getInfo());
    }

    /**
     * Post a change event through the event admin
     * @param event
     */
    private void postOSGiEvent(final ProviderEvent event) {
        final EventAdmin ea = this.eventAdmin;
        if ( ea != null ) {
            final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
            eventProps.put(SlingConstants.PROPERTY_PATH, event.path);
            if (event.pid != null) {
                eventProps.put(Constants.SERVICE_PID, event.pid);
            }
            ea.postEvent(new Event(event.isAdd ? SlingConstants.TOPIC_RESOURCE_PROVIDER_ADDED : SlingConstants.TOPIC_RESOURCE_PROVIDER_REMOVED,
                    eventProps));
        }
    }

    /**
     * Post a change event for a resource provider change
     * @param type The change type
     * @param info The resource provider
     */
    private void postResourceProviderChange(final ProviderEvent event) {
        final ObservationReporter or = this.providerReporter;
        if ( or != null ) {
            final ResourceChange change = new ResourceChange(event.isAdd ? ChangeType.PROVIDER_ADDED : ChangeType.PROVIDER_REMOVED,
                    event.path, false, null, null, null);
            or.reportChanges(Collections.singletonList(change), false);
        }
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
                    fill(d, h);
                }
            }
        }
        synchronized ( this.invalidProviders ) {
            for(final Map.Entry<ResourceProviderInfo, FailureReason> entry : this.invalidProviders.entrySet()) {
                final ResourceProviderFailureDTO d = new ResourceProviderFailureDTO();
                fill(d, entry.getKey());
                d.reason = entry.getValue();
                failures.add(d);
            }
        }
        dto.providers = dtos.toArray(new ResourceProviderDTO[dtos.size()]);
        dto.failedProviders = failures.toArray(new ResourceProviderFailureDTO[failures.size()]);
    }

    @Override
    public ResourceProviderStorage getResourceProviderStorage() {
        ResourceProviderStorage result = storage;
        if (result == null) {
            synchronized (this.handlers) {
                if (storage == null) {
                    final List<ResourceProviderHandler> handlerList = new ArrayList<ResourceProviderHandler>();
                    for (List<ResourceProviderHandler> list : handlers.values()) {
                        ResourceProviderHandler h  = list.get(0);
                        if (h != null) {
                            handlerList.add(h);
                        }
                    }
                    storage = new ResourceProviderStorage(handlerList);
                }
                result = storage;
            }
        }
        return result;
    }

    private void fill(final ResourceProviderDTO d, final ResourceProviderInfo info) {
        d.adaptable = info.isAdaptable();
        d.attributable = info.isAttributable();
        d.authType = info.getAuthType();
        d.modifiable = info.isModifiable();
        d.name = info.getName();
        d.path = info.getPath();
        d.refreshable = info.isRefreshable();
        d.serviceId = (Long)info.getServiceReference().getProperty(Constants.SERVICE_ID);
        d.supportsQueryLanguage = false;
        d.useResourceAccessSecurity = info.getUseResourceAccessSecurity();
    }

    private void fill(final ResourceProviderDTO d, final ResourceProviderHandler handler) {
        fill(d, handler.getInfo());
        final ResourceProvider<?> provider = handler.getResourceProvider();
        if ( provider != null ) {
            d.supportsQueryLanguage = provider.getQueryLanguageProvider() != null;
        }
    }

    private void updateProviderContext(final ResourceProviderHandler handler) {
        final Set<String> excludedPaths = new HashSet<String>();
        final Path handlerPath = new Path(handler.getPath());
        for(final String otherPath : handlers.keySet()) {
            if ( !handler.getPath().equals(otherPath) && handlerPath.matches(otherPath) ) {
                excludedPaths.add(otherPath);
            }
        }
        final PathSet excludedPathSet = PathSet.fromStringCollection(excludedPaths);
        handler.getProviderContext().update(
                reporterGenerator.create(handlerPath, excludedPathSet),
                excludedPathSet);
    }

    private void postEvents(final List<ProviderEvent> events) {
        if ( events.isEmpty() ) {
            return;
        }
        if ( this.listener == null && this.providerReporter == null ) {
            return;
        }
        final Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                for(final ProviderEvent e : events) {
                    postOSGiEvent(e);
                    postResourceProviderChange(e);
                }
            }
        });
        t.setName("Apache Sling Resource Provider Change Notifier");
        t.setDaemon(true);

        t.start();
    }

    private static final class ProviderEvent {
        public final boolean isAdd;
        public final String pid;
        public final String path;

        public ProviderEvent(final boolean isAdd, final ResourceProviderInfo info) {
            this.isAdd = isAdd;
            this.path = info.getPath();
            String pid = (String) info.getServiceReference().getProperty(Constants.SERVICE_PID);
            if (pid == null) {
                pid = (String) info.getServiceReference().getProperty(LegacyResourceProviderWhiteboard.ORIGINAL_SERVICE_PID);
            }
            this.pid = pid;
        }
    }
}
