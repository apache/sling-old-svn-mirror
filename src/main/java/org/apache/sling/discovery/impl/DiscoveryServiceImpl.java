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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
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
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
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
    private Scheduler scheduler;

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
    private TopologyViewImpl oldView;

    /** whether or not there is a delayed event sending pending **/
    private boolean delayedEventPending = false;

    /**
     * Activate this service
     */
    @Activate
    protected void activate() {
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

        URL[] topologyConnectorURLs = config.getTopologyConnectorURLs();
        if (topologyConnectorURLs != null) {
            for (int i = 0; i < topologyConnectorURLs.length; i++) {
                final URL aURL = topologyConnectorURLs[i];
                if (aURL!=null) {
                	try{
                		logger.info("activate: registering outgoing topology connector to "+aURL);
                		connectorRegistry.registerOutgoingConnector(clusterViewService, aURL);
                	} catch (final Exception e) {
                		logger.info("activate: could not register url: "+aURL+" due to: "+e, e);
                	}
                }
            }
        }

        logger.debug("DiscoveryServiceImpl activated.");
    }

    private void sendTopologyEvent(final TopologyEventListener da, final TopologyEvent event) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("sendTopologyEvent: sending topologyEvent {}, to {}", event, da);
    	}
        try{
            da.handleTopologyEvent(event);
        } catch(final Exception e) {
            logger.warn("sendTopologyEvent: handler threw exception. handler: "+da+", exception: "+e, e);
        }
    }

    /**
     * Deactivate this service
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("DiscoveryServiceImpl deactivated.");
        synchronized (lock) {
            activated = false;
        }
    }

    /**
     * bind a topology event listener
     */
    protected void bindTopologyEventListener(final TopologyEventListener eventListener) {

        logger.debug("bindTopologyEventListener: Binding TopologyEventListener {}",
                eventListener);

        boolean activated = false;
        synchronized (lock) {
            final List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
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
     * Unbind a topology event listener
     */
    protected void unbindTopologyEventListener(final TopologyEventListener eventListener) {

        logger.debug("unbindTopologyEventListener: Releasing TopologyEventListener {}",
                eventListener);

        synchronized (lock) {
            final List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                    Arrays.asList(eventListeners));
            currentList.remove(eventListener);
            this.eventListeners = currentList
                    .toArray(new TopologyEventListener[currentList.size()]);
        }
    }

    /**
     * Bind a new property provider.
     */
    protected void bindPropertyProvider(final PropertyProvider propertyProvider,
                                        final Map<String, Object> props) {
        logger.debug("bindPropertyProvider: Binding PropertyProvider {}",
                propertyProvider);

        synchronized (lock) {
            this.bindPropertyProviderInteral(propertyProvider, props);
        }
    }

    /**
     * Bind a new property provider.
     */
    private void bindPropertyProviderInteral(final PropertyProvider propertyProvider,
            final Map<String, Object> props) {
        final ProviderInfo info = new ProviderInfo(propertyProvider, props);
        this.providerInfos.add(info);
        Collections.sort(this.providerInfos);
        this.doUpdateProperties();
        handlePotentialTopologyChange();
    }

    /**
     * Update a property provider.
     */
    protected void updatedPropertyProvider(final PropertyProvider propertyProvider,
                                           final Map<String, Object> props) {
        logger.debug("bindPropertyProvider: Updating PropertyProvider {}",
                propertyProvider);

        synchronized (lock) {
           this.unbindPropertyProviderInternal(propertyProvider, props, false);
           this.bindPropertyProviderInteral(propertyProvider, props);
        }
    }

    /**
     * Unbind a property provider
     */
    protected void unbindPropertyProvider(final PropertyProvider propertyProvider,
                                          final Map<String, Object> props) {
        logger.debug("unbindPropertyProvider: Releasing PropertyProvider {}",
                propertyProvider);
        synchronized (lock) {
            this.unbindPropertyProviderInternal(propertyProvider, props, true);
        }
    }

    /**
     * Unbind a property provider
     */
    private void unbindPropertyProviderInternal(
            final PropertyProvider propertyProvider,
            final Map<String, Object> props, final boolean update) {

        final ProviderInfo info = new ProviderInfo(propertyProvider, props);
        if ( this.providerInfos.remove(info) && update ) {
            this.doUpdateProperties();
            this.handlePotentialTopologyChange();
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
            // SLING-2879 - revert/refresh resourceResolver here to work
            // around a potential issue with jackrabbit in a clustered environment
            resourceResolver.revert();
            resourceResolver.refresh();

            final ModifiableValueMap myInstanceMap = myInstance.adaptTo(ModifiableValueMap.class);
            final Set<String> keys = new HashSet<String>(myInstanceMap.keySet());
            for(final String key : keys) {
                if (newProps.containsKey(key)) {
                    // perfect
                    continue;
                } else if (key.indexOf(":")!=-1) {
                    // ignore
                    continue;
                } else {
                    // remove
                	myInstanceMap.remove(key);
                }
            }

            boolean anyChanges = false;
            for(final Entry<String, String> entry : newProps.entrySet()) {
            	Object existingValue = myInstanceMap.get(entry.getKey());
            	if (entry.getValue().equals(existingValue)) {
            	    // SLING-3389: dont rewrite the properties if nothing changed!
                    if (logger.isDebugEnabled()) {
                        logger.debug("doUpdateProperties: unchanged: {}={}", entry.getKey(), entry.getValue());
                    }
            	    continue;
            	}
            	if (logger.isDebugEnabled()) {
            	    logger.debug("doUpdateProperties: changed: {}={}", entry.getKey(), entry.getValue());
            	}
            	anyChanges = true;
                myInstanceMap.put(entry.getKey(), entry.getValue());
            }

            if (anyChanges) {
                resourceResolver.commit();
            }
        } catch (LoginException e) {
            logger.error(
                    "handleEvent: could not log in administratively: " + e, e);
            throw new RuntimeException("Could not log in to repository (" + e
                    + ")", e);
        } catch (PersistenceException e) {
            logger.error("handleEvent: got a PersistenceException: " + e, e);
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
        if (clusterViewService == null) {
            throw new IllegalStateException(
                    "DiscoveryService not yet initialized with IClusterViewService");
        }
        // create a new topology view
        final TopologyViewImpl topology = new TopologyViewImpl();

        final ClusterView localClusterView = clusterViewService.getClusterView();

        final List<InstanceDescription> localInstances = localClusterView.getInstances();
        topology.addInstances(localInstances);

        Collection<InstanceDescription> attachedInstances = announcementRegistry
                .listInstances(localClusterView);
        topology.addInstances(attachedInstances);

        // TODO: isCurrent() might be wrong!!!

        return topology;
    }

    /**
     * Update the properties and sent a topology event if applicable
     */
    public void updateProperties() {
        synchronized (lock) {
            logger.debug("updateProperties: calling doUpdateProperties.");
            doUpdateProperties();
            logger.debug("updateProperties: calling handlePotentialTopologyChange.");
            handlePotentialTopologyChange();
            logger.debug("updateProperties: done.");
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
        if (!activated) {
            // ignore this call then - an early call to issue
            // a topologyevent before even activated
            logger.debug("handlePotentialTopologyChange: ignoring early change before activate finished.");
            return;
        }
        if (delayedEventPending) {
            logger.debug("handlePotentialTopologyChange: ignoring potential change since a delayed event is pending.");
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
            logger.debug("handlePotentialTopologyChange: identical views. not informing listeners");
            return;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("handlePotentialTopologyChange: difference: {}, oldView={}, newView={}",
                        new Object[] {difference, oldView, newView});
            }
        }

        oldView.markOld();
        if (difference!=Type.TOPOLOGY_CHANGED) {
            for (final TopologyEventListener da : eventListeners) {
                sendTopologyEvent(da, new TopologyEvent(difference, oldView,
                        newView));
            }
        } else { // TOPOLOGY_CHANGED

        	// send a TOPOLOGY_CHANGING first
            for (final TopologyEventListener da : eventListeners) {
                sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView,
                        null));
            }

        	if (config.getMinEventDelay()>0) {
                // then delay the sending of the next event
                logger.debug("handlePotentialTopologyChange: delaying event sending to avoid event flooding");

                if (runAfter(config.getMinEventDelay() /*seconds*/ , new Runnable() {

                    public void run() {
                        synchronized(lock) {
                        	delayedEventPending = false;
                        	logger.debug("handlePotentialTopologyChange: sending delayed event now");
                        	if (!activated) {
                        		logger.debug("handlePotentialTopologyChange: no longer activated. not sending delayed event");
                        		return;
                        	}
                            final TopologyViewImpl newView = (TopologyViewImpl) getTopology();
                            // irrespective of the difference, send the latest topology
                            // via a topology_changed event (since we already sent a changing)
                            for (final TopologyEventListener da : eventListeners) {
                                sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGED,
                                        DiscoveryServiceImpl.this.oldView, newView));
                            }
                            DiscoveryServiceImpl.this.oldView = newView;
                        }
                        if (heartbeatHandler!=null) {
                            // trigger a heartbeat 'now' to pass it on to the topology asap
                            heartbeatHandler.triggerHeartbeat();
                        }
                    }
                })) {
                	delayedEventPending = true;
                    logger.debug("handlePotentialTopologyChange: delaying of event triggered.");
                    return;
                } else {
                	logger.debug("handlePotentialTopologyChange: delaying did not work for some reason.");
                }
        	}

        	// otherwise, send the TOPOLOGY_CHANGED now
            for (final TopologyEventListener da : eventListeners) {
                sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGED, oldView,
                        newView));
            }
        }

        this.oldView = newView;
        if (heartbeatHandler!=null) {
            // trigger a heartbeat 'now' to pass it on to the topology asap
            heartbeatHandler.triggerHeartbeat();
        }
    }

    /**
     * run the runnable after the indicated number of seconds, once.
     * @return true if the scheduling of the runnable worked, false otherwise
     */
    private boolean runAfter(int seconds, final Runnable runnable) {
        final Scheduler theScheduler = scheduler;
    	if (theScheduler == null) {
    		logger.info("runAfter: no scheduler set");
    		return false;
    	}
    	logger.debug("runAfter: trying with scheduler.fireJob");
    	final Date date = new Date(System.currentTimeMillis() + seconds * 1000);
		try {
		    theScheduler.fireJobAt(null, runnable, null, date);
			return true;
		} catch (Exception e) {
			logger.info("runAfter: could not schedule a job: "+e);
			return false;
		}
    }

    /**
     * Internal class caching some provider infos like service id and ranking.
     */
    private final static class ProviderInfo implements Comparable<ProviderInfo> {

        public final PropertyProvider provider;
        public final Object propertyProperties;
        public final int ranking;
        public final long serviceId;
        public final Map<String, String> properties = new HashMap<String, String>();

        public ProviderInfo(final PropertyProvider provider,
                final Map<String, Object> serviceProps) {
            this.provider = provider;
            this.propertyProperties = serviceProps.get(PropertyProvider.PROPERTY_PROPERTIES);
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
            if (this.propertyProperties instanceof String) {
                final String val = provider.getProperty((String) this.propertyProperties);
                if (val != null) {
                	putPropertyIfValid((String) this.propertyProperties, val);
                }
            } else if (this.propertyProperties instanceof String[]) {
                for (final String name : (String[]) this.propertyProperties) {
                    final String val = provider.getProperty(name);
                    if (val != null) {
                        putPropertyIfValid(name, val);
                    }
                }
            }
        }

        /** SLING-2883 : put property only if valid **/
		private void putPropertyIfValid(final String name, final String val) {
			if (ResourceHelper.isValidPropertyName(name)) {
				this.properties.put(name, val);
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
        logger.debug("handleTopologyChanged: calling handlePotentialTopologyChange.");
        synchronized ( this.lock ) {
            handlePotentialTopologyChange();
        }
    }

    /** SLING-2901 : send a TOPOLOGY_CHANGING event and shutdown the service thereafter **/
	public void forcedShutdown() {
		synchronized(lock) {
	        if (!activated) {
	            logger.error("forcedShutdown: ignoring forced shutdown. Service is not activated.");
	            return;
	        }
	        if (oldView == null) {
	            logger.error("forcedShutdown: ignoring forced shutdown. No oldView available.");
	            return;
	        }
	        logger.error("forcedShutdown: sending TOPOLOGY_CHANGING to all listeners");
            for (final TopologyEventListener da : eventListeners) {
                sendTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView,
                        null));
            }
	        logger.error("forcedShutdown: deactivating DiscoveryService.");
	        // to make sure no further event is sent after this, flag this service as deactivated
            activated = false;
		}
	}

}
