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
package org.apache.sling.discovery.oak;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

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
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.DefaultTopologyView;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.apache.sling.discovery.commons.providers.base.ViewStateManagerFactory;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.base.ClusterSyncHistory;
import org.apache.sling.discovery.commons.providers.spi.base.ClusterSyncServiceChain;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.commons.providers.spi.base.OakBacklogClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.discovery.commons.providers.util.PropertyNameHelper;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.oak.pinger.OakViewChecker;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the cross-cluster service uses the view manager
 * implementation for detecting changes in a cluster and only supports one
 * cluster (of which this instance is part of).
 */
@Component(immediate = true)
@Service(value = { DiscoveryService.class, OakDiscoveryService.class })
public class OakDiscoveryService extends BaseDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(OakDiscoveryService.class);

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
    private volatile boolean activated = false;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private OakViewChecker oakViewChecker;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ConnectorRegistry connectorRegistry;

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private Config config;

    @Reference
    private IdMapService idMapService;
    
    @Reference
    private OakBacklogClusterSyncService oakBacklogClusterSyncService;

    @Reference
    private SyncTokenService syncTokenService;

    /** the slingId of the local instance **/
    private String slingId;

    private ServiceRegistration mbeanRegistration;

    private ViewStateManager viewStateManager;

    private final ReentrantLock viewStateManagerLock = new ReentrantLock(); 
    
    private final List<TopologyEventListener> pendingListeners = new LinkedList<TopologyEventListener>();
    
    private TopologyEventListener changePropagationListener = new TopologyEventListener() {

        public void handleTopologyEvent(TopologyEvent event) {
            OakViewChecker checker = oakViewChecker;
            if (activated && checker != null 
                    && (event.getType() == Type.TOPOLOGY_CHANGED || event.getType() == Type.PROPERTIES_CHANGED)) {
                logger.info("changePropagationListener.handleTopologyEvent: topology changed - propagate through connectors");
                checker.triggerAsyncConnectorPing();
            }
        }
    };

    public static OakDiscoveryService testConstructor(SlingSettingsService settingsService,
            AnnouncementRegistry announcementRegistry,
            ConnectorRegistry connectorRegistry,
            ClusterViewService clusterViewService,
            Config config,
            OakViewChecker connectorPinger,
            Scheduler scheduler,
            IdMapService idMapService,
            OakBacklogClusterSyncService oakBacklogClusterSyncService,
            SyncTokenService syncTokenService,
            ResourceResolverFactory factory) {
        OakDiscoveryService discoService = new OakDiscoveryService();
        discoService.settingsService = settingsService;
        discoService.announcementRegistry = announcementRegistry;
        discoService.connectorRegistry = connectorRegistry;
        discoService.clusterViewService = clusterViewService;
        discoService.config = config;
        discoService.oakViewChecker = connectorPinger;
        discoService.scheduler = scheduler;
        discoService.idMapService = idMapService;
        discoService.oakBacklogClusterSyncService = oakBacklogClusterSyncService;
        discoService.syncTokenService = syncTokenService;
        discoService.resourceResolverFactory = factory;
        return discoService;
    }

    protected void handleIsolatedFromTopology() {
        if (oakViewChecker!=null) {
            // SLING-5030 part 2: when we detect being isolated we should
            // step at the end of the leader-election queue and 
            // that can be achieved by resetting the leaderElectionId
            // (which will in turn take effect on the next round of
            // voting, or also double-checked when the local instance votes)
            //
            //TODO:
            // Note that when the local instance doesn't notice
            // an 'ISOLATED_FROM_TOPOLOGY' case, then the leaderElectionId
            // will not be reset. Which means that it then could potentially
            // regain leadership.
            if (oakViewChecker.resetLeaderElectionId()) {
                logger.info("getTopology: reset leaderElectionId to force this instance to the end of the instance order (thus incl not to remain leader)");
            }
        }
    }
    
    /**
     * Activate this service
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        logger.debug("OakDiscoveryService activating...");

        if (settingsService == null) {
            throw new IllegalStateException("settingsService not found");
        }
        if (oakViewChecker == null) {
            throw new IllegalStateException("heartbeatHandler not found");
        }

        slingId = settingsService.getSlingId();

        ClusterSyncService consistencyService;
        if (config.getSyncTokenEnabled()) {
            //TODO: ConsistencyHistory is implemented a little bit hacky ..
            ClusterSyncHistory consistencyHistory = new ClusterSyncHistory();
            oakBacklogClusterSyncService.setConsistencyHistory(consistencyHistory);
            syncTokenService.setConsistencyHistory(consistencyHistory);
            consistencyService = new ClusterSyncServiceChain(oakBacklogClusterSyncService, syncTokenService);
        } else {
            consistencyService = oakBacklogClusterSyncService;
            
        }
        viewStateManager = ViewStateManagerFactory.newViewStateManager(viewStateManagerLock, consistencyService);

        if (config.getMinEventDelay()>0) {
            viewStateManager.installMinEventDelayHandler(this, scheduler, config.getMinEventDelay());
        }

        final String isolatedClusterId = UUID.randomUUID().toString();
        {
            // create a pre-voting/isolated topologyView which would be used
            // until the first voting has finished.
            // this way for the single-instance case the clusterId can
            // remain the same between a getTopology() that is invoked before
            // the first TOPOLOGY_INIT and afterwards
            DefaultClusterView isolatedCluster = new DefaultClusterView(isolatedClusterId);
            Map<String, String> emptyProperties = new HashMap<String, String>();
            DefaultInstanceDescription isolatedInstance = 
                    new DefaultInstanceDescription(isolatedCluster, true, true, slingId, emptyProperties);
            Collection<InstanceDescription> col = new ArrayList<InstanceDescription>();
            col.add(isolatedInstance);
            final DefaultTopologyView topology = new DefaultTopologyView();
            topology.addInstances(col);
            topology.setNotCurrent();
            setOldView(topology);
        }
        setOldView((DefaultTopologyView) getTopology());
        getOldView().setNotCurrent();

        // make sure the first heartbeat is issued as soon as possible - which
        // is right after this service starts. since the two (discoveryservice
        // and heartbeatHandler need to know each other, the discoveryservice
        // is passed on to the heartbeatHandler in this initialize call).
        oakViewChecker.initialize(this);

        viewStateManagerLock.lock();
        try{
            viewStateManager.handleActivated();

            doUpdateProperties();

            DefaultTopologyView newView = (DefaultTopologyView) getTopology();
            if (newView.isCurrent()) {
                viewStateManager.handleNewView(newView);
            } else {
                // SLING-3750: just issue a log.info about the delaying
                logger.info("activate: this instance is in isolated mode and must yet finish voting before it can send out TOPOLOGY_INIT.");
            }
            activated = true;
            setOldView(newView);
            
            // in case bind got called before activate we now have pending listeners,
            // bind them to the viewstatemanager too
            for (TopologyEventListener listener : pendingListeners) {
                viewStateManager.bind(listener);
            }
            pendingListeners.clear();
            
            viewStateManager.bind(changePropagationListener);
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
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
        
        logger.debug("OakDiscoveryService activated.");
    }

    /**
     * Deactivate this service
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("OakDiscoveryService deactivated.");
        viewStateManagerLock.lock();
        try{
            viewStateManager.unbind(changePropagationListener);

            viewStateManager.handleDeactivated();
            
            activated = false;
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
        }
        try{
            if ( this.mbeanRegistration != null ) {
                this.mbeanRegistration.unregister();
                this.mbeanRegistration = null;
            }
        } catch(Exception e) {
            logger.error("deactivate: Error on unregister: "+e, e);
        }
    }

    /**
     * bind a topology event listener
     */
    protected void bindTopologyEventListener(final TopologyEventListener eventListener) {
        viewStateManagerLock.lock();
        try{
            if (!activated) {
                pendingListeners.add(eventListener);
            } else {
                viewStateManager.bind(eventListener);
            }
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
        }
    }

    /**
     * Unbind a topology event listener
     */
    protected void unbindTopologyEventListener(final TopologyEventListener eventListener) {
        viewStateManagerLock.lock();
        try{
            if (!activated) {
                pendingListeners.remove(eventListener);
            } else {
                viewStateManager.unbind(eventListener);
            }
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
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
        if (activated) {
            this.doUpdateProperties();
        }
        checkForTopologyChange();
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
            if (activated) {
                this.doUpdateProperties();
            }
            this.checkForTopologyChange();
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
        // SLING-5382 : the caller must ensure that this method
        // is not invoked after deactivation or before activation.
        // so this method doesn't have to do any further synchronization.
        // what we do nevertheless is a paranoia way of checking if
        // all variables are available and do a NOOP if that's not the case.
        final ResourceResolverFactory rrf = resourceResolverFactory;
        final Config c = config;
        final String sid = slingId;
        if (rrf == null || c == null || sid == null) {
            // cannot update the properties then..
            logger.debug("doUpdateProperties: too early to update the properties. "
                    + "resourceResolverFactory ({}), config ({}) or slingId ({}) not yet set.",
                    new Object[] {rrf, c, sid});
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
            resourceResolver = rrf
                    .getAdministrativeResourceResolver(null);

            Resource myInstance = ResourceHelper
                    .getOrCreateResource(
                            resourceResolver,
                            c.getClusterInstancesPath()
                                    + "/" + sid + "/properties");
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
     * Update the properties and sent a topology event if applicable
     */
    public void updateProperties() {
        synchronized (lock) {
            if (!activated) {
                logger.debug("updateProperties: not yet activated, not calling doUpdateProperties this time.");
            } else {
                logger.debug("updateProperties: calling doUpdateProperties.");
                doUpdateProperties();
            }
            logger.debug("updateProperties: calling handlePotentialTopologyChange.");
            checkForTopologyChange();
            logger.debug("updateProperties: done.");
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
			if (PropertyNameHelper.isValidPropertyName(name)) {
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
     * Check the current topology for any potential change
     */
    public void checkForTopologyChange() {
        viewStateManagerLock.lock();
        try{
            if (!activated) {
                logger.debug("checkForTopologyChange: not yet activated, ignoring");
                return;
            }
            DefaultTopologyView t = (DefaultTopologyView) getTopology();
            if (t.isCurrent()) {
                // if we have a valid view, let the viewStateManager do the
                // comparison and sending of an event, if necessary
                viewStateManager.handleNewView(t);
                setOldView(t);
            } else {
                // if we don't have a view, then we might have to send
                // a CHANGING event, let that be decided by the viewStateManager as well
                viewStateManager.handleChanging();
            }
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
        }
    }

    /**
     * Handle the fact that the topology has started to change - inform the listeners asap
     */
    public void handleTopologyChanging() {
        logger.debug("handleTopologyChanging: invoking viewStateManager.handlechanging");
        viewStateManager.handleChanging();
    }

    protected ClusterViewService getClusterViewService() {
        return clusterViewService;
    }
    
    protected AnnouncementRegistry getAnnouncementRegistry() {
        return announcementRegistry;
    }

    /** for testing only 
     * @return */
    public ViewStateManager getViewStateManager() {
        return viewStateManager;
    }

}
