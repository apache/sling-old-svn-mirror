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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException.Reason;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.apache.sling.discovery.commons.providers.base.ViewStateManagerFactory;
import org.apache.sling.discovery.commons.providers.spi.ClusterSyncService;
import org.apache.sling.discovery.commons.providers.spi.LocalClusterView;
import org.apache.sling.discovery.commons.providers.spi.base.SyncTokenService;
import org.apache.sling.discovery.commons.providers.util.PropertyNameHelper;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.discovery.impl.cluster.ClusterViewServiceImpl;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
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
@Service(value = { DiscoveryService.class, DiscoveryServiceImpl.class })
public class DiscoveryServiceImpl extends BaseDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    /**
     * This ClusterSyncService just 'passes the call through', ie it doesn't actually
     * do anything in sync but just calls callback.run() immediately. It therefore
     * also doesn't have to do anything on cancelling.
     */
    private static final ClusterSyncService PASS_THROUGH_CLUSTER_SYNC_SERVICE = new ClusterSyncService() {

        @Override
        public void sync(BaseTopologyView view, Runnable callback) {
            logger.debug("sync: no syncToken applicable");
            callback.run();
        }
        
        @Override
        public void cancelSync() {
            // cancelling not applicable
        }
    };

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
    private ClusterViewServiceImpl clusterViewService;

    @Reference
    private Config config;
    
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
            HeartbeatHandler handler = heartbeatHandler;
            if (activated && handler != null 
                    && (event.getType() == Type.TOPOLOGY_CHANGED || event.getType() == Type.PROPERTIES_CHANGED)) {
                logger.info("changePropagationListener.handleTopologyEvent: topology changed - propagate through connectors");
                handler.triggerAsyncConnectorPing();
            }
        }
    };

    /** for testing only **/
    public static BaseDiscoveryService testConstructor(ResourceResolverFactory resourceResolverFactory, 
            AnnouncementRegistry announcementRegistry, 
            ConnectorRegistry connectorRegistry,
            ClusterViewServiceImpl clusterViewService,
            HeartbeatHandler heartbeatHandler,
            SlingSettingsService settingsService,
            Scheduler scheduler,
            Config config,
            SyncTokenService syncTokenServiceOrNull) {
        DiscoveryServiceImpl discoService = new DiscoveryServiceImpl();
        discoService.resourceResolverFactory = resourceResolverFactory;
        discoService.announcementRegistry = announcementRegistry;
        discoService.connectorRegistry = connectorRegistry;
        discoService.clusterViewService = clusterViewService;
        discoService.heartbeatHandler = heartbeatHandler;
        discoService.settingsService = settingsService;
        discoService.scheduler = scheduler;
        discoService.config = config;
        discoService.syncTokenService = syncTokenServiceOrNull;
        return discoService;
    }

    public DiscoveryServiceImpl() {
    }

    protected void registerMBean(BundleContext bundleContext) {
        if (this.mbeanRegistration!=null) {
            try{
                if ( this.mbeanRegistration != null ) {
                    this.mbeanRegistration.unregister();
                    this.mbeanRegistration = null;
                }
            } catch(Exception e) {
                logger.error("registerMBean: Error on unregister: "+e, e);
            }
        }
        try {
            final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
            mbeanProps.put("jmx.objectname", "org.apache.sling:type=discovery,name=DiscoveryServiceImpl");

            final DiscoveryServiceMBeanImpl mbean = new DiscoveryServiceMBeanImpl(heartbeatHandler);
            this.mbeanRegistration = bundleContext.registerService(DiscoveryServiceMBeanImpl.class.getName(), mbean, mbeanProps);
        } catch (Throwable t) {
            logger.warn("registerMBean: Unable to register DiscoveryServiceImpl MBean", t);
        }
    }
    
    protected void handleIsolatedFromTopology() {
        if (heartbeatHandler!=null) {
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
            if (heartbeatHandler.resetLeaderElectionId()) {
                logger.info("getTopology: reset leaderElectionId to force this instance to the end of the instance order (thus incl not to remain leader)");
            }
        }
    }
    
    /**
     * Activate this service
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        logger.debug("DiscoveryServiceImpl activating...");

        if (settingsService == null) {
            throw new IllegalStateException("settingsService not found");
        }
        if (heartbeatHandler == null) {
            throw new IllegalStateException("heartbeatHandler not found");
        }

        slingId = settingsService.getSlingId();

        final ClusterSyncService clusterSyncService;
        if (!config.useSyncTokenService()) {
            logger.info("activate: useSyncTokenService is configured to false. Using pass-through cluster-sync-service.");
            clusterSyncService = PASS_THROUGH_CLUSTER_SYNC_SERVICE;
        } else if (syncTokenService == null) {
            logger.warn("activate: useSyncTokenService is configured to true but there's no SyncTokenService! Using pass-through cluster-sync-service instead.");
            clusterSyncService = syncTokenService;
        } else {
            logger.info("activate: useSyncTokenService is configured to true, using the available SyncTokenService: " + syncTokenService);
            clusterSyncService = syncTokenService;
        }
        viewStateManager = ViewStateManagerFactory.newViewStateManager(viewStateManagerLock, clusterSyncService);

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
        heartbeatHandler.initialize(this, isolatedClusterId);

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
        
        registerMBean(bundleContext);        

        logger.debug("DiscoveryServiceImpl activated.");
    }

    /**
     * Deactivate this service
     */
    @Deactivate
    protected void deactivate() {
        logger.debug("DiscoveryServiceImpl deactivated.");
        viewStateManagerLock.lock();
        try{
            if (viewStateManager != null) {
                viewStateManager.unbind(changePropagationListener);
                viewStateManager.handleDeactivated();
            }
            
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
        this.doUpdateProperties();
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
            this.doUpdateProperties();
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
     * Update the properties and sent a topology event if applicable
     */
    public void updateProperties() {
        synchronized (lock) {
            logger.debug("updateProperties: calling doUpdateProperties.");
            doUpdateProperties();
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
     * only checks for local clusterView changes.
     * thus eg avoids doing synchronized with annotationregistry 
     **/
    public void checkForLocalClusterViewChange() {
        viewStateManagerLock.lock();
        try{
            if (!activated) {
                logger.debug("checkForLocalClusterViewChange: not yet activated, ignoring");
                return;
            }
            try {
                ClusterViewService clusterViewService = getClusterViewService();
                if (clusterViewService == null) {
                    throw new UndefinedClusterViewException(
                            Reason.REPOSITORY_EXCEPTION,
                            "no ClusterViewService available at the moment");
                }
                LocalClusterView localClusterView = clusterViewService.getLocalClusterView();
            } catch (UndefinedClusterViewException e) {
                // SLING-5030 : when we're cut off from the local cluster we also
                // treat it as being cut off from the entire topology, ie we don't
                // update the announcements but just return
                // the previous oldView marked as !current
                logger.info("checkForLocalClusterViewChange: undefined cluster view: "+e.getReason()+"] "+e);
                getOldView().setNotCurrent();
                viewStateManager.handleChanging();
                if (e.getReason()==Reason.ISOLATED_FROM_TOPOLOGY) {
                    handleIsolatedFromTopology();
                }
            }
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
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
        viewStateManagerLock.lock();
        try{
            if (!activated) {
                logger.error("handleTopologyChanging: not yet activated!");
                return;
            }
            logger.debug("handleTopologyChanging: invoking viewStateManager.handlechanging");
            viewStateManager.handleChanging();
        } finally {
            if (viewStateManagerLock!=null) {
                viewStateManagerLock.unlock();
            }
        }
    }

    /** SLING-2901 : send a TOPOLOGY_CHANGING event and shutdown the service thereafter **/
	public void forcedShutdown() {
		synchronized(lock) {
	        if (!activated) {
	            logger.error("forcedShutdown: ignoring forced shutdown. Service is not activated.");
	            return;
	        }
	        if (getOldView() == null) {
	            logger.error("forcedShutdown: ignoring forced shutdown. No oldView available.");
	            return;
	        }
	        logger.error("forcedShutdown: sending TOPOLOGY_CHANGING to all listeners");
	        // SLING-4638: make sure the oldView is really marked as old:
	        getOldView().setNotCurrent();
	        viewStateManager.handleChanging();
	        logger.error("forcedShutdown: deactivating DiscoveryService.");
	        // to make sure no further event is sent after this, flag this service as deactivated
            activated = false;
		}
	}
	
    protected ClusterViewService getClusterViewService() {
        return clusterViewService;
    }
    
    public ClusterViewServiceImpl getClusterViewServiceImpl() {
        return clusterViewService;
    }

    protected AnnouncementRegistry getAnnouncementRegistry() {
        return announcementRegistry;
    }

    /** for testing only 
     * @return */
    protected ViewStateManager getViewStateManager() {
        return viewStateManager;
    }
}
