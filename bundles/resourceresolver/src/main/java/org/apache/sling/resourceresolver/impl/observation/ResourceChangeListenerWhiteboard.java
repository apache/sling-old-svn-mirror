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
package org.apache.sling.resourceresolver.impl.observation;

import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.resourceresolver.impl.observation.BasicObserverConfiguration.Builder;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceListener;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

@Component(immediate = true)
@Service(ResourceChangeListenerWhiteboard.class)
@References({
        @Reference(name = "ResourceChangeListener", referenceInterface = ResourceChangeListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "ResourceResolverFactory", referenceInterface = ResourceResolverFactory.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC) })
public class ResourceChangeListenerWhiteboard {

    public static final String TOPIC_RESOURCE_CHANGE_LISTENER_UPDATE = "org/apache/sling/api/resource/ResourceChangeListener/UPDATE";

    private Map<ResourceChangeListener, ObserverConfiguration> listeners = new IdentityHashMap<ResourceChangeListener, ObserverConfiguration>();

    private Map<ResourceChangeListener, Builder> pendingListeners = new IdentityHashMap<ResourceChangeListener, Builder>();

    private String[] searchPaths;

    @Reference
    private EventAdmin eventAdmin;

    public ObservationReporter getObservationReporter() {
        return new BasicObservationReporter(listeners);
    }

    protected void bindResourceChangeListener(ResourceChangeListener listener, Map<String, Object> properties) {
        Builder builder = new Builder();
        builder.setFromProperties(properties);
        builder.setIncludeExternal(listener instanceof ExternalResourceListener);

        if (searchPaths == null) {
            pendingListeners.put(listener, builder);
        } else {
            builder.setSearchPaths(searchPaths);
            listeners.put(listener, builder.build());
            postListenersChangedEvent();
        }
    }

    protected void unbindResourceChangeListener(ResourceChangeListener listener, Map<String, Object> properties) {
        if (listeners.remove(listener) != null) {
            postListenersChangedEvent();
        }
        pendingListeners.remove(listener);
    }

    protected void bindResourceResolverFactory(ResourceResolverFactory factory) throws LoginException {
        ResourceResolver resolver = factory.getResourceResolver(null);
        try {
            this.searchPaths = resolver.getSearchPath();
            activatePendingListeners();
        } finally {
            resolver.close();
        }
    }

    protected void unbindResourceResolverFactory(ResourceResolverFactory factory) {
        this.searchPaths = null;
    }

    private void activatePendingListeners() {
        boolean added = false;
        for (Entry<ResourceChangeListener, Builder> e : pendingListeners.entrySet()) {
            Builder builder = e.getValue();
            builder.setSearchPaths(searchPaths);
            listeners.put(e.getKey(), builder.build());
            added = true;
        }
        pendingListeners.clear();
        if (added) {
            postListenersChangedEvent();
        }
    }

    private void postListenersChangedEvent() {
        eventAdmin.sendEvent(new Event(TOPIC_RESOURCE_CHANGE_LISTENER_UPDATE, new Hashtable<String, Object>()));
    }
}
