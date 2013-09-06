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

import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * A topology view is a cross-cluster list of instances and clusters
 * that have announced themselves with the DiscoveryService.
 *
 */
@ProviderType
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
	 * Provides the InstanceDescription belonging to <b>this</b> instance.
	 * @return the InstanceDescription belonging to <b>this</b> instance
	 */
	InstanceDescription getLocalInstance();

    /**
     * Provides the set of InstanceDescriptions in the entire topology,
     * without any particular order
     * @return the set of InstanceDescriptions in the entire topology,
     * without any particular order
     */
	Set<InstanceDescription> getInstances();

	/**
	 * Searches through this topology and picks those accepted by the provided
	 * <code>InstanceFilter</code> - and returns them without any particular order
	 * @param filter the filter to use
	 * @return the set of InstanceDescriptions which were accepted by the InstanceFilter,
	 * without any particular order
	 */
	Set<InstanceDescription> findInstances(InstanceFilter filter);

    /**
     * Provides the collection of ClusterViews.
     * <p>
     * Note that all InstanceDescriptions belong to exactly one ClusterView -
     * including InstanceDescriptions that form "a cluster of 1"
     * @return the set of ClusterViews, without any particular order
     */
	Set<ClusterView> getClusterViews();
}
