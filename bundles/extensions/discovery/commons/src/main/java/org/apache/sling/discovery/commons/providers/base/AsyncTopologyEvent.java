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
package org.apache.sling.discovery.commons.providers.base;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SLING-4755 : encapsulates an event that yet has to be sent (asynchronously) for a particular listener **/
final class AsyncTopologyEvent implements AsyncEvent {
    
    static final Logger logger = LoggerFactory.getLogger(AsyncTopologyEvent.class);

    final TopologyEventListener listener;
    final TopologyEvent event;
    AsyncTopologyEvent(TopologyEventListener listener, TopologyEvent event) {
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
        return "an AsyncTopologyEvent[event="+event+", listener="+listener+"]";
    }

    /** Actual sending of the asynchronous event - catches RuntimeExceptions a listener can send. (Error is caught outside) **/
    public void trigger() {
        logger.trace("trigger: start");
        try{
            logger.debug("trigger: sending to listener: {}, event: {}", listener, event);
            listener.handleTopologyEvent(event);
        } catch(final Exception e) {
            logger.warn("trigger: handler threw exception. handler: "+listener+", exception: "+e, e);
        }
        logger.trace("trigger: end: listener: {}, event: {}", listener, event);
    }

}