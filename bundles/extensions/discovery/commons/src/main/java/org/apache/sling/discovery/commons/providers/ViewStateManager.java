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

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyEventListener;

/**
 * The ViewStateManager is at the core of managing TopologyEventListeners,
 * the 'view state' (changing vs changed) and sending out the appropriate
 * and according TopologyEvents to the registered listeners - depending
 * on the implementation it also supports the ConsistencyService, which is
 * invoked on handleNewView.
 */
public interface ViewStateManager {

    /**
     * Installs an optional 'min event delay handler' which, using the given scheduler,
     * delays sending TOPOLOGY_CHANGED event after receiving a handleNewView - with the
     * idea as to limit the number of toggling between view states.
     */
    void installMinEventDelayHandler(DiscoveryService discoveryService, Scheduler scheduler, 
            long minEventDelaySecs);
    
    /** 
     * Binds the given eventListener, sending it an INIT event if applicable.
     * @param eventListener the eventListener that is to bind
     */
    void bind(TopologyEventListener eventListener);

    /** 
     * Unbinds the given eventListener, returning whether or not it was bound at all.
     * @param eventListener the eventListner that is to unbind
     * @return whether or not the listener was added in the first place 
     */
    boolean unbind(TopologyEventListener eventListener);

    /**
     * Handles activation - ie marks this manager as activated thus the TOPOLOGY_INIT
     * event can be sent to already bound listeners and subsequent calls to
     * handleChanging/handleNewView will result in according/appropriate TOPOLOGY_CHANGING/
     * TOPOLOGY_CHANGED events.
     */
    void handleActivated();

    /** 
     * Must be called when the corresponding service (typically a DiscoveryService implementation)
     * is deactivated.
     * <p>
     * Will mark this manager as deactivated and flags the last available view as not current.
     */
    void handleDeactivated();

    /**
     * Handles the fact that some (possibly early) indicator of a change in a topology
     * has been detected and that a new view is being agreed upon (whatever that means,
     * be it voting or similar).
     * <p>
     * Will send out TOPOLOGY_CHANGING to all initialized listeners.
     */
    void handleChanging();

    /**
     * Handles the fact that a new view became true/established and sends out
     * TOPOLOGY_INIT to uninitialized listeners and TOPOLOGY_CHANGED to already initialized
     * listeners (in the latter case, also sends a TOPOLOGY_CHANGING if that has not yet been 
     * done)
     * @param newView the new, established view
     * @return false if the newView was the same as previous and we were not in 'changing' mode,
     * true if we were either in changing mode or the newView was different from the previous one.
     */
    void handleNewView(BaseTopologyView newView);

}