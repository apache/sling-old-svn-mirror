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
package org.apache.sling.discovery.base.connectors.announcement;

import org.apache.sling.discovery.base.connectors.BaseConfig;
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

    private long lastPing = System.currentTimeMillis();

    private final Announcement announcement;
    
    private long firstPing = System.currentTimeMillis();

    private long backoffIntervalSeconds = -1;

    private final BaseConfig config;
    
    CachedAnnouncement(final Announcement announcement, final BaseConfig config) {
        this.announcement = announcement;
        this.config = config;
    }
    
    private long getConfiguredConnectorTimeout() {
        return config.getConnectorPingTimeout();
    }
    
    private long getConfiguredConnectorInterval() {
        return config.getConnectorPingInterval();
    }

    public final boolean hasExpired() {
        final long now = System.currentTimeMillis();
        final long diff = now-lastPing;
        if (diff<1000*getEffectiveHeartbeatTimeout()) {
            return false;
        } else {
            return true;
        }
    }
    
    public final long getLastPing() {
        return lastPing;
    }
    
    public final long getFirstPing() {
        return firstPing;
    }
    
    /** Returns the second until the next heartbeat is expected, otherwise the timeout will hit **/
    public final long getSecondsUntilTimeout() {
        final long now = System.currentTimeMillis();
        final long diff = now-lastPing;
        final long left = 1000*getEffectiveHeartbeatTimeout() - diff;
        return left/1000;
    }
    
    
    private final long getEffectiveHeartbeatTimeout() {
        final long configuredGoodwill = getConfiguredConnectorTimeout() - getConfiguredConnectorInterval();
        return Math.max(getConfiguredConnectorTimeout(), backoffIntervalSeconds + configuredGoodwill);
    }

    /** Registers a heartbeat event, and returns the new resulting backoff interval -
     * or 0 if no backoff is applicable yet.
     * @param incomingAnnouncement 
     * @return the new resulting backoff interval -
     * or 0 if no backoff is applicable yet.
     */
    final long registerPing(Announcement incomingAnnouncement, BaseConfig config) {
        lastPing = System.currentTimeMillis();
        announcement.registerPing(incomingAnnouncement);
        if (incomingAnnouncement.isInherited()) {
            // then we are the client, we inherited this announcement from the server
            // hence we have no power to do any backoff instructions towards the server
            // (since the server decides about backoff-ing). hence returning 0 here
            // but taking note of what the server instructed us in terms of backoff
            backoffIntervalSeconds = incomingAnnouncement.getBackoffInterval();
            logger.debug("registerPing: inherited announcement - hence returning 0");
            return 0;
        }
        if (incomingAnnouncement.getResetBackoff()) {
            // on resetBackoff we reset the firstHeartbeat and start 
            // from 0 again
            firstPing = lastPing;
            logger.debug("registerPing: got a resetBackoff - hence returning 0");
            return 0;
        }
        final long stableSince = lastPing - firstPing;
        final long numStableTimeouts = stableSince / (1000 * config.getConnectorPingTimeout());
        final long backoffFactor = Math.min(numStableTimeouts, config.getBackoffStableFactor());
        backoffIntervalSeconds = backoffFactor * config.getConnectorPingInterval();
        return backoffIntervalSeconds;
    }

    public final Announcement getAnnouncement() {
        return announcement;
    }

}
