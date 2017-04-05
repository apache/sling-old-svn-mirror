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
package org.apache.sling.discovery.base.connectors;

import java.net.URL;

/**
 * Configuration for discovery.base
 */
public interface BaseConfig {

    /**
     * Returns the socket connect() timeout used by the topology connector, 0 disables the timeout
     * @return the socket connect() timeout used by the topology connector, 0 disables the timeout
     */
    public int getSocketConnectTimeout();

    /**
     * Returns the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     * @return the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     */
    public int getSoTimeout();
    
    /**
     * Returns the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured (default is null)
     * @return the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured
     */
    public URL[] getTopologyConnectorURLs();

    /**
     * Returns a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     * @return a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     */
    public String[] getTopologyConnectorWhitelist();

    /**
     * Returns the resource path where cluster instance informations are stored.
     * @return the resource path where cluster instance informations are stored
     */
    public String getClusterInstancesPath();


    /**
     * @return true if hmac is enabled.
     */
    public boolean isHmacEnabled();

    /**
     * @return the shared key
     */
    public String getSharedKey();

    /**
     * @return the interval of the shared key for hmac.
     */
    public long getKeyInterval();

    /**
     * @return true if encryption is enabled.
     */
    public boolean isEncryptionEnabled();
    
    /**
     * @return true if requests on the topology connector should be gzipped
     * (which only works if the server accepts that.. ie discovery.impl 1.0.4+)
     */
    public boolean isGzipConnectorRequestsEnabled();
    
    /**
     * @return true if the auto-stopping of local-loop topology connectors is enabled.
     */
    public boolean isAutoStopLocalLoopEnabled();

    /**
     * Returns the backoff factor to be used for standby (loop) connectors
     * @return the backoff factor to be used for standby (loop) connectors
     */
    public int getBackoffStandbyFactor();

    /**
     * Returns the (maximum) backoff factor to be used for stable connectors
     * @return the (maximum) backoff factor to be used for stable connectors
     */
    public int getBackoffStableFactor();

    /**
     * Returns the backoff interval for standby (loop) connectors in seconds
     * @return the backoff interval for standby (loop) connectors in seconds
     */
    public long getBackoffStandbyInterval();

    /**
     * Returns the interval (in seconds) in which connectors are pinged
     * @return the interval (in seconds) in which connectors are pinged
     */
    public long getConnectorPingInterval();

    /**
     * Returns the timeout (in seconds) after which a connector ping is considered invalid/timed out
     * @return the timeout (in seconds) after which a connector ping is considered invalid/timed out
     */
    public long getConnectorPingTimeout();

    /**
     * The minEventDelay to apply to the ViewStateManager
     */
    public int getMinEventDelay();

}
