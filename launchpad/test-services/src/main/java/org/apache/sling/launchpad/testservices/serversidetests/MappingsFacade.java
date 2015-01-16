/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.serversidetests;

import javax.jcr.Session;

import org.apache.sling.launchpad.testservices.events.EventsCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Facade for saving mappings and waiting for the corresponding events */
public class MappingsFacade {

    public static final String MAPPING_EVENT_TOPIC = "org/apache/sling/api/resource/ResourceResolverMapping/CHANGED";
    private static final Logger logger = LoggerFactory.getLogger(MappingsFacade.class);
    private final EventsCounter eventsCounter;
    private static boolean firstInstance = true;
    
    // How long to wait for mapping updates
    public static final String MAPPING_UPDATE_TIMEOUT_MSEC = "ResourceResolverTest.mapping.update.timeout.msec";
    private static final long updateTimeout = Long.valueOf(System.getProperty(MAPPING_UPDATE_TIMEOUT_MSEC, "10000"));

    public MappingsFacade(EventsCounter c) {
        if(firstInstance) {
            logger.info("updateTimeout = {}, use {} system property to change", updateTimeout, MAPPING_UPDATE_TIMEOUT_MSEC);
            firstInstance = false;
        }
        eventsCounter = c;
    }
    
    /** Save a Session that has mapping changes, and wait for the OSGi event
     *  that signals that mappings have been updated.
     *  @return error message, null if ok
     */
    public String saveMappings(Session session) throws Exception {
        final int oldEventsCount = eventsCounter.getEventsCount(MAPPING_EVENT_TOPIC);
        logger.debug("Saving Session and waiting for event counter {} to change from current value {}", MAPPING_EVENT_TOPIC, oldEventsCount);
        session.save();
        final long timeout = System.currentTimeMillis() + updateTimeout;
        while(System.currentTimeMillis() < timeout) {
            final int newCount = eventsCounter.getEventsCount(MAPPING_EVENT_TOPIC); 
            if(newCount != oldEventsCount) {
                // Sleeping here shouldn't be needed but it looks
                // like mappings are not immediately updated once the event arrives
                logger.debug("Event counter {} is now {}", MAPPING_EVENT_TOPIC, newCount);
                Thread.sleep(updateTimeout / 50);
                return null;
            }
            try {
                Thread.sleep(10);
            } catch(InterruptedException ignore) {
            }
        }
        return "Timeout waiting for " + MAPPING_EVENT_TOPIC + " event, after " + updateTimeout + " msec";
    }
}
