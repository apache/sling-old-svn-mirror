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

import java.util.Collection;

import org.apache.sling.discovery.InstanceDescription;

/**
 * The announcement registry keeps track of all the announcement that this
 * instance either received by a joined topology connector or that a topology
 * connector inherited from the counterpart (the topology connector servlet)
 */
public interface AnnouncementRegistry {

    enum ListScope {
        OnlyInherited, // lists only announcements that were inherited from a
                       // TopologyConnectorServlet by a client
        AllLocally, // lists all announcements that were received by the local
                    // instance
        AllInSameCluster // lists all announcements - also those that were
                         // received by other instances in the same cluster
    }

    /** Register the given announcement **/
    boolean registerAnnouncement(Announcement topologyAnnouncement);

    /** List all announcements that match the given scope **/
    Collection<Announcement> listAnnouncements(ListScope scope);

    /** Check for expired announcements and remove any if applicable **/
    void checkExpiredAnnouncements();

    /** Returns the list of instances contained in all non-expired announcements of this registry **/
    Collection<InstanceDescription> listInstances();

    /** Add all registered announcements to the given target announcement that are accepted by the given filter **/
    void addAllExcept(Announcement target, AnnouncementFilter filter);

    /** Unregister the announcement owned by the given slingId **/
    void unregisterAnnouncement(String ownerId);

}
