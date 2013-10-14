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
package org.apache.sling.discovery;

import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * A ClusterView represents the instances of a cluster that are
 * up and running and that all can see each other at a certain point in time.
 * <p>
 * A ClusterView can also consist of just one single instance.
 */
@ProviderType
public interface ClusterView {

	/**
	 * Returns an id of this cluster view.
	 * <p>
	 * Note that this id is not guaranteed to be stable between
	 * instances of ClusterViews. The id is identifying only the
	 * current view of the cluster, not the cluster itself, and 
	 * thus if the view changes the id might (or might not) change.
	 * @return an id of this cluster view
	 * @deprecated due to the unstable nature of this id - it only
	 * identifies the current view of the cluster, not the cluster
	 * itself - this id has been deprecated. If an id of the cluster
	 * itself is needed, this must be dealt with on an application
	 * level - for example by letting the cluster leader define
	 * such an id and taking care of special cases like cluster joins.
	 * @see <a href="http://issues.apache.org/jira/browse/SLING-3164">SLING-3164</a>
	 */
    String getId();

    /**
     * Provides the list of InstanceDescriptions with a stable ordering.
     * <p>
     * Stable ordering implies that unless an instance leaves the cluster
     * (due to shutdown/crash/network problems) the instance keeps the
     * relative position in the list.
     * @return the list of InstanceDescriptions (with a stable ordering)
     */
    List<InstanceDescription> getInstances();

	/**
	 * Provides the InstanceDescription belonging to the leader instance.
	 * <p>
	 * Every ClusterView is guaranteed to have one and only one leader.
	 * <p>
	 * The leader is stable: once a leader is elected it stays leader
	 * unless it leaves the cluster (due to shutdown/crash/network problems)
	 * @return the InstanceDescription belonging to the leader instance
	 */
    InstanceDescription getLeader();
}
