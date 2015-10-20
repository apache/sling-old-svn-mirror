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
package org.apache.sling.discovery.base.its.setup.mock;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;

public class SimpleConnectorConfig implements ModifiableTestBaseConfig {

    private int connectionTimeout;
    private int soTimeout;
    private URL[] topologyConnectorURLs;
    private List<String> topologyConnectorWhitelist;
    private String clusterInstancesPath = "/var/discovery/impl/clusterInstances";
    private boolean hmacEnabled;
    private String sharedKey;
    private long keyInterval;
    private boolean encryptionEnabled;
    private boolean gzipConnectorRequestsEnabled;
    private boolean autoStopLocalLoopEnabled;
    private int backoffStandbyFactor;
    private int backoffStableFactor;
    private long backoffStandbyInterval;
    private long announcementInterval = 20;
    private long announcementTimeout = 20;
    private int minEventDelay;

    @Override
    public int getSocketConnectTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public int getSoTimeout() {
        return soTimeout;
    }
    
    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    @Override
    public URL[] getTopologyConnectorURLs() {
        return topologyConnectorURLs;
    }
    
    public void setTopologyConnectorURLs(URL[] topologyConnectorURLs) {
        this.topologyConnectorURLs = topologyConnectorURLs;
    }

    @Override
    public String[] getTopologyConnectorWhitelist() {
        if (topologyConnectorWhitelist==null) {
            return null;
        }
        return topologyConnectorWhitelist.toArray(new String[topologyConnectorWhitelist.size()]);
    }

    public void addTopologyConnectorWhitelistEntry(String whitelistEntry) {
        if (topologyConnectorWhitelist==null) {
            topologyConnectorWhitelist = new LinkedList<String>();
        }
        topologyConnectorWhitelist.add(whitelistEntry);
    }

    @Override
    public String getClusterInstancesPath() {
        return clusterInstancesPath;
    }
    
    public void setClusterInstancesPath(String clusterInstancesPath) {
        this.clusterInstancesPath = clusterInstancesPath;
    }

    @Override
    public boolean isHmacEnabled() {
        return hmacEnabled;
    }
    
    public void setHmacEnabled(boolean hmacEnabled) {
        this.hmacEnabled = hmacEnabled;
    }

    @Override
    public String getSharedKey() {
        return sharedKey;
    }
    
    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Override
    public long getKeyInterval() {
        return keyInterval;
    }
    
    public void setKeyInterval(int keyInterval) {
        this.keyInterval = keyInterval;
    }

    @Override
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    @Override
    public boolean isGzipConnectorRequestsEnabled() {
        return gzipConnectorRequestsEnabled;
    }
    
    public void setGzipConnectorRequestsEnabled(boolean gzipConnectorRequestsEnabled) {
        this.gzipConnectorRequestsEnabled = gzipConnectorRequestsEnabled;
    }

    @Override
    public boolean isAutoStopLocalLoopEnabled() {
        return autoStopLocalLoopEnabled;
    }
    
    public void setAutoStopLocalLoopEnabled(boolean autoStopLocalLoopEnabled) {
        this.autoStopLocalLoopEnabled = autoStopLocalLoopEnabled;
    }

    @Override
    public int getBackoffStandbyFactor() {
        return backoffStandbyFactor;
    }
    
    public void setBackoffStandbyFactor(int backoffStandbyFactor) {
        this.backoffStandbyFactor = backoffStandbyFactor;
    }

    @Override
    public int getBackoffStableFactor() {
        return backoffStableFactor;
    }
    
    public void setBackoffStableFactor(int backoffStableFactor) {
        this.backoffStableFactor = backoffStableFactor;
    }

    @Override
    public long getBackoffStandbyInterval() {
        return backoffStandbyInterval;
    }
    
    public void setBackoffStandbyInterval(long backoffStandbyInterval) {
        this.backoffStandbyInterval = backoffStandbyInterval;
    }

    @Override
    public long getConnectorPingInterval() {
        return announcementInterval;
    }
    
    public void setAnnouncementInterval(long announcementInterval) {
        this.announcementInterval = announcementInterval;
    }

    @Override
    public long getConnectorPingTimeout() {
        return announcementTimeout;
    }
    
    public void setAnnouncementTimeout(long announcementTimeout) {
        this.announcementTimeout = announcementTimeout;
    }

    @Override
    public int getMinEventDelay() {
        return minEventDelay;
    }
    
    public void setMinEventDelay(int minEventDelay) {
        this.minEventDelay = minEventDelay;
    }

    @Override
    public void setViewCheckTimeout(int viewCheckTimeout) {
        announcementTimeout = viewCheckTimeout;
    }

    @Override
    public void setViewCheckInterval(int viewCheckInterval) {
        announcementInterval = viewCheckInterval;
    }
}
