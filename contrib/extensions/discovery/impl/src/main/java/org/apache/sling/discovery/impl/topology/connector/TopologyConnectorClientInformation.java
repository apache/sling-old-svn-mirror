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

/**
 * provides information about a topology connector client
 */
public interface TopologyConnectorClientInformation {

    public static enum OriginInfo {
        Config, // this connector was created via config
        WebConsole, // this connector was created via the wbconsole
        Programmatically // this connector was created programmatically
    }

    /** the endpoint url where this connector is connecting to **/
    public URL getConnectorUrl();

    /** whether or not this connector was able to successfully connect **/
    public boolean isConnected();

    /** the sling id of the remote end **/
    public String getRemoteSlingId();

    // public List<String> listFallbackConnectorUrls();

    /** the unique id of this connector **/
    public String getId();

    /** the information about how this connector was created **/
    public OriginInfo getOriginInfo();

}
