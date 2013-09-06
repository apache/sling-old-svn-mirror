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

import aQute.bnd.annotation.ProviderType;

/**
 * The discovery service can be used to get the current topology view.
 * <p>
 * The discovery service is in charge of managing live instances that
 * have announced themselves as being part of a topology view. The exact
 * details of how this announcement occurs is implementation dependent.
 * <p>
 * Note that the discovery service fails if it detects a situation
 * where more than one instance with the same sling.id exists in a cluster.
 */
@ProviderType
public interface DiscoveryService {

	/**
	 * Returns the topology that was last discovered by this service.
	 * <p>
	 * If for some reason the service is currently not able to do topology discovery
	 * it will return the last valid topology marked with <code>false</code> in the call
	 * to <codeTopologyView.isCurrent()</code>. This is also true if the service
	 * has noticed a potential change in the topology and is in the process of
	 * settling the change in the topology (eg with peers, ie voting).
	 * <p>
	 * Note that this call is synchronized with <code>TopologyEventListener.handleTopologyEvent()</code>
	 * calls: ie if calls to <code>TopologyEventListener.handleTopologyEvent()</code> are currently
	 * ongoing, then the call to this method will block until all <code>TopologyEventListener</code>s
	 * have been called. Be careful not to cause deadlock situations.
	 * <p>
	 * @return the topology that was last discovered by this service. This will never
	 * be null (ie even if a change in the topology is ongoing at the moment or the
	 * cluster consists only of the local instance).
	 */
    TopologyView getTopology();

}
