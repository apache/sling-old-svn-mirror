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
package org.apache.sling.discovery.impl.topology.connector;

import java.net.URL;
import java.util.Collection;

import org.apache.sling.discovery.impl.cluster.ClusterViewService;

/**
 * Registry for topology connector clients
 */
public interface ConnectorRegistry {

    /** Register an outgoing topology connector using the provided endpoint url **/
    TopologyConnectorClientInformation registerOutgoingConnector(
            ClusterViewService clusterViewService, URL topologyConnectorEndpoint);

    /** Lists all outgoing topology connectors **/
    Collection<TopologyConnectorClientInformation> listOutgoingConnectors();

    /** ping all outgoing topology connectors **/
    void pingOutgoingConnectors(boolean force);

    /** Unregister an outgoing topology connector identified by the given (connector) id **/
    boolean unregisterOutgoingConnector(String id);

}
