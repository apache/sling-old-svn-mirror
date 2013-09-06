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

import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * An InstanceDescription represents and contains information about an
 * instance that is part of a TopologyView.
 * <p>
 * Note that all methods are idempotent - they always return the same values
 * on subsequent calls. Rather, on any change new InstanceDescriptions are created.
 *
 *
 * @see TopologyView
 */
@ProviderType
public interface InstanceDescription {

    /**
     * Property containing a name for the instance.
     * The instance should provide this property.
     */
    String PROPERTY_NAME = "org.apache.sling.instance.name";

    /**
     * Property containing a description for the instance.
     * The instance should provide this property.
     */
    String PROPERTY_DESCRIPTION = "org.apache.sling.instance.description";

    /**
     * Property containing endpoints to connect to the instance. The
     * value is a comma separated list.
     * The instance should provide this property.
     */
    String PROPERTY_ENDPOINTS = "org.apache.sling.instance.endpoints";

    /**
	 * Returns the ClusterView of which this instance is part of.
	 * <p>
	 * Every instance is part of a ClusterView even if it is standalone.
	 * @return the ClusterView
	 */
    ClusterView getClusterView();

	/**
	 * If an instance is part of a cluster, it can potentially be a leader of that cluster -
	 * this information is queried here.
	 * <p>
	 * If an instance is not part of a cluster, this method returns true.
	 * <p>
	 * Only one instance of a cluster is guaranteed to be the leader at any time.
	 * This guarantee is provided by this service.
	 * If the leader goes down, the service elects a new leader and announces it to
	 * TopologyEventListener listeners.
	 * @return true if this instance is the - only -  leader in this cluster,
	 * false if it is one of the slaves, or true if it is not at all part of a cluster
	 */
	boolean isLeader();

	/**
	 * Determines whether this InstanceDescription is representing the local instance.
	 * @return whether this InstanceDescription is representing the local instance.
	 */
	boolean isLocal();

    /**
     * The identifier of the running Sling instance.
     */
    String getSlingId();

    /**
     * Returns the value of a particular property.
     * <p>
     * Note that there are no hard guarantees or requirements as to how quickly
     * a property is available once it is set on a distant instance.
     * @param name The property name
     * @return The value of the property or <code>null</code>
     * @see DiscoveryService#setProperty(String, String)
     */
    String getProperty(final String name);

    /**
     * Returns a Map containing all properties of this instance.
     * This method always returns a map, it might be empty. The returned map
     * is not modifiable.
     * @return a Map containing all properties of this instance
     */
    Map<String,String> getProperties();
}
