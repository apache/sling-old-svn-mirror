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

import org.apache.sling.discovery.TopologyView;

/**
 * Very simple abstract base class for the TopologyView which
 * comes with the 'setNotCurrent()' method - that allows the
 * ViewStateManager to mark a topologyView as no longer current
 * - and the isCurrent() is handled accordingly.
 */
public abstract class BaseTopologyView implements TopologyView {

    /** Whether or not this topology is considered 'current' / ie currently valid **/
    private volatile boolean current = true;
    
    /**
     * {@inheritDoc}
     */
    public boolean isCurrent() {
        return current;
    }
    
    /**
     * Marks this view as no longer current - this typically
     * results in a TOPOLOGY_CHANGING event to be sent.
     * <p>
     * Note that once marked as not current, it can no longer
     * be reverted to current==true
     */
    public void setNotCurrent() {
        current = false;
    }

    /**
     * Returns the id that shall be used in the syncToken
     * by the ConsistencyService.
     * <p>
     * The clusterSyncId uniquely identifies each change
     * of the local cluster for all participating instances. 
     * That means, all participating instances know of the 
     * clusterSyncId and it is the same for all instances.
     * Whenever an instance joins/leaves the cluster, this
     * clusterSyncId must change. 
     * <p>
     * Since this method returns the *local* clusterSyncId,
     * it doesn't care if a remote cluster experienced
     * changes - it must only change when the local cluster changes.
     * However, it *can* change when a remote cluster changes too.
     * So the requirement is just that it changes *at least* when
     * the local cluster changes - but implementations
     * can opt to regard this rather as a TopologyView-ID too
     * (ie an ID that identifies a particular incarnation
     * of the TopologyView for all participating instances
     * in the whole topology).
     * <p>
     * This id can further safely be used by the ConsistencyService
     * to identify a syncToken that it writes and that all
     * other instances in the lcoal cluster wait for, before
     * sending a TOPOLOGY_CHANGED event.
     * <p>
     * Note that this is obviously not to be confused
     * with the ClusterView.getId() which is stable throughout
     * the lifetime of a cluster.
     */
    public abstract String getLocalClusterSyncTokenId();

}
