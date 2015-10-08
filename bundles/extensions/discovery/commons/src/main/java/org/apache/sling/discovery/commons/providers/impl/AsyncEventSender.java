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

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
final class AsyncEventSender implements Runnable {
    
    static final Logger logger = LoggerFactory.getLogger(AsyncEventSender.class);

    /** stopped is always false until flushThenStop is called **/
    private boolean stopped = false;

    /** eventQ contains all AsyncEvent objects that have yet to be sent - in order to be sent **/
    private final List<AsyncEvent> eventQ = new LinkedList<AsyncEvent>();
    
    /** flag to track whether or not an event is currently being sent (but already taken off the Q **/
    private boolean isSending = false;
    
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
                        isSending = false;
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
                        isSending = asyncEvent!=null;
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
        logger.trace("sendTopologyEvent: start");
        final TopologyEventListener listener = asyncEvent.listener;
        final TopologyEvent event = asyncEvent.event;
        try{
            logger.debug("sendTopologyEvent: sending to listener: {}, event: {}", listener, event);
            listener.handleTopologyEvent(event);
        } catch(final Exception e) {
            logger.warn("sendTopologyEvent: handler threw exception. handler: "+listener+", exception: "+e, e);
        }
        logger.trace("sendTopologyEvent: start: listener: {}, event: {}", listener, event);
    }

    /** for testing only: checks whether there are any events being queued or sent **/
    boolean hasInFlightEvent() {
        synchronized(eventQ) {
            return isSending || !eventQ.isEmpty();
        }
    }
    
}