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

/**
 * A topology event is sent whenever a change in the topology occurs.
 *
 * This event object might be extended in the future with new event types
 * and methods.
 * @see DiscoveryAware
 */
public class TopologyEvent {

	public static enum Type {
		TOPOLOGY_INIT,      // Inform the service about the initial topology state
		TOPOLOGY_CHANGING,  // Inform the service about the fact that a state change was detected
		                    // in the cluster topology and that a voting amongst the members about
		                    // a new, valid view has just started.
		                    // Note that implementations might not support this event at all.
		TOPOLOGY_CHANGED,   // Inform the service about a state change in the cluster topology.
		PROPERTIES_CHANGED  // one or many properties have been changed on an instance which is part
		                    // of the topology
    }

	private final Type type;
	private final TopologyView oldView;
	private final TopologyView newView;

	public TopologyEvent(final Type type, final TopologyView oldView, final TopologyView newView) {
		if (type==null) {
			throw new IllegalArgumentException("type must not be null");
		}

		if (type==Type.TOPOLOGY_INIT) {
			// then oldView is null
			if (oldView!=null) {
				throw new IllegalArgumentException("oldView must be null");
			}
			// and newView must be not null
			if (newView==null) {
				throw new IllegalArgumentException("newView must not be null");
			}
		} else if (type==Type.TOPOLOGY_CHANGING) {
			// then newView is null
			if (newView!=null) {
				throw new IllegalArgumentException("newView must be null");
			}
			// and oldView must not be null
			if (oldView==null) {
				throw new IllegalArgumentException("oldView must not be null");
			}
		} else {
			// in all other cases both oldView and newView must not be null
			if (oldView==null) {
				throw new IllegalArgumentException("oldView must not be null");
			}
			if (newView==null) {
				throw new IllegalArgumentException("newView must not be null");
			}
		}
		this.type = type;
		this.oldView = oldView;
		this.newView = newView;
	}

	/**
	 * Returns the type of this event
	 * @return the type of this event
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the view which was valid up until now.
	 * <p>
	 * This is null in case of <code>TOPOLOGY_INIT</code>
	 * @return the view which was valid up until now, or null in case of a fresh instance start
	 */
	public TopologyView getOldView() {
		return oldView;
	}

	/**
	 * Returns the view which is currently (i.e. newly) valid.
	 * <p>
	 * This is null in case of <code>TOPOLOGY_CHANGING</code>
	 * @return the view which is currently valid, or null in case of <code>TOPOLOGY_CHANGING</code>
	 */
	public TopologyView getNewView() {
		return newView;
	}

    @Override
    public String toString() {
        return "TopologyEvent [type=" + type + ", oldView=" + oldView
                + ", newView=" + newView + "]";
    }
}
