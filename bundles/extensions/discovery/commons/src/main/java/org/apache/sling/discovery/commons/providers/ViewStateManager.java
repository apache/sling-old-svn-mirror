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
package org.apache.sling.discovery.commons.providers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ViewStateManager is at the core of managing TopologyEventListeners,
 * the 'view state' (changing vs changed) and sending out the appropriate
 * and according TopologyEvents to the registered listeners.
 * <p>
 * Note that this class is completely unsynchronized and the idea is that
 * (since this class is only of interest to other implementors/providers of 
 * the discovery.api, not to users of the discovery.api however) that those
 * other implementors take care of proper synchronization. Without synchronization
 * this class is prone to threading issues! The most simple form of 
 * synchronization is to synchronize all of the public (non-static..) methods
 * with the same monitor object.
 */
public class ViewStateManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /** 
     * List of bound listeners that have already received their INIT event - others are in unInitializedEventListeners.
     * @see ViewStateManager#unInitializedEventListeners
     */
    private List<TopologyEventListener> eventListeners = new ArrayList<TopologyEventListener>();

    /**
     * List of bound listeners that have not yet received their TOPOLOGY_INIT event - 
     * once they are sent the TOPOLOGY_INIT event they are moved to eventListeners (and stay there).
     * <p>
     * This list becomes necessary for cases where the bind() happens before activate, or after activate but at a time
     * when the topology is TOPOLOGY_CHANGING - at which point an TOPOLOGY_INIT event can not yet be sent.
     * @see ViewStateManager#eventListeners
     */
    private List<TopologyEventListener> unInitializedEventListeners = new ArrayList<TopologyEventListener>();
    
    /** 
     * Set true when the bundle.activate() was called, false if not yet or when deactivate() is called.
     * <p>
     * This controls whether handleChanging() and handleNewView() should cause any events
     * to be sent - which they do not if called before handleActivated() (or after handleDeactivated())
     * @see ViewStateManager#handleActivated()
     * @see ViewStateManager#handleChanging()
     * @see ViewStateManager#handleNewView(BaseTopologyView)
     * @see ViewStateManager#handleDeactivated()
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
     * Binds the given eventListener, sending it an INIT event if applicable.
     * <p>
     * Note: no synchronization done in ViewStateManager, <b>must</b> be done externally
     * @param eventListener the eventListener that is to bind
     */
    public void bind(final TopologyEventListener eventListener) {

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
                unInitializedEventListeners.add(eventListener);
            } else {
                // otherwise we can send the TOPOLOGY_INIT now
                sendEvent(eventListener, newInitEvent(previousView));
                eventListeners.add(eventListener);
            }
        } else {
            unInitializedEventListeners.add(eventListener);
        }
    }
    
    /** 
     * Unbinds the given eventListener, returning whether or not it was bound at all.
     * <p>
     * Note: no synchronization done in ViewStateManager, <b>must</b> be done externally
     * @param eventListener the eventListner that is to unbind
     * @return whether or not the listener was added in the first place 
     */
    public boolean unbind(final TopologyEventListener eventListener) {

        logger.debug("unbind: Releasing TopologyEventListener {}",
                eventListener);

        // even though a listener must always only ever exist in one of the two,
        // the unbind we do - for safety-paranoia-reasons - remove them from both
        final boolean a = eventListeners.remove(eventListener);
        final boolean b = unInitializedEventListeners.remove(eventListener);
        return a || b;
    }
    
    /** Internal helper method that sends a given event to a list of listeners **/
    private void sendEvent(final List<TopologyEventListener> audience, final TopologyEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("sendEvent: sending topologyEvent {}, to all ({}) listeners", event, audience.size());
        }
        for (Iterator<TopologyEventListener> it = audience.iterator(); it.hasNext();) {
            TopologyEventListener topologyEventListener = it.next();
            sendEvent(topologyEventListener, event);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("sendEvent: sent topologyEvent {}, to all ({}) listeners", event, audience.size());
        }
    }
    
    /** Internal helper method that sends a given event to a particular listener **/
    private void sendEvent(final TopologyEventListener da, final TopologyEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("sendEvent: sending topologyEvent {}, to {}", event, da);
        }
        try{
            da.handleTopologyEvent(event);
        } catch(final Exception e) {
            logger.warn("sendEvent: handler threw exception. handler: "+da+", exception: "+e, e);
        }
    }

    /** Note: no synchronization done in ViewStateManager, <b>must</b> be done externally **/
    public void handleActivated() {
        logger.debug("handleActivated: activating the ViewStateManager");
        activated = true;
        
        if (previousView!=null && !isChanging) {
            sendEvent(unInitializedEventListeners, newInitEvent(previousView));
            eventListeners.addAll(unInitializedEventListeners);
            unInitializedEventListeners.clear();
        }
        logger.debug("handleActivated: activated the ViewStateManager");
    }

    /** Simple factory method for creating a TOPOLOGY_INIT event with the given newView **/
    public static TopologyEvent newInitEvent(final BaseTopologyView newView) {
        if (newView==null) {
            throw new IllegalStateException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            throw new IllegalStateException("newView must be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_INIT, null, newView);
    }
    
    /** Simple factory method for creating a TOPOLOGY_CHANGING event with the given oldView **/
    public static TopologyEvent newChangingEvent(final BaseTopologyView oldView) {
        if (oldView==null) {
            throw new IllegalStateException("oldView must not be null");
        }
        if (oldView.isCurrent()) {
            throw new IllegalStateException("oldView must not be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_CHANGING, oldView, null);
    }
    
    /** Simple factory method for creating a TOPOLOGY_CHANGED event with the given old and new views **/
    public static TopologyEvent newChangedEvent(final BaseTopologyView oldView, final BaseTopologyView newView) {
        if (oldView==null) {
            throw new IllegalStateException("oldView must not be null");
        }
        if (oldView.isCurrent()) {
            throw new IllegalStateException("oldView must not be current");
        }
        if (newView==null) {
            throw new IllegalStateException("newView must not be null");
        }
        if (!newView.isCurrent()) {
            throw new IllegalStateException("newView must be current");
        }
        return new TopologyEvent(Type.TOPOLOGY_CHANGED, oldView, newView);
    }

    /** 
     * Must be called when the corresponding service (typically a DiscoveryService implementation)
     * is deactivated.
     * <p>
     * Will mark this manager as deactivated and flags the last available view as not current.
     * <p>
     * Note: no synchronization done in ViewStateManager, <b>must</b> be done externally 
     */
    public void handleDeactivated() {
        logger.debug("handleDeactivated: deactivating the ViewStateManager");
        activated = false;

        if (previousView!=null) {
            previousView.setNotCurrent();
            previousView = null;
        }
        isChanging = false;
        
        eventListeners.clear();
        unInitializedEventListeners.clear();
        logger.debug("handleDeactivated: deactivated the ViewStateManager");
    }
    
    /** Note: no synchronization done in ViewStateManager, <b>must</b> be done externally **/
    public void handleChanging() {
        logger.debug("handleChanging: start");

        if (isChanging) {
            // if isChanging: then this is no news
            // hence: return asap
            logger.debug("handleChanging: was already changing - ignoring.");
            return;
        }
        
        // whether activated or not: set isChanging to true now
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
        sendEvent(eventListeners, newChangingEvent(previousView));
        logger.debug("handleChanging: end");
    }
    
    /** Note: no synchronization done in ViewStateManager, <b>must</b> be done externally **/
    public void handleNewView(BaseTopologyView newView) {
        if (logger.isDebugEnabled()) {
            logger.debug("handleNewView: start, newView={}", newView);
        }
        if (!newView.isCurrent()) {
            logger.error("handleNewView: newView must be current");
            throw new IllegalArgumentException("newView must be current");
        }
        
        if (!isChanging) {
            // verify if there is actually a change between previousView and newView
            // if there isn't, then there is not much point in sending a CHANGING/CHANGED tuple
            // at all
            if (previousView!=null && previousView.equals(newView)) {
                // then nothing to send - the view has not changed, and we haven't
                // sent the CHANGING event - so we should not do anything here
                logger.debug("handleNewView: we were not in changing state and new view matches old, so - ignoring");
                return;
            }
            logger.debug("handleNewView: simulating a handleChanging as we were not in changing state");
            handleChanging();
            logger.debug("handleNewView: simulation of a handleChanging done");
        }

        // whether activated or not: set isChanging to false, first thing
        isChanging = false;
        
        if (!activated) {
            // then all we can do is to pass this on to previoueView
            previousView = newView;
            // other than that, we can't currently send any event, before activate
            logger.debug("handleNewView: not yet activated - ignoring");
            return;
        }
        
        if (previousView==null) {
            // this is the first time handleNewTopologyView is called
            
            if (eventListeners.size()>0) {
                logger.info("handleNewTopologyView: no previous view available even though listeners already got CHANGED event");
            }
            
            // otherwise this is the normal case where there are uninitialized event listeners waiting below
                
        } else {
            logger.debug("handleNewView: sending TOPOLOGY_CHANGED to initialized listeners");
            previousView.setNotCurrent();
            sendEvent(eventListeners, newChangedEvent(previousView, newView));
        }
        
        if (unInitializedEventListeners.size()>0) {
            // then there were bindTopologyEventListener calls coming in while
            // we were in CHANGING state - so we must send those the INIT they were 
            // waiting for oh so long
            if (logger.isDebugEnabled()) {
                logger.debug("handleNewView: sending TOPOLOGY_INIT to uninitialized listeners ({})", 
                        unInitializedEventListeners.size());
            }
            sendEvent(unInitializedEventListeners, newInitEvent(newView));
            eventListeners.addAll(unInitializedEventListeners);
            unInitializedEventListeners.clear();
        }
        
        previousView = newView;
        logger.debug("handleNewView: end");
    }
    
}
