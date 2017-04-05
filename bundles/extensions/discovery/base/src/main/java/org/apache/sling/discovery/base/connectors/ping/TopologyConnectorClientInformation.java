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
package org.apache.sling.discovery.base.connectors.ping;

import java.net.URL;

/**
 * provides information about a topology connector client
 */
public interface TopologyConnectorClientInformation {

    /** the endpoint url where this connector is connecting to **/
    URL getConnectorUrl();

    /** return the http status code of the last post to the servlet, -1 if no post was ever done **/
    int getStatusCode();

    /** SLING-3316 : whether or not this connector was auto-stopped **/
    boolean isAutoStopped();
    
    /** whether or not this connector was able to successfully connect **/
    boolean isConnected();
    
    /** provides more details about connection failures **/
    String getStatusDetails();

    /** whether or not the counterpart of this connector has detected a loop in the topology connectors **/
    boolean representsLoop();
    
    /** the sling id of the remote end **/
    String getRemoteSlingId();

    /** the unique id of this connector **/
    String getId();

    /** the Content-Encoding of the last request **/
    String getLastRequestEncoding();

    /** the Content-Encoding of the last response **/
    String getLastResponseEncoding();

    /** the unix-millis when the last heartbeat was sent **/
    long getLastPingSent();

    /** the seconds until the next heartbeat is due **/
    int getNextPingDue();
}
