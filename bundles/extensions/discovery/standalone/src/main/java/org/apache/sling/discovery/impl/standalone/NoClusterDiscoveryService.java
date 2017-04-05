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
package org.apache.sling.discovery.impl.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple implementation of the discovery service
 * which can be used for a cluster less installation (= single instance).
 */
@Component(immediate=true) // immediate as this is component is also handling the listeners
@Service(value = {DiscoveryService.class})
public class NoClusterDiscoveryService implements DiscoveryService {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Sling settings service to get the Sling ID and run modes.
     */
    @Reference
    private SlingSettingsService settingsService;

    /**
     * All topology event listeners.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private TopologyEventListener[] listeners = new TopologyEventListener[0];

    /**
     * All property providers.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               referenceInterface=PropertyProvider.class, updated="updatedPropertyProvider")
    private List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();

    /**
     * Special lock object to sync data structure access
     */
    private final Object lock = new Object();

    /**
     * The current topology view.
     */
    private volatile TopologyViewImpl currentTopologyView;

    private volatile Map<String, String> cachedProperties = Collections.emptyMap();

    /**
     * Activate this service
     * Create a new description.
     */
    @Activate
    protected void activate() {
        logger.debug("NoClusterDiscoveryService started.");
        createNewView(Type.TOPOLOGY_INIT, true);
    }

    /**
     * Deactivate this service.
     */
    @Deactivate
    protected void deactivate() {
        synchronized ( lock ) {
            if ( this.currentTopologyView != null ) {
                this.currentTopologyView.invalidate();
                this.currentTopologyView = null;
            }
            this.cachedProperties = null;
        }
        logger.debug("NoClusterDiscoveryService stopped.");
    }

    private void createNewView(final Type eventType, boolean inform) {
        final TopologyEventListener[] registeredServices;
        final TopologyView newView;
        final TopologyView oldView;
        synchronized ( lock ) {
            // invalidate old view
            if ( this.currentTopologyView != null ) {
                this.currentTopologyView.invalidate();
                oldView = currentTopologyView;
            } else {
                oldView = null;
            }
            final InstanceDescription myInstanceDescription = new InstanceDescriptionImpl(this.settingsService.getSlingId(),
                    this.cachedProperties);
            this.currentTopologyView = new TopologyViewImpl(myInstanceDescription);
            registeredServices = this.listeners;
            newView = this.currentTopologyView;

            if ( inform ) {
                for(final TopologyEventListener da: registeredServices) {
                    da.handleTopologyEvent(new TopologyEvent(eventType, oldView, newView));
                }
            }
        }
    }

    /**
     * Bind a new property provider.
     */
    private void bindPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
    	logger.debug("Binding PropertyProvider {}", propertyProvider);

        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.add(info);
            Collections.sort(this.providerInfos);
            this.updatePropertiesCache();
            if ( this.currentTopologyView != null ) {
                this.createNewView(Type.PROPERTIES_CHANGED, true);
            }
        }
    }

    /**
     * Update a property provider.
     */
    @SuppressWarnings("unused")
    private void updatedPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
        logger.debug("Updating PropertyProvider {}", propertyProvider);

        synchronized (lock) {
            this.unbindPropertyProvider(propertyProvider, props, false);
            this.bindPropertyProvider(propertyProvider, props);
        }
    }

    /**
     * Unbind a property provider
     */
    @SuppressWarnings("unused")
	private void unbindPropertyProvider(final PropertyProvider propertyProvider, final Map<String, Object> props) {
        this.unbindPropertyProvider(propertyProvider, props, true);
    }

    /**
     * Unbind a property provider
     */
    private void unbindPropertyProvider(final PropertyProvider propertyProvider,
            final Map<String, Object> props,
            final boolean inform) {
    	logger.debug("Releasing PropertyProvider {}", propertyProvider);

        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.remove(info);
            this.updatePropertiesCache();
            if ( this.currentTopologyView != null ) {
                this.createNewView(Type.PROPERTIES_CHANGED, inform);
            }
        }
    }

    /**
     * Update the properties cache.
     */
    private void updatePropertiesCache() {
        final Map<String, String> newProps = new HashMap<String, String>();
        for(final ProviderInfo info : this.providerInfos) {
            newProps.putAll(info.properties);
        }
        this.cachedProperties = newProps;
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("New properties: {}", this.cachedProperties);
        }
    }

    @SuppressWarnings("unused")
    private void bindTopologyEventListener(final TopologyEventListener listener) {
        logger.debug("Binding TopologyEventListener {}", listener);

        boolean inform = true;
        synchronized (lock) {
            final List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                Arrays.asList(listeners));
            currentList.add(listener);
            this.listeners = currentList.toArray(new TopologyEventListener[currentList.size()]);
            if ( this.currentTopologyView != null ) {
                listener.handleTopologyEvent(new TopologyEvent(Type.TOPOLOGY_INIT, null, this.currentTopologyView));
            }
        }
    }

    @SuppressWarnings("unused")
    private void unbindTopologyEventListener(final TopologyEventListener listener) {
        logger.debug("Releasing TopologyEventListener {}", listener);

        synchronized (lock) {
            final List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(Arrays.asList(listeners));
            currentList.remove(listener);
            this.listeners = currentList.toArray(new TopologyEventListener[currentList.size()]);
        }
    }

    /**
     * @see DiscoveryService#getTopology()
     */
    @Override
    public TopologyView getTopology() {
    	return this.currentTopologyView;
    }
}
