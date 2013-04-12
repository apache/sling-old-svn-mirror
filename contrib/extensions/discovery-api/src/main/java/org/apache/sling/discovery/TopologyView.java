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

import java.util.Collection;
import java.util.List;

/**
 * A topology view is a cross-cluster list of instances and clusters
 * that have announced themselves with the DiscoveryService.
 *
 */
public interface TopologyView {

	/**
	 * Checks if this TopologyView is currently valid - or if the
	 * service knows of a topology change just going on (or another
	 * uncertainty about the topology such as IOException etc)
	 * @return true if this TopologyView is currently valid, false
	 * if the service knows of a topology change just going on (or
	 * another issue with discovery like IOException etc)
	 */
	boolean isCurrent();

	/**
	 * Provides the InstanceDescription belonging to *this* instance.
	 * @return the InstanceDescription belonging to *this* instance
	 */
	InstanceDescription getOwnInstance();

    /**
     * Provides the list of InstanceDescriptions ordered by Sling Id.
     * @return the list of InstanceDescriptions ordered by Sling Id or null if there are no instances
     */
	List<InstanceDescription> getInstances();

	/**
	 * Search the current topology for instances which the provided InstancePicker has accepted.
	 * @param filter the filter to use
	 * @return the list of InstanceDescriptions which were accepted by the InstanceFilter
	 */
	List<InstanceDescription> findInstances(InstanceFilter filter);

    /**
     * Provides the collection of ClusterViews.
     * <p>
     * Note that all InstanceDescriptions belong to a ClusterView, even if 
     * they are only a "cluster of 1" (ie not really a cluster).
     * @return the collection of ClusterViews
     */
	Collection<ClusterView> getClusterViews();
}
