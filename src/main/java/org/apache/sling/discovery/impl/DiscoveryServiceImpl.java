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
package org.apache.sling.discovery.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.apache.sling.discovery.impl.topology.TopologyViewImpl;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation.OriginInfo;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the cross-cluster service uses the view manager
 * implementation for detecting changes in a cluster and only supports one
 * cluster (of which this instance is part of).
 */
@Component(immediate = true)
@Service(value = { DiscoveryService.class, DiscoveryServiceImpl.class })
public class DiscoveryServiceImpl implements DiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService settingsService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, referenceInterface = TopologyEventListener.class)
    private TopologyEventListener[] eventListeners = new TopologyEventListener[0];

    /**
     * All property providers.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, referenceInterface = PropertyProvider.class, updated = "updatedPropertyProvider")
    private List<ProviderInfo> providerInfos = new ArrayList<ProviderInfo>();

    /** lock object used for synching bind/unbind and topology event sending **/
    private final Object lock = new Object();

    /**
     * whether or not this service is activated - necessary to avoid sending
     * events to discovery awares before activate is done
     **/
    private boolean activated = false;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private HeartbeatHandler heartbeatHandler;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ConnectorRegistry connectorRegistry;

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private Config config;

    /** the slingId of the local instance **/
    private String slingId;

    /** the old view previously valid and sent to the TopologyEventListeners **/
    private TopologyViewImpl oldView = null;

    /**
     * Activate this service
     */
    protected void activate(final ComponentContext context) {
        logger.debug("DiscoveryServiceImpl activating...");

        if (settingsService == null) {
            throw new IllegalStateException("settingsService not found");
        }
        if (heartbeatHandler == null) {
            throw new IllegalStateException("heartbeatHandler not found");
        }

        slingId = settingsService.getSlingId();

        oldView = (TopologyViewImpl) getTopology();

        // make sure the first heartbeat is issued as soon as possible - which
        // is right after this service starts. since the two (discoveryservice
        // and heartbeatHandler need to know each other, the discoveryservice
        // is passed on to the heartbeatHandler in this initialize call).
        heartbeatHandler.initialize(this,
                clusterViewService.getIsolatedClusterViewId());

        final TopologyEventListener[] registeredServices;
        synchronized (lock) {
            registeredServices = this.eventListeners;
            doUpdateProperties();

            TopologyViewImpl newView = (TopologyViewImpl) getTopology();
            TopologyEvent event = new TopologyEvent(Type.TOPOLOGY_INIT, null,
                    newView);
            for (final TopologyEventListener da : registeredServices) {
                sendTopologyEvent(da, event);
            }
            activated = true;
            oldView = newView;
        }

        URL topologyConnectorURL = config.getTopologyConnectorURL();
        if (topologyConnectorURL != null) {
            connectorRegistry.registerOutgoingConnection(clusterViewService,
                    topologyConnectorURL, OriginInfo.Config);
        }

        logger.debug("DiscoveryServiceImpl activated.");
    }

    private void sendTopologyEvent(TopologyEventListener da, TopologyEvent event) {
        logger.debug("sendTopologyEvent: sending topologyEvent "+event+", to "+da);
        try{
            da.handleTopologyEvent(event);
        } catch(Exception e) {
            logger.warn("sendTopologyEvent: handler threw exception. handler: "+da+", exception: "+e, e);
        }
    }

    /**
     * Deactivate this service
     */
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("DiscoveryServiceImpl deactivated.");
        synchronized (lock) {
            activated = false;
        }
    }

    /**
     * bind a discovery aware
     */
    protected void bindTopologyEventListener(final TopologyEventListener eventListener) {

        logger.debug("bindTopologyEventListener: Binding TopologyEventListener {}",
                eventListener);

        boolean activated = false;
        synchronized (lock) {
            List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                    Arrays.asList(eventListeners));
            currentList.add(eventListener);
            this.eventListeners = currentList
                    .toArray(new TopologyEventListener[currentList.size()]);
            activated = this.activated;
        }

        if (activated) {
            sendTopologyEvent(eventListener, new TopologyEvent(
                    Type.TOPOLOGY_INIT, null, getTopology()));
        }
    }

    /**
     * Unbind a discovery aware
     */
    protected void unbindTopologyEventListener(final TopologyEventListener clusterAware) {

        logger.debug("unbindTopologyEventListener: Releasing TopologyEventListener {}",
                clusterAware);

        synchronized (lock) {
            List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                    Arrays.asList(eventListeners));
            currentList.remove(clusterAware);
            this.eventListeners = currentList
                    .toArray(new TopologyEventListener[currentList.size()]);
        }
    }

    /**
     * Bind a new property provider.
     */
    private void bindPropertyProvider(final PropertyProvider propertyProvider,
            final Map<String, Object> props) {
        logger.debug("bindPropertyProvider: Binding PropertyProvider {}",
                propertyProvider);

        final TopologyEventListener[] awares;
        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.add(info);
            Collections.sort(this.providerInfos);
            this.doUpdateProperties();
            if (activated) {
                awares = this.eventListeners;
            } else {
                awares = null;
            }
        }
        if (awares != null) {
            handlePotentialTopologyChange();
        }
    }

    /**
     * Update a property provider.
     */
    @SuppressWarnings("unused")
    private void updatedPropertyProvider(
            final PropertyProvider propertyProvider,
            final Map<String, Object> props) {
        logger.debug("bindPropertyProvider: Updating PropertyProvider {}",
                propertyProvider);

        this.unbindPropertyProvider(propertyProvider, props, false);
        this.bindPropertyProvider(propertyProvider, props);
    }

    /**
     * Unbind a property provider
     */
    @SuppressWarnings("unused")
    private void unbindPropertyProvider(
            final PropertyProvider propertyProvider,
            final Map<String, Object> props) {
        this.unbindPropertyProvider(propertyProvider, props, true);
    }

    /**
     * Unbind a property provider
     */
    private void unbindPropertyProvider(
            final PropertyProvider propertyProvider,
            final Map<String, Object> props, final boolean inform) {
        logger.debug("unbindPropertyProvider: Releasing PropertyProvider {}",
                propertyProvider);

        final TopologyEventListener[] awares;
        synchronized (lock) {
            final ProviderInfo info = new ProviderInfo(propertyProvider, props);
            this.providerInfos.remove(info);
            this.doUpdateProperties();
            if (activated) {
                awares = this.eventListeners;
            } else {
                awares = null;
            }
        }
        if (inform && awares != null) {
            handlePotentialTopologyChange();
        }
    }

    /**
     * Update the properties by inquiring the PropertyProvider's current values.
     * <p>
     * This method is invoked regularly by the heartbeatHandler.
     * The properties are stored in the repository under Config.getClusterInstancesPath()
     * and announced in the topology.
     * <p>
     * @see Config#getClusterInstancesPath()
     */
    private void doUpdateProperties() {
        if (resourceResolverFactory == null) {
            // cannot update the properties then..
            logger.debug("doUpdateProperties: too early to update the properties. resourceResolverFactory not yet set.");
            return;
        } else {
            logger.debug("doUpdateProperties: updating properties now..");
        }

        final Map<String, String> newProps = new HashMap<String, String>();
        for (final ProviderInfo info : this.providerInfos) {
            info.refreshProperties();
            newProps.putAll(info.properties);
        }

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            Resource myInstance = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            config.getClusterInstancesPath()
                                    + "/" + slingId + "/properties");

            Node node = myInstance.adaptTo(Node.class);

            PropertyIterator pit = node.getProperties();
            while (pit.hasNext()) {
                Property p = pit.nextProperty();
                if (newProps.containsKey(p.getName())) {
                    // perfect
                    continue;
                } else if (p.getName().equals("jcr:primaryType")) {
                    // ignore
                    continue;
                } else {
                    // remove
                    p.remove();
                }
            }

            for (Iterator<Entry<String, String>> it = newProps.entrySet()
                    .iterator(); it.hasNext();) {
                Entry<String, String> entry = it.next();
                logger.debug("doUpdateProperties: " + entry.getKey() + "="
                        + entry.getValue());
                node.setProperty(entry.getKey(), entry.getValue());
            }

            node.getSession().save();
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (RepositoryException e) {
            logger.error("handleEvent: got a RepositoryException: " + e, e);
            throw new RuntimeException(
                    "Exception while talking to repository (" + e + ")", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        logger.debug("doUpdateProperties: updating properties done.");
    }

    /**
     * @see DiscoveryService#getTopology()
     */
    public TopologyView getTopology() {
        TopologyViewImpl topology = new TopologyViewImpl();

        if (clusterViewService == null) {
            throw new IllegalStateException(
                    "DiscoveryService not yet initialized with IClusterViewService");
        }

        ClusterView localClusterView = clusterViewService.getClusterView();

        List<InstanceDescription> localInstances = localClusterView
                .getInstances();
        topology.addInstances(localInstances);

        Collection<InstanceDescription> attachedInstances = announcementRegistry
                .listInstances();
        topology.addInstances(attachedInstances);

        // TODO: isCurrent() might be wrong!!!

        return topology;
    }

    /**
     * Update the properties and sent a topology event if applicable
     */
    public void updateProperties() {
        synchronized (lock) {
            doUpdateProperties();
            handlePotentialTopologyChange();
        }
    }

    /**
     * Internal handle method which checks if anything in the topology has
     * changed and informs the TopologyEventListeners if such a change occurred.
     * <p>
     * All changes should go through this method. This method keeps track of
     * oldView/newView as well.
     */
    private void handlePotentialTopologyChange() {
        synchronized (lock) {
            if (!activated) {
                // ignore this call then - an early call to issue
                // a topologyevent before even activated
                logger.debug("handlePotentialTopologyChange: ignoring early change before activate finished.");
                return;
            }
            if (oldView == null) {
                throw new IllegalStateException("oldView must not be null");
            }
            TopologyViewImpl newView = (TopologyViewImpl) getTopology();
            TopologyViewImpl oldView = this.oldView;

            Type difference = newView.compareTopology(oldView);
            if (difference == null) {
                // then dont send any event then
                return;
            }

            oldView.markOld();
            if (difference!=Type.TOPOLOGY_CHANGED) {
                for (final TopologyEventListener da : eventListeners) {
                    sendTopologyEvent(da, new TopologyEvent(difference, oldView,
                            newView));
                }
            } else {
                for (final TopologyEventListener da : eventListeners) {
                    sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView,
                            null));
                }
                for (final TopologyEventListener da : eventListeners) {
                    sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGED, oldView,
                            newView));
                }
            }
            this.oldView = newView;
        }
    }

    /**
     * Internal class caching some provider infos like service id and ranking.
     */
    private final static class ProviderInfo implements Comparable<ProviderInfo> {

        public final PropertyProvider provider;
        public final Map<String, Object> serviceProps;
        public final int ranking;
        public final long serviceId;
        public final Map<String, String> properties = new HashMap<String, String>();

        public ProviderInfo(final PropertyProvider provider,
                final Map<String, Object> serviceProps) {
            this.provider = provider;
            this.serviceProps = serviceProps;
            final Object sr = serviceProps.get(Constants.SERVICE_RANKING);
            if (sr == null || !(sr instanceof Integer)) {
                this.ranking = 0;
            } else {
                this.ranking = (Integer) sr;
            }
            this.serviceId = (Long) serviceProps.get(Constants.SERVICE_ID);
            refreshProperties();
        }

        public void refreshProperties() {
            properties.clear();
            final Object namesObj = serviceProps
                    .get(PropertyProvider.PROPERTY_PROPERTIES);
            if (namesObj instanceof String) {
                final String val = provider.getProperty((String) namesObj);
                if (val != null) {
                    this.properties.put((String) namesObj, val);
                }
            } else if (namesObj instanceof String[]) {
                for (final String name : (String[]) namesObj) {
                    final String val = provider.getProperty(name);
                    if (val != null) {
                        this.properties.put(name, val);
                    }
                }
            }
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final ProviderInfo o) {
            // Sort by rank in ascending order.
            if (this.ranking < o.ranking) {
                return -1; // lower rank
            } else if (this.ranking > o.ranking) {
                return 1; // higher rank
            }
            // If ranks are equal, then sort by service id in descending order.
            return (this.serviceId < o.serviceId) ? 1 : -1;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ProviderInfo) {
                return ((ProviderInfo) obj).serviceId == this.serviceId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return provider.hashCode();
        }
    }

    /**
     * Handle the fact that the topology has likely changed
     */
    public void handleTopologyChanged() {
        logger.debug("handleTopologyChanged: informing the TopologyEventListeners...");
        handlePotentialTopologyChange();
    }

}
