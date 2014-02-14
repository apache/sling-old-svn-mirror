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

/**
 * With SLING-3389 the Announcement itself doesn't use the created
 * (ie timeout) field anymore (it still has it currently for backwards
 * compatibility on the wire-level) - hence that's why there's this
 * small in-memory wrapper object which contains an Announcement and 
 * carries a lastHeartbeat property.
 */
public class CachedAnnouncement {
    
    private long lastHeartbeat = System.currentTimeMillis();

    private final Announcement announcement;

    CachedAnnouncement(final Announcement announcement) {
        this.announcement = announcement;
    }
        
    public final long getLastHeartbeat() {
        return lastHeartbeat;
    }
    
    final void registerHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }

    public final Announcement getAnnouncement() {
        return announcement;
    }

}
