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
package org.apache.sling.discovery.commons.providers.spi.base;

/**
 * Provides the configuration of a few paths needed by discovery-lite processing services
 */
public interface DiscoveryLiteConfig {

    /**
     * Returns the configured path to store the syncTokens to
     * @return the configured path to store the syncTokens to
     */
    String getSyncTokenPath();

    /**
     * Returns the configured path to store the idMap to
     * @return the configured path to store the idMap to
     */
    String getIdMapPath();

    /**
     * Returns the timeout (in milliseconds) to be used when waiting for the sync tokens or id mapping
     * @return the timeout (in milliseconds) to be used when waiting for the sync tokens or id mapping
     */
    long getClusterSyncServiceTimeoutMillis();
    
    /**
     * Returns the interval (in milliseconds) to be used when waiting for the sync tokens or id mapping
     * @return the interval (in milliseconds) to be used when waiting for the sync tokens or id mapping
     */
    long getClusterSyncServiceIntervalMillis();

}
