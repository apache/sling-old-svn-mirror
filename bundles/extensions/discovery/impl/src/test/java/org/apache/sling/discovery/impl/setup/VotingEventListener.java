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
package org.apache.sling.discovery.impl.setup;

import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VotingEventListener implements EventListener {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 
     */
    private final VirtualInstance instance;
    private final VotingHandler votingHandler;
    volatile boolean stopped = false;
    private final String slingId;
    private ConcurrentLinkedQueue<org.osgi.service.event.Event> q = new ConcurrentLinkedQueue<org.osgi.service.event.Event>();
    
    public VotingEventListener(VirtualInstance instance, final VotingHandler votingHandler, final String slingId) {
        this.instance = instance;
        this.votingHandler = votingHandler;
        this.slingId = slingId;
        Thread th = new Thread(new Runnable() {

            @Override
            public void run() {
                while(!stopped) {
                    try{
                        org.osgi.service.event.Event ev = q.poll();
                        if (ev==null) {
                            Thread.sleep(10);
                            continue;
                        }
                        logger.debug("async.run: delivering event to listener: "+slingId+", stopped: "+stopped+", event: "+ev);
                        votingHandler.handleEvent(ev);
                    } catch(Exception e) {
                        logger.error("async.run: got Exception: "+e, e);
                    }
                }
            }
            
        });
        th.setName("VotingEventListener-"+instance.getDebugName());
        th.setDaemon(true);
        th.start();
    }
    
    public void stop() {
        logger.debug("stop: stopping listener for slingId: "+slingId);
        stopped = true;
    }

    public void onEvent(EventIterator events) {
        if (stopped) {
            logger.info("onEvent: listener: "+slingId+" getting late events even though stopped: "+events.hasNext());
            return;
        }
        try {
            while (!stopped && events.hasNext()) {
                Event event = events.nextEvent();
                Properties properties = new Properties();
                String topic;
                if (event.getType() == Event.NODE_ADDED) {
                    topic = SlingConstants.TOPIC_RESOURCE_ADDED;
                } else if (event.getType() == Event.NODE_MOVED) {
                    topic = SlingConstants.TOPIC_RESOURCE_CHANGED;
                } else if (event.getType() == Event.NODE_REMOVED) {
                    topic = SlingConstants.TOPIC_RESOURCE_REMOVED;
                } else {
                    topic = SlingConstants.TOPIC_RESOURCE_CHANGED;
                }
                try {
                    properties.put("path", event.getPath());
                    org.osgi.service.event.Event osgiEvent = new org.osgi.service.event.Event(
                            topic, properties);
                    logger.debug("onEvent: enqueuing event to listener: "+slingId+", stopped: "+stopped+", event: "+osgiEvent);
                    q.add(osgiEvent);
                } catch (RepositoryException e) {
                    logger.warn("RepositoryException: " + e, e);
                }
            }
            if (stopped) {
                logger.info("onEvent: listener stopped: "+slingId+", pending events: "+events.hasNext());
            }
        } catch (Throwable th) {
            try {
                this.instance.dumpRepo();
            } catch (Exception e) {
                logger.info("onEvent: could not dump as part of catching a throwable, e="+e+", th="+th);
            }
            logger.error(
                    "Throwable occurred in onEvent: " + th, th);
        }
    }
}