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
package org.apache.sling.discovery.oak.its.setup;

import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;
import org.apache.sling.discovery.oak.Config;

public class OakTestConfig extends Config implements ModifiableTestBaseConfig {

    public OakTestConfig() {
        // empty
    }
    
    public void setDiscoveryResourcePath(String discoveryResourcePath) {
        this.discoveryResourcePath = discoveryResourcePath;
    }

    public void setMinEventDelay(int minEventDelay) {
        this.minEventDelay = minEventDelay;
    }

    public void addTopologyConnectorWhitelistEntry(String whitelistEntry) {
        if (topologyConnectorWhitelist==null) {
            topologyConnectorWhitelist = new String[] {whitelistEntry};
        } else {
            String[] list = topologyConnectorWhitelist;
            topologyConnectorWhitelist = new String[list.length+1];
            System.arraycopy(list, 0, topologyConnectorWhitelist, 0, list.length);
            topologyConnectorWhitelist[topologyConnectorWhitelist.length-1] = whitelistEntry;
        }
    }

    @Override
    public int getBackoffStableFactor() {
        return 1;
    }

    @Override
    public int getBackoffStandbyFactor() {
        return 1;
    }

    public void setConnectorInterval(long connectorInterval) {
        this.connectorPingInterval = connectorInterval;
    }

    public void setConnectorTimeout(long connectorTimeout) {
        this.connectorPingTimeout = connectorTimeout;
    }
    
    @Override
    public void setViewCheckTimeout(int viewCheckTimeout) {
        setConnectorTimeout(viewCheckTimeout);
    }

    @Override
    public void setViewCheckInterval(int viewCheckInterval) {
        setConnectorInterval(viewCheckInterval);
    }
    
    public long getViewCheckerTimeout() {
        return connectorPingTimeout;
    }
}