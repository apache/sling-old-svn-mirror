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
package org.apache.sling.discovery.impl.topology.announcement;

import org.apache.sling.discovery.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With SLING-3389 the Announcement itself doesn't use the created
 * (ie timeout) field anymore (it still has it currently for backwards
 * compatibility on the wire-level) - hence that's why there's this
 * small in-memory wrapper object which contains an Announcement and 
 * carries a lastHeartbeat property.
 */
public class CachedAnnouncement {
    
    private final static Logger logger = LoggerFactory.getLogger(CachedAnnouncement.class);

    private long lastHeartbeat = System.currentTimeMillis();

    private final Announcement announcement;
    
    private long firstHeartbeat = System.currentTimeMillis();

    private long backoffIntervalSeconds = -1;

    private final long configuredHeartbeatTimeout;

    private final long configuredHeartbeatInterval;
    
    CachedAnnouncement(final Announcement announcement, final Config config) {
        this.announcement = announcement;
        this.configuredHeartbeatTimeout = config.getHeartbeatTimeout();
        this.configuredHeartbeatInterval = config.getHeartbeatInterval();
    }

    public final boolean hasExpired() {
        final long now = System.currentTimeMillis();
        final long diff = now-lastHeartbeat;
        if (diff<1000*getEffectiveHeartbeatTimeout()) {
            return false;
        } else {
            return true;
        }
    }
    
    public final long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    /** Returns the second until the next heartbeat is expected, otherwise the timeout will hit **/
    public final long getSecondsUntilTimeout() {
        final long now = System.currentTimeMillis();
        final long diff = now-lastHeartbeat;
        final long left = 1000*getEffectiveHeartbeatTimeout() - diff;
        return left/1000;
    }
    
    
    private final long getEffectiveHeartbeatTimeout() {
        final long configuredGoodwill = configuredHeartbeatTimeout - configuredHeartbeatInterval;
        return Math.max(configuredHeartbeatTimeout, backoffIntervalSeconds + configuredGoodwill);
    }

    /** Registers a heartbeat event, and returns the new resulting backoff interval -
     * or 0 if no backoff is applicable yet.
     * @param incomingAnnouncement 
     * @return the new resulting backoff interval -
     * or 0 if no backoff is applicable yet.
     */
    final long registerHeartbeat(Announcement incomingAnnouncement, Config config) {
        lastHeartbeat = System.currentTimeMillis();
        if (incomingAnnouncement.isInherited()) {
            // then we are the client, we inherited this announcement from the server
            // hence we have no power to do any backoff instructions towards the server
            // (since the server decides about backoff-ing). hence returning 0 here
            // but taking note of what the server instructed us in terms of backoff
            backoffIntervalSeconds = incomingAnnouncement.getBackoffInterval();
            logger.debug("registerHeartbeat: inherited announcement - hence returning 0");
            return 0;
        }
        if (incomingAnnouncement.getResetBackoff()) {
            // on resetBackoff we reset the firstHeartbeat and start 
            // from 0 again
            firstHeartbeat = lastHeartbeat;
            logger.debug("registerHeartbeat: got a resetBackoff - hence returning 0");
            return 0;
        }
        final long stableSince = lastHeartbeat - firstHeartbeat;
        final long numStableTimeouts = stableSince / config.getHeartbeatTimeoutMillis();
        final long backoffFactor = Math.min(numStableTimeouts, config.getBackoffStableFactor());
        backoffIntervalSeconds = backoffFactor * config.getHeartbeatInterval();
        return backoffIntervalSeconds;
    }

    public final Announcement getAnnouncement() {
        return announcement;
    }

}
