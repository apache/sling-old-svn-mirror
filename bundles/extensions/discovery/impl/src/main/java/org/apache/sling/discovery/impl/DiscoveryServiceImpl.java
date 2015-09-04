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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
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
import org.apache.sling.discovery.impl.common.resource.IsolatedInstanceDescription;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;
import org.apache.sling.discovery.impl.topology.TopologyViewImpl;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
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
public class DiscoveryServiceImpl implements DiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    /** SLING-4755 : encapsulates an event that yet has to be sent (asynchronously) for a particular listener **/
    private final static class AsyncEvent {
        private final TopologyEventListener listener;
        private final TopologyEvent event;
        AsyncEvent(TopologyEventListener listener, TopologyEvent event) {
            if (listener==null) {
                throw new IllegalArgumentException("listener must not be null");
            }
            if (event==null) {
                throw new IllegalArgumentException("event must not be null");
            }
            this.listener = listener;
            this.event = event;
        }
        @Override
        public String toString() {
            return "an AsyncEvent[event="+event+", listener="+listener+"]";
        }
    }
    
    /** 
     * SLING-4755 : background runnable that takes care of asynchronously sending events.
     * <p>
     * API is: enqueue() puts a listener-event tuple onto the internal Q, which
     * is processed in a loop in run that does so (uninterruptably, even catching
     * Throwables to be 'very safe', but sleeps 5sec if an Error happens) until
     * flushThenStop() is called - which puts the sender in a state where any pending
     * events are still sent (flush) but then stops automatically. The argument of
     * using flush before stop is that the event was originally meant to be sent
     * before the bundle was stopped - thus just because the bundle is stopped
     * doesn't undo the event and it still has to be sent. That obviously can
     * mean that listeners can receive a topology event after deactivate. But I
     * guess that was already the case before the change to become asynchronous.
     */
    private final static class AsyncEventSender implements Runnable {
        
        /** stopped is always false until flushThenStop is called **/
        private boolean stopped = false;

        /** eventQ contains all AsyncEvent objects that have yet to be sent - in order to be sent **/
        private final List<AsyncEvent> eventQ = new LinkedList<AsyncEvent>();
        
        /** Enqueues a particular event for asynchronous sending to a particular listener **/
        void enqueue(TopologyEventListener listener, TopologyEvent event) {
            final AsyncEvent asyncEvent = new AsyncEvent(listener, event);
            synchronized(eventQ) {
                eventQ.add(asyncEvent);
                if (logger.isDebugEnabled()) {
                    logger.debug("enqueue: enqueued event {} for async sending (Q size: {})", asyncEvent, eventQ.size());
                }
                eventQ.notifyAll();
            }
        }
        
        /**
         * Stops the AsyncEventSender as soon as the queue is empty
         */
        void flushThenStop() {
            synchronized(eventQ) {
                logger.info("AsyncEventSender.flushThenStop: flushing (size: {}) & stopping...", eventQ.size());
                stopped = true;
                eventQ.notifyAll();
            }
        }
        
        /** Main worker loop that dequeues from the eventQ and calls sendTopologyEvent with each **/
        public void run() {
            logger.info("AsyncEventSender.run: started.");
            try{
                while(true) {
                    try{
                        final AsyncEvent asyncEvent;
                        synchronized(eventQ) {
                            while(!stopped && eventQ.isEmpty()) {
                                try {
                                    eventQ.wait();
                                } catch (InterruptedException e) {
                                    // issue a log debug but otherwise continue
                                    logger.debug("AsyncEventSender.run: interrupted while waiting for async events");
                                }
                            }
                            if (stopped) {
                                if (eventQ.isEmpty()) {
                                    // then we have flushed, so we can now finally stop
                                    logger.info("AsyncEventSender.run: flush finished. stopped.");
                                    return;
                                } else {
                                    // otherwise the eventQ is not yet empty, so we are still in flush mode
                                    logger.info("AsyncEventSender.run: flushing another event. (pending {})", eventQ.size());
                                }
                            }
                            asyncEvent = eventQ.remove(0);
                            if (logger.isDebugEnabled()) {
                                logger.debug("AsyncEventSender.run: dequeued event {}, remaining: {}", asyncEvent, eventQ.size());
                            }
                        }
                        if (asyncEvent!=null) {
                            sendTopologyEvent(asyncEvent);
                        }
                    } catch(Throwable th) {
                        // Even though we should never catch Error or RuntimeException
                        // here's the thinking about doing it anyway:
                        //  * in case of a RuntimeException that would be less dramatic
                        //    and catching it is less of an issue - we rather want
                        //    the background thread to be able to continue than
                        //    having it finished just because of a RuntimeException
                        //  * catching an Error is of course not so nice.
                        //    however, should we really give up this thread even in
                        //    case of an Error? It could be an OOM or some other 
                        //    nasty one, for sure. But even if. Chances are that
                        //    other parts of the system would also get that Error
                        //    if it is very dramatic. If not, then catching it
                        //    sounds feasible. 
                        // My two cents..
                        // the goal is to avoid quitting the AsyncEventSender thread
                        logger.error("AsyncEventSender.run: Throwable occurred. Sleeping 5sec. Throwable: "+th, th);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            logger.warn("AsyncEventSender.run: interrupted while sleeping");
                        }
                    }
                }
            } finally {
                logger.info("AsyncEventSender.run: quits (finally).");
            }
        }

        /** Actual sending of the asynchronous event - catches RuntimeExceptions a listener can send. (Error is caught outside) **/
        private void sendTopologyEvent(AsyncEvent asyncEvent) {
            final TopologyEventListener listener = asyncEvent.listener;
            final TopologyEvent event = asyncEvent.event;
            logger.debug("sendTopologyEvent: start: listener: {}, event: {}", listener, event);
            try{
                listener.handleTopologyEvent(event);
            } catch(final Exception e) {
                logger.warn("sendTopologyEvent: handler threw exception. handler: "+listener+", exception: "+e, e);
            }
            logger.debug("sendTopologyEvent: start: listener: {}, event: {}", listener, event);
        }
        
    }
    
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

    /** SLING-3750 : set when activate() could not send INIT events due to being in isolated mode **/
    private boolean initEventDelayed = false;
    
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

    /** 
     * whether or not there is a delayed event sending pending.
     * Marked volatile to allow getTopology() to read this without need for
     * synchronized(lock) (which would be deadlock-prone). (introduced with SLING-4638).
     **/
    private volatile boolean delayedEventPending = false;

    private ServiceRegistration mbeanRegistration;

    /** SLING-4755 : reference to the background AsyncEventSender. Started/stopped in activate/deactivate **/
    private AsyncEventSender asyncEventSender;

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

        oldView = (TopologyViewImpl) getTopology();
        oldView.markOld();

        // make sure the first heartbeat is issued as soon as possible - which
        // is right after this service starts. since the two (discoveryservice
        // and heartbeatHandler need to know each other, the discoveryservice
        // is passed on to the heartbeatHandler in this initialize call).
        heartbeatHandler.initialize(this,
                clusterViewService.getIsolatedClusterViewId());

        final TopologyEventListener[] registeredServices;
        synchronized (lock) {
            // SLING-4755 : start the asyncEventSender in the background
            //              will be stopped in deactivate (at which point
            //              all pending events will still be sent but no
            //              new events can be enqueued)
            asyncEventSender = new AsyncEventSender();
            Thread th = new Thread(asyncEventSender);
            th.setName("Discovery-AsyncEventSender");
            th.setDaemon(true);
            th.start();

            registeredServices = this.eventListeners;
            doUpdateProperties();

            TopologyViewImpl newView = (TopologyViewImpl) getTopology();
            final boolean isIsolatedView = isIsolated(newView);
            if (config.isDelayInitEventUntilVoted() && isIsolatedView) {
                // SLING-3750: just issue a log.info about the delaying
                logger.info("activate: this instance is in isolated mode and must yet finish voting before it can send out TOPOLOGY_INIT.");
                initEventDelayed = true;
            } else {
                if (isIsolatedView) {
                    // SLING-3750: issue a log.info about not-delaying even though isolated
                    logger.info("activate: this instance is in isolated mode and likely should delay TOPOLOGY_INIT - but corresponding config ('delayInitEventUntilVoted') is disabled.");
                }
                final TopologyEvent event = new TopologyEvent(Type.TOPOLOGY_INIT, null,
                        newView);
                for (final TopologyEventListener da : registeredServices) {
                    enqueueAsyncTopologyEvent(da, event);
                }
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
        
        registerMBean(bundleContext);        

        logger.debug("DiscoveryServiceImpl activated.");
    }

    private boolean isIsolated(TopologyViewImpl view) {
        final InstanceDescription localInstance = view.getLocalInstance();
        // 'instanceof' is not so nice here - but anything else requires 
        // excessive changing (introducing new classes/interfaces)
        // which is an overkill in and of itself.. thus: 'instanceof'
        return localInstance instanceof IsolatedInstanceDescription;
    }

    private void enqueueAsyncTopologyEvent(final TopologyEventListener da, final TopologyEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("enqueueAsyncTopologyEvent: sending topologyEvent {}, to {}", event, da);
        }
        if (asyncEventSender==null) {
            // this should never happen - sendTopologyEvent should only be called
            // when activated
            logger.warn("enqueueAsyncTopologyEvent: asyncEventSender is null, cannot send event ({}, {})!", da, event);
            return;
        }
        asyncEventSender.enqueue(da, event);
        if (logger.isDebugEnabled()) {
            logger.debug("enqueueAsyncTopologyEvent: sending topologyEvent {}, to {}", event, da);
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
            if (asyncEventSender!=null) {
                // it should always be not-null though
                asyncEventSender.flushThenStop();
                asyncEventSender = null;
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

        logger.debug("bindTopologyEventListener: Binding TopologyEventListener {}",
                eventListener);

        synchronized (lock) {
            final List<TopologyEventListener> currentList = new ArrayList<TopologyEventListener>(
                    Arrays.asList(eventListeners));
            currentList.add(eventListener);
            this.eventListeners = currentList
                    .toArray(new TopologyEventListener[currentList.size()]);
            if (activated && !initEventDelayed) {
                final TopologyViewImpl topology = (TopologyViewImpl) getTopology();
                if (delayedEventPending) {
                    // that means that for other TopologyEventListeners that were already bound
                    // and in general: the topology is currently CHANGING
                    // so we must reflect this with the isCurrent() flag (SLING-4638)
                    topology.markOld();
                }
                enqueueAsyncTopologyEvent(eventListener, new TopologyEvent(
                        Type.TOPOLOGY_INIT, null, topology));
            }
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

        // SLING-4638: set 'current' correctly
        if (isIsolated(topology) || delayedEventPending) {
            topology.markOld();
        }

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
        
        if (initEventDelayed) {
            if (isIsolated(newView)) {
                // we cannot proceed until we're out of the isolated mode..
                // SLING-4535 : while this has warning character, it happens very frequently,
                //              eg also when binding a PropertyProvider (so normal processing)
                //              hence lowering to info for now
                logger.info("handlePotentialTopologyChange: still in isolated mode - cannot send TOPOLOGY_INIT yet.");
                return;
            }
            logger.info("handlePotentialTopologyChange: new view is no longer isolated sending delayed TOPOLOGY_INIT now.");
            final TopologyEvent initEvent = new TopologyEvent(Type.TOPOLOGY_INIT, null,
                    newView); // SLING-4638: OK: newView is current==true as we're just coming out of initEventDelayed first time.
            for (final TopologyEventListener da : eventListeners) {
                enqueueAsyncTopologyEvent(da, initEvent);
            }
            // now after having sent INIT events, we need to set oldView to what we've
            // just sent out - which is newView. This makes sure that we don't send
            // out any CHANGING/CHANGED event afterwards based on an 'isolated-oldView'
            // (which would be wrong). Hence:
            this.oldView = newView;
            oldView = newView;

            initEventDelayed = false;
        }

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
                enqueueAsyncTopologyEvent(da, new TopologyEvent(difference, oldView,
                        newView));
            }
        } else { // TOPOLOGY_CHANGED

        	// send a TOPOLOGY_CHANGING first
            for (final TopologyEventListener da : eventListeners) {
                enqueueAsyncTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView,
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
                                enqueueAsyncTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGED,
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
                enqueueAsyncTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGED, oldView,
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
	        // SLING-4638: make sure the oldView is really marked as old:
	        oldView.markOld();
            for (final TopologyEventListener da : eventListeners) {
                enqueueAsyncTopologyEvent(da, new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView,
                        null));
            }
	        logger.error("forcedShutdown: deactivating DiscoveryService.");
	        // to make sure no further event is sent after this, flag this service as deactivated
            activated = false;
		}
	}

}
