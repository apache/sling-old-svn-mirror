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
package org.apache.sling.discovery.base.its.setup;

import org.apache.sling.discovery.base.connectors.ping.TopologyConnectorClientInformation;

public class VirtualConnector {
    @SuppressWarnings("unused")
    private final VirtualInstance from;
    @SuppressWarnings("unused")
    private final VirtualInstance to;
    private final int jettyPort;
    @SuppressWarnings("unused")
    private final TopologyConnectorClientInformation connectorInfo;

    public VirtualConnector(VirtualInstance from, VirtualInstance to) throws Throwable {
        this.from = from;
        this.to = to;
        to.startJetty();
        this.jettyPort = to.getJettyPort();
        this.connectorInfo = from.connectTo("http://localhost:"+jettyPort+"/system/console/topology/connector");
    }
}