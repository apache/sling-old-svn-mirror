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

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>TopologyEventListener</code> service interface may be implemented by
 * components interested in being made aware of changes in the topology.
 * <p>
 * Upon registration and whenever changes in the topology occur, this
 * service is informed.
 */
@ConsumerType
public interface TopologyEventListener {

	/**
	 * Inform the service about an event in the topology - or in the discovery
	 * of the topology.
	 * <p>
	 * The <code>TopologyEvent</code> contains details about what changed.
	 * The supported event types are:
	 * <ul>
	 *  <li><code>TOPOLOGY_INIT</code> sent when the <code>TopologyEventListener</code>
	 *  was first bound to the discovery service - represents the initial state
	 *  of the topology at that time.</li>
	 *  <li><code>TOPOLOGY_CHANGING</code> sent when the discovery service
	 *  discovered a change in the topology and has started to settle the change.
	 *  This event is sent before <code>TOPOLOGY_CHANGED</code> but is optional</li>
	 *  <li><code>TOPOLOGY_CHANGED</code> sent when the discovery service
	 *  discovered a change in the topology and has settled it.</li>
	 *  <li><code>PROPERTIES_CHANGED</code> sent when the one or many properties
	 *  have changed in an instance in the current topology</li>
	 * </ul>
	 * A note on instance restarts: it is currently not a requirement on the
	 * discovery service to send a TopologyEvent should an instance restart
	 * occur rapidly (ie within the change detection timeout). A TopologyEvent
	 * is only sent if the number of instances or any property changes.
	 * Should there be a requirement to detect a restart in a guaranteed fashion,
	 * it is always possible to set a particular property (using the PropertyProvider)
	 * to the instance start time and have others detect a change in that property.
	 * @param event The topology event
	 */
	void handleTopologyEvent(TopologyEvent event);

}
