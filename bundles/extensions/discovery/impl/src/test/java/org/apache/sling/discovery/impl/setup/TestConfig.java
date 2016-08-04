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
package org.apache.sling.discovery.impl.setup;

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;
import org.apache.sling.discovery.impl.Config;

public class TestConfig extends Config implements ModifiableTestBaseConfig {

    List<String> whitelist;
    private String drPath;
    
    public TestConfig(String path) {
        this.drPath = path;
        heartbeatTimeout = 20;
        heartbeatInterval = 20;
        minEventDelay = 1;
    }
    
    @Override
    public String getDiscoveryResourcePath() {
        return drPath;
    }
    
    @Override
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(long heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public int getMinEventDelay() {
        return minEventDelay;
    }

    public void setMinEventDelay(int minEventDelay) {
        this.minEventDelay = minEventDelay;
    }

    @Override
    public String[] getTopologyConnectorWhitelist() {
        if (whitelist==null) {
            return null;
        }
        return whitelist.toArray(new String[whitelist.size()]);
    }

    public void addTopologyConnectorWhitelistEntry(String whitelistEntry) {
        if (whitelist==null) {
            whitelist = new LinkedList<String>();
        }
        whitelist.add(whitelistEntry);
    }

    @Override
    public int getBackoffStableFactor() {
        return 1;
    }

    @Override
    public int getBackoffStandbyFactor() {
        return 1;
    }

    @Override
    public void setViewCheckTimeout(int viewCheckTimeout) {
        setHeartbeatTimeout(viewCheckTimeout);
    }

    @Override
    public void setViewCheckInterval(int viewCheckInterval) {
        setHeartbeatInterval(viewCheckInterval);
    }

    public void setPath(String path) {
        drPath = path;
    }
    
}