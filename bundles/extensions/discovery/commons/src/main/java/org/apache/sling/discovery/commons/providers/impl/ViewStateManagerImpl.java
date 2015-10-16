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
package org.apache.sling.discovery.commons.providers.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.commons.InstancesDiff;
import org.apache.sling.discovery.commons.InstancesDiff.InstanceCollection;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.EventFactory;
import org.apache.sling.discovery.commons.providers.ViewStateManager;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ViewStateManager is at the core of managing TopologyEventListeners,
 * the 'view state' (changing vs changed) and sending out the appropriate
 * and according TopologyEvents to the registered listeners.
 * <p>
 * Note re synchronization: this class rquires a lock object to be passed
 * in the constructor - this will be applied to all public methods
 * appropriately. Additionally, the ConsistencyService callback will
 * also be locked using the provided lock object.
 */
class ViewStateManagerImpl implements ViewStateManager {

    private static final Logger logger = LoggerFactory.getLogger(ViewStateManagerImpl.class);
    
    /** 
     * List of bound listeners that have already received their INIT event - others are in unInitializedEventListeners.
     * @see ViewStateManagerImpl#unInitializedEventListeners
     */
    private List<TopologyEventListener> eventListeners = new ArrayList<TopologyEventListener>();

    /**
     * List of bound listeners that have not yet received their TOPOLOGY_INIT event - 
     * once they are sent the TOPOLOGY_INIT event they are moved to eventListeners (and stay there).
     * <p>
     * This list becomes necessary for cases where the bind() happens before activate, or after activate but at a time
     * when the topology is TOPOLOGY_CHANGING - at which point an TOPOLOGY_INIT event can not yet be sent.
     * @see ViewStateManagerImpl#eventListeners
     */
    private List<TopologyEventListener> unInitializedEventListeners = new ArrayList<TopologyEventListener>();
    
    /** 
     * Set true when the bundle.activate() was called, false if not yet or when deactivate() is called.
     * <p>
     * This controls whether handleChanging() and handleNewView() should cause any events
     * to be sent - which they do not if called before handleActivated() (or after handleDeactivated())
     * @see ViewStateManagerImpl#handleActivated()
     * @see ViewStateManagerImpl#handleChanging()
     * @see ViewStateManagerImpl#handleNewView(BaseTopologyView)
     * @see ViewStateManagerImpl#handleDeactivated()
     */
    private boolean activated;
    
    /**
     * Represents the 'newView' passed to handleNewTopologyView at the most recent invocation.
     * <p>
     * This is used for:
     * <ul>
     *  <li>sending with the TOPOLOGY_INIT event to newly bound listeners or at activate time</li>
     *  <li>sending as oldView (marked not current) with the TOPOLOGY_CHANGING event</li>
     *  <li>sending as oldView (marked not current in case handleChanging() was not invoked) with the TOPOLOGY_CHANGED event</li>
     * </ul>
     */
    private BaseTopologyView previousView;
    
    /**
     * Set to true when handleChanging is called - set to false in handleNewView.
     * When this goes true, a TOPOLOGY_CHANGING is sent.
     * When this goes false, a TOPOLOGY_CHANGED is sent.
     */
    private boolean isChanging;

    /**
     * The lock object with which all public methods are guarded - to be provided in the constructor.
     */
    protected final Lock lock;

    /**
     * An optional ConsistencyService can be provided in the constructor which, when set, will
     * be invoked upon a new view becoming available (in handleNewView) and the actual
     * TOPOLOGY_CHANGED event will only be sent once the ConsistencyService.sync method
     * does the according callback (which can be synchronous or asynchronous again).
     */
    private final ConsistencyService consistencyService;
    
    /** 
     * A modification counter that increments on each of the following methods:
     * <ul>
     *  <li>handleActivated()</li>
     *  <li>handleDeactivated()</li>
     *  <li>handleChanging()</li>
     *  <li>handleNewView()</li>
     * </ul>
     * with the intent that - when a consistencyService is set - the callback from the
     * ConsistencyService can check if any of the above methods was invoked - and if so,
     * it does not send the TOPOLOGY_CHANGED event due to those new facts that happened
     * while it was synching with the repository.
     */
    private int modCnt = 0;

    /** SLING-4755 : reference to the background AsyncEventSender. Started/stopped in activate/deactivate **/
    private AsyncEventSender asyncEventSender;
    
    /** SLING-5030 : this map contains the event last sent to each listener to prevent duplicate CHANGING events when scheduler is broken**/
    private Map<TopologyEventListener,TopologyEvent.Type> lastEventMap = new HashMap<TopologyEventListener, TopologyEvent.Type>();

    private MinEventDelayHandler minEventDelayHandler;

    /**
     * Creates a new ViewStateManager which synchronizes each method with the given
     * lock and which optionally uses the given ConsistencyService to sync the repository
     * upon handling a new view where an instances leaves the local cluster.
     * @param lock the lock to be used - must not be null
     * @param consistencyService optional (ie can be null) - the ConsistencyService to 
     * sync the repository upon handling a new view where an instances leaves the local cluster.
     */
    ViewStateManagerImpl(Lock lock, ConsistencyService consistencyService) {
        if (lock==null) {
            throw new IllegalArgumentException("lock must not be null");
        }
        this.lock = lock;
        this.consistencyService = consistencyService;
    }

    @Override
    public void installMinEventDelayHandler(DiscoveryService discoveryService, Scheduler scheduler, long minEventDelaySecs) {
        this.minEventDelayHandler = new MinEventDelayHandler(this, lock, discoveryService, scheduler, minEventDelaySecs);
    }
    
    protected boolean hadPreviousView() {
        return previousView!=null;
    }
    
    protected boolean unchanged(BaseTopologyView newView) {
        if (isChanging) {
            return false;
        }
        if (previousView==null) {
            return false;
        }
        return previousView.equals(newView);
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#bind(org.apache.sling.discovery.TopologyEventListener)
     */
    @Override
    public void bind(final TopologyEventListener eventListener) {
        logger.trace("bind: start {}", eventListener);
        lock.lock();
        try{
            logger.debug("bind: Binding TopologyEventListener {}",
                    eventListener);
            
            if (eventListeners.contains(eventListener) || unInitializedEventListeners.contains(eventListener)) {
                logger.info("bind: TopologyEventListener already registered: "+eventListener);
                return;
            }
    
            if (activated) {
                // check to see in which state we are
                if (isChanging || (previousView==null)) {
                    // then we cannot send the TOPOLOGY_INIT at this point - need to delay this
                    if (logger.isDebugEnabled()) {
                        logger.debug("bind: view is not yet/currently not defined (isChanging: "+isChanging+
                                ", previousView==null: "+(previousView==null)+
                                ", delaying INIT to "+eventListener);
                    }
                    unInitializedEventListeners.add(eventListener);
                } else {
                    // otherwise we can send the TOPOLOGY_INIT now
                    logger.debug("bind: view is defined, sending INIT now to {}",
                            eventListener);
                    enqueue(eventListener, EventFactory.newInitEvent(previousView));
                    eventListeners.add(eventListener);
                }
            } else {
                logger.debug("bind: not yet activated, delaying INIT to {}",
                        eventListener);
                unInitializedEventListeners.add(eventListener);
            }
        } finally {
            lock.unlock();
            logger.trace("bind: end");
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#unbind(org.apache.sling.discovery.TopologyEventListener)
     */
    @Override
    public boolean unbind(final TopologyEventListener eventListener) {
        logger.trace("unbind: start {}", eventListener);
        lock.lock();
        try{
            logger.debug("unbind: Releasing TopologyEventListener {}",
                    eventListener);
    
            // even though a listener must always only ever exist in one of the two,
            // the unbind we do - for safety-paranoia-reasons - remove them from both
            final boolean a = eventListeners.remove(eventListener);
            final boolean b = unInitializedEventListeners.remove(eventListener);
            return a || b;
        } finally {
            lock.unlock();
            logger.trace("unbind: end");
        }
    }
    
    private void enqueue(final TopologyEventListener da, final TopologyEvent event) {
        logger.trace("enqueue: start: topologyEvent {}, to {}", event, da);
        if (asyncEventSender==null) {
            // this should never happen - sendTopologyEvent should only be called
            // when activated
            logger.warn("enqueue: asyncEventSender is null, cannot send event ({}, {})!", da, event);
            return;
        }
        if (lastEventMap.get(da)==event.getType() && event.getType()==Type.TOPOLOGY_CHANGING) {
            // don't sent TOPOLOGY_CHANGING twice
            logger.debug("enqueue: listener already got TOPOLOGY_CHANGING: {}", da);
            return;
        }
        logger.debug("enqueue: enqueuing topologyEvent {}, to {}", event, da);
        asyncEventSender.enqueue(da, event);
        lastEventMap.put(da, event.getType());
        logger.trace("enqueue: sending topologyEvent {}, to {}", event, da);
    }

    /** Internal helper method that sends a given event to a list of listeners **/
    private void enqueueForAll(final List<TopologyEventListener> audience, final TopologyEvent event) {
        logger.debug("enqueueForAll: sending topologyEvent {}, to all ({}) listeners", event, audience.size());
        for (Iterator<TopologyEventListener> it = audience.iterator(); it.hasNext();) {
            TopologyEventListener topologyEventListener = it.next();
            enqueue(topologyEventListener, event);
        }
        logger.trace("enqueueForAll: sent topologyEvent {}, to all ({}) listeners", event, audience.size());
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#handleActivated()
     */
    @Override
    public void handleActivated() {
        logger.trace("handleActivated: start");
        lock.lock();
        try{
            logger.debug("handleActivated: activating the ViewStateManager");
            activated = true;
            modCnt++;
            
            // SLING-4755 : start the asyncEventSender in the background
            //              will be stopped in deactivate (at which point
            //              all pending events will still be sent but no
            //              new events can be enqueued)
            asyncEventSender = new AsyncEventSender();
            Thread th = new Thread(asyncEventSender);
            th.setName("Discovery-AsyncEventSender");
            th.setDaemon(true);
            th.start();

            if (previousView!=null && !isChanging) {
                enqueueForAll(unInitializedEventListeners, EventFactory.newInitEvent(previousView));
                eventListeners.addAll(unInitializedEventListeners);
                unInitializedEventListeners.clear();
            }
            logger.debug("handleActivated: activated the ViewStateManager");
        } finally {
            lock.unlock();
            logger.trace("handleActivated: finally");
        }
    }

    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#handleDeactivated()
     */
    @Override
    public void handleDeactivated() {
        logger.trace("handleDeactivated: start");
        lock.lock();
        try{
            logger.debug("handleDeactivated: deactivating the ViewStateManager");
            activated = false;
            modCnt++;
    
            if (asyncEventSender!=null) {
                // it should always be not-null though
                asyncEventSender.flushThenStop();
                asyncEventSender = null;
            }

            if (previousView!=null) {
                previousView.setNotCurrent();
                logger.trace("handleDeactivated: setting previousView to null");
                previousView = null;
            }
            logger.trace("handleDeactivated: setting isChanging to false");
            isChanging = false;
            
            eventListeners.clear();
            unInitializedEventListeners.clear();
            logger.debug("handleDeactivated: deactivated the ViewStateManager");
        } finally {
            lock.unlock();
            logger.trace("handleDeactivated: finally");
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#handleChanging()
     */
    @Override
    public void handleChanging() {
        logger.trace("handleChanging: start");
        lock.lock();
        try{
    
            if (isChanging) {
                // if isChanging: then this is no news
                // hence: return asap
                logger.debug("handleChanging: was already changing - ignoring.");
                return;
            }
            modCnt++;
            
            // whether activated or not: set isChanging to true now
            logger.trace("handleChanging: setting isChanging to true");
            isChanging = true;
            
            if (!activated) {
                // if not activated: we can only start sending events once activated
                // hence returning here - after isChanging was set to true accordingly
                
                // note however, that if !activated, there should be no eventListeners yet
                // all of them should be in unInitializedEventListeners at the moment
                // waiting for activate() and handleNewTopologyView
                logger.debug("handleChanging: not yet activated - ignoring.");
                return;
            }
            
            if (previousView==null) {
                // then nothing further to do - this is a very early changing event
                // before even the first view was available
                logger.debug("handleChanging: no previousView set - ignoring.");
                return;
            }
            
            logger.debug("handleChanging: sending TOPOLOGY_CHANGING to initialized listeners");
            previousView.setNotCurrent();
            enqueueForAll(eventListeners, EventFactory.newChangingEvent(previousView));
        } finally {
            lock.unlock();
            logger.trace("handleChanging: finally");
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.discovery.commons.providers.impl.ViewStateManager#handleNewView(org.apache.sling.discovery.commons.providers.BaseTopologyView)
     */
    @Override
    public void handleNewView(final BaseTopologyView newView) {
        logger.trace("handleNewView: start, newView={}", newView);
        if (newView==null) {
            throw new IllegalArgumentException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            logger.debug("handleNewView: newView is not current - calling handleChanging.");
            handleChanging();
            return;// false;
        }
        // paranoia-testing:
        InstanceDescription localInstance = newView.getLocalInstance();
        if (localInstance==null) {
            throw new IllegalStateException("newView does not contain the local instance - hence cannot be current");
        }
        if (!localInstance.isLocal()) {
            throw new IllegalStateException("newView's local instance is not isLocal - very unexpected - hence cannot be current");
        }
        logger.debug("handleNewView: newView is current, so trying with minEventDelayHandler...");
        if (minEventDelayHandler!=null) {
            if (minEventDelayHandler.handlesNewView(newView)) {
                return;// true;
            }
        }
        logger.debug("handleNewView: minEventDelayHandler not set or not applicable this time, invoking hanldeNewViewNonDelayed...");
        /*return */handleNewViewNonDelayed(newView);
    }

    boolean handleNewViewNonDelayed(final BaseTopologyView newView) {
        logger.trace("handleNewViewNonDelayed: start");
        lock.lock();
        try{
            logger.debug("handleNewViewNonDelayed: start, newView={}", newView);
            if (!newView.isCurrent()) {
                logger.error("handleNewViewNonDelayed: newView must be current");
                throw new IllegalArgumentException("newView must be current");
            }
            modCnt++;
            
            if (!isChanging) {
                // verify if there is actually a change between previousView and newView
                // if there isn't, then there is not much point in sending a CHANGING/CHANGED tuple
                // at all
                if (previousView!=null && previousView.equals(newView)) {
                    // then nothing to send - the view has not changed, and we haven't
                    // sent the CHANGING event - so we should not do anything here
                    logger.debug("handleNewViewNonDelayed: we were not in changing state and new view matches old, so - ignoring");
                    return false;
                }
                if (previousView==null || !isPropertiesDiff(newView)) {
                    logger.debug("handleNewViewNonDelayed: implicitly triggering a handleChanging as we were not in changing state");
                    handleChanging();
                    logger.debug("handleNewViewNonDelayed: implicitly triggering of a handleChanging done");
                }
            }
                
            if (!activated) {
                // then all we can do is to pass this on to previoueView
                logger.trace("handleNewViewNonDelayed: setting previousView to {}", newView);
                previousView = newView;
                // plus set the isChanging flag to false
                logger.trace("handleNewViewNonDelayed: setting isChanging to false");
                isChanging = false;
                
                // other than that, we can't currently send any event, before activate
                logger.debug("handleNewViewNonDelayed: not yet activated - ignoring");
                return true;
            }
            
            // now check if the view indeed changed or if it was just the properties
            if (!isChanging && isPropertiesDiff(newView)) {
                // well then send a properties changed event only
                // and that one does not go via consistencyservice
                logger.info("handleNewViewNonDelayed: properties changed to: "+newView);
                previousView.setNotCurrent();
                enqueueForAll(eventListeners, EventFactory.newPropertiesChangedEvent(previousView, newView));
                logger.trace("handleNewViewNonDelayed: setting previousView to {}", newView);
                previousView = newView;
                return true;
            }
            
            final boolean invokeConsistencyService;
            if (consistencyService==null) {
                logger.debug("handleNewViewNonDelayed: no consistencyService set - continuing directly.");
                invokeConsistencyService = false;
            } else if (previousView==null) {
                // when there was no previous view, we cannot determine if
                // any instance left
                // so for safety reason: always invoke the consistencyservice
                logger.debug("handleNewViewNonDelayed: no previousView set - invoking consistencyService");
                invokeConsistencyService = true;
            } else {
                final InstancesDiff diff = new InstancesDiff(previousView, newView);
                InstanceCollection removed = diff.removed();
//                Collection<InstanceDescription> c = removed.get();
//                Iterator<InstanceDescription> it = c.iterator();
//                while(it.hasNext()) {
//                    logger.info("handleNewViewNonDelayed: removed: "+it.next());
//                }
                InstanceCollection inClusterView = removed.
                        isInClusterView(newView.getLocalInstance().getClusterView());
//                c = removed.get();
//                it = c.iterator();
//                while(it.hasNext()) {
//                    logger.info("handleNewViewNonDelayed: inClusterView: "+it.next());
//                }
                final boolean anyInstanceLeftLocalCluster = inClusterView.
                        get().size()>0;
                if (anyInstanceLeftLocalCluster) {
                    logger.debug("handleNewViewNonDelayed: anyInstanceLeftLocalCluster=true, hence invoking consistencyService next");
                } else {
                    logger.debug("handleNewViewNonDelayed: anyInstanceLeftLocalCluster=false - continuing directly.");
                }
                invokeConsistencyService = anyInstanceLeftLocalCluster;
            }
                        
            if (invokeConsistencyService) {
                // if "instances from the local cluster have been removed"
                // then:
                // run the set consistencyService
                final int lastModCnt = modCnt;
                logger.debug("handleNewViewNonDelayed: invoking consistencyService (modCnt={})", modCnt);
                consistencyService.sync(newView,
                        new Runnable() {
                    
                    public void run() {
                        logger.trace("consistencyService.callback.run: start. acquiring lock...");
                        lock.lock();
                        try{
                            logger.debug("consistencyService.callback.run: lock aquired. (modCnt should be {}, is {})", lastModCnt, modCnt);
                            if (modCnt!=lastModCnt) {
                                logger.debug("consistencyService.callback.run: modCnt changed (from {} to {}) - ignoring",
                                        lastModCnt, modCnt);
                                return;
                            }
                            // else:
                            doHandleConsistent(newView);
                        } finally {
                            lock.unlock();
                            logger.trace("consistencyService.callback.run: end.");
                        }
                    }
                    
                });
            } else {
                // otherwise we're either told not to use any ConsistencyService
                // or using it is not applicable at this stage - so continue
                // with sending the TOPOLOGY_CHANGED (or TOPOLOGY_INIT if there
                // are any newly bound topology listeners) directly
                doHandleConsistent(newView);
            }
            logger.debug("handleNewViewNonDelayed: end");
            return true;
        } finally {
            lock.unlock();
            logger.trace("handleNewViewNonDelayed: finally");
        }
    }

    protected boolean isPropertiesDiff(BaseTopologyView newView) {
        if (previousView==null) {
            return false;
        }
        if (newView==null) {
            throw new IllegalArgumentException("newView must not be null");
        }
        if (previousView.getInstances().size()!=newView.getInstances().size()) {
            return false;
        }
        if (previousView.equals(newView)) {
            return false;
        }
        Set<String> newIds = new HashSet<String>();
        for(InstanceDescription newInstance : newView.getInstances()) {
            newIds.add(newInstance.getSlingId());
        }
        
        for(InstanceDescription oldInstance : previousView.getInstances()) {
            if (!newIds.contains(oldInstance.getSlingId())) {
                return false;
            }
        }
        return true;
    }

    private void doHandleConsistent(BaseTopologyView newView) {
        logger.trace("doHandleConsistent: start");
        
        // unset the isChanging flag
        logger.trace("doHandleConsistent: setting isChanging to false");
        isChanging = false;

        if (previousView==null) {
            // this is the first time handleNewTopologyView is called
            
            if (eventListeners.size()>0) {
                logger.info("doHandleConsistent: no previous view available even though listeners already got CHANGED event");
            } else {
                logger.debug("doHandleConsistent: no previous view and there are no event listeners yet. very quiet.");
            }

            // otherwise this is the normal case where there are uninitialized event listeners waiting below

        } else {
            logger.debug("doHandleConsistent: sending TOPOLOGY_CHANGED to initialized listeners");
            previousView.setNotCurrent();
            enqueueForAll(eventListeners, EventFactory.newChangedEvent(previousView, newView));
        }
        
        if (unInitializedEventListeners.size()>0) {
            // then there were bindTopologyEventListener calls coming in while
            // we were in CHANGING state - so we must send those the INIT they were 
            // waiting for oh so long
            logger.debug("doHandleConsistent: sending TOPOLOGY_INIT to uninitialized listeners ({})", 
                    unInitializedEventListeners.size());
            enqueueForAll(unInitializedEventListeners, EventFactory.newInitEvent(newView));
            eventListeners.addAll(unInitializedEventListeners);
            unInitializedEventListeners.clear();
        }
        
        logger.trace("doHandleConsistent: setting previousView to {}", newView);
        previousView = newView;
        logger.trace("doHandleConsistent: end");
    }

    /** get-hook for testing only! **/
    AsyncEventSender getAsyncEventSender() {
        return asyncEventSender;
    }
    
}
