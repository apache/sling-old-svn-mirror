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
package org.apache.sling.discovery.commons;

import java.sql.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This listener facade applies a 'startup delay' to a topology event handler,
 * that is, it allows to specify a startup time during which any topology events
 * will be queued and processing only starts after this time.
 * <p>
 * What happens aFter the startup time depends on what was received during the delay:
 * <ul>
 *  <li>if no events were received then this is a no-op</li>
 *  <li>if the last event received was a CHANGING then this facade
 *      waits until it receives the next non-CHANGING event (which should
 *      be the very next), to then simulate an INIT event
 *      (as the discovery API says the first event received is an INIT event).
 *      </li>
 *  <li>if the last event received was not a CHANGING event
 *      (ie it was an INIT, CHANGED or PROPERTIES), then 
 *      as soon as the startup time passes this facade will simulate
 *      an INIT event
 *      (again, as the discovery API says the first event received is an INIT event)
 *      </li>
 * </ul>
 * Note that users of this facade must call dispose to avoid any async calls
 * to the delegate after startup, in case they themselves are deactivated!
 * @since 1.1.0
 */
public class InitDelayingTopologyEventListener implements TopologyEventListener {

    /** the logger used by this listener - can be set in the constructor optionally to fit better into user log structure */
    private final Logger logger;

    /** the delegate which does the actual event processing after delaying **/
    private final TopologyEventListener delegate;

    /** the sync object used to guard asynchronous discovery events and scheduler callback **/
    private final Object syncObj = new Object();

    /** the guarded flag indicating whether this listener is active or disposed - used between scheduler callback and dispose **/
    private final AtomicBoolean active = new AtomicBoolean(false);

    /** flag indicating whether we're still delaying or not **/
    private boolean delaying = true;
    
    /** the last pending delayed event - we only have to keep the last event to support all different cases **/
    private TopologyEvent pendingDelayedEvent = null;

    /** 
     * Creates a new init-delaying listener with the given delay, delegate and scheduler.
     * <p>
     * For properly disposing the caller should use the dispose method!
     * @see #dispose()
     */
    public InitDelayingTopologyEventListener(final long startupDelay, final TopologyEventListener delegate, final Scheduler scheduler) {
        this(startupDelay, delegate, scheduler, null);
    }

    /**
     * Creates a new init-delaying listener with the given delay, delegate, scheduler and optinoal logger.
     * <p>
     * For properly disposing the caller should use the dispose method!
     * @see #dispose()
     */
    public InitDelayingTopologyEventListener(final long startupDelay, final TopologyEventListener delegate, 
            final Scheduler scheduler, final Logger loggerOrNull) {
        if ( scheduler == null ) {
            throw new IllegalArgumentException("scheduler must not be null");
        }
        if ( delegate == null ) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        if ( startupDelay <= 0) {
            throw new IllegalArgumentException("startupDelay must be greater than 0, not " + startupDelay);
        }
        this.delegate = delegate;
        if ( loggerOrNull != null ) {
            this.logger = loggerOrNull;
        } else {
            this.logger = LoggerFactory.getLogger(this.getClass());
        }
        final Runnable r = new Runnable() {
            
            @Override
            public void run() {
                if (InitDelayingTopologyEventListener.this.active.get()) {
                    // only call afterStartupDelay if we're active
                    // (to avoid this call if disposed in the meantime)
                    
                    // and since after disposing this listener is no longer
                    // used - ie it is a throw-away - you can create
                    // such a listener on each activate and dispose it on
                    // deactivate and you'll be fine.

                    // in any case - time for calling afterStartupDelay here:
                    afterStartupDelay();
                }
            }
        };
        
        // mark this listener as active
        this.active.set(true);

        // schedule me if you can
        if ( !scheduler.schedule(r, scheduler.AT(new Date(System.currentTimeMillis() + startupDelay * 1000))) ) {
            // if for whatever reason scheduling doesn't work, let's run now
            logger.warn("activate: could not schedule startupDelay handler with scheduler ({}) - "
                    + "thus starting ({}) immediately", scheduler, delegate);
            r.run();
        }
        // SLING-5560 : at this point either r is invoked immediately or scheduled after the delay
    }
    
    @Override
    public void handleTopologyEvent(TopologyEvent event) {
        synchronized ( syncObj ) {
            if ( this.delaying ) {
                // when we're delaying, keep hold of the last event
                // as action afterStartupDelay depends on this last
                // event
                this.logger.debug("handleTopologyEvent: delaying processing of received topology event (startup delay active) {}", event);
                this.pendingDelayedEvent = event;
                
                // and we're delaying - so stop processing now and return
                return;
            } else if ( this.pendingDelayedEvent != null ) {
                // this means that while we're no longer delaying we still
                // have a pending delayed event to process.
                // more concretely, it means that this event is a CHANGING
                // event (as the others are treated via an artificial INIT event
                // in afterStartupDelay).
                // which means that we must now convert the new event into an INIT
                // to ensure our code gets an INIT first thing
                
                // paranoia check:
                if ( event.getType() == Type.TOPOLOGY_CHANGING ) {
                    // this should never happen - but if it does, rinse and repeat
                    this.pendingDelayedEvent = event;
                    this.logger.info("handleTopologyEvent: ignoring received topology event of type CHANGING {}", event);
                    return;
                } else {
                    // otherwise we now **convert** the event to an init event
                    this.pendingDelayedEvent = null;
                    this.logger.debug("handleTopologyEvent: first stable topology event received after startup delaying. "
                            + "Simulating an INIT event with this new view: {}", event);
                    event = new TopologyEvent(Type.TOPOLOGY_INIT, null, event.getNewView());
                }
            } else {
                // otherwise we're no longer delaying nor do we have a pending-backlog.
                // we can just pass the event through
                this.logger.debug("handleTopologyEvent: received topology event {}", event);
            }
        }
        // no delaying applicable - call delegate
        this.delegate.handleTopologyEvent(event);
    }
    
    /**
     * Marks this listener as no longer active - ensures that it doesn't call the delegate
     * via any potentially pending scheduler callback.
     * <p>
     * Note that after dispose you can *still* call handleTopologyEvent and the events
     * are passed to the delegate - but those are expected to be 'late' events and not 
     * really part of the normal game. Hence, the caller must also ensure that the 
     * handleTopologyEvent method isn't called anymore (which typically is automatically
     * guaranteed since the caller is typically an osgi service that gets unregistered anyway)
     */
    public void dispose() {
        this.active.set(false);
    }

    /**
     * Called via the scheduler callback when the startup delay has passed.
     * <p>
     * Hence only called once!
     */
    private void afterStartupDelay() {
        synchronized ( this.syncObj ) {
            // stop any future delaying
            this.delaying = false;
            
            if ( this.pendingDelayedEvent == null ) {
                // if no event received while we delayed,
                // then we don't have to do anything later
                this.logger.debug("afterStartupDelay: startup delay passed without any events delayed. "
                        + "So, ready for first upcoming INIT event");
            } else if ( this.pendingDelayedEvent.getType() == Type.TOPOLOGY_CHANGING ) {
                // if the last delayed event was CHANGING
                // then we must convert the next upcoming CHANGED, PROPERTIES
                // into an INIT
                
                // and the way this is done in this class is by leving this
                // event sit in this.pendingDelayedEvent, for grabs in handleTopologyEvent later
                this.logger.debug("afterStartupDelay: startup delay passed, pending delayed event was CHANGING. "
                        + "Waiting for next stable topology event");
            } else {
                // otherwise the last delayed event was either an INIT, CHANGED or PROPERTIES
                // - but in any case we definitely never 
                // processed any INIT - and our code expects an INIT
                // as the first event ever..
                
                // so we now convert the event into an INIT
                final TopologyEvent artificialInitEvent = 
                        new TopologyEvent(Type.TOPOLOGY_INIT, null, this.pendingDelayedEvent.getNewView());
                
                this.logger.debug("afterStartupDelay: startup delay passed, last pending delayed event was stable ({}). "
                        + "Simulating an INIT event with that view: {}", this.pendingDelayedEvent, artificialInitEvent); 
                this.pendingDelayedEvent = null;
                
                // call the delegate.
                // we must do this call in the synchronized block
                // to ensure any concurrent new event waits properly
                // before the INIT is done
                delegate.handleTopologyEvent(artificialInitEvent);
            }
        }
    }
}
