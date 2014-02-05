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
package org.apache.sling.discovery.impl.topology.connector.wl;

import javax.servlet.ServletRequest;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

/**
 * Implementation of a WhitelistEntry which accepts
 * cidr and ip mask notations.
 */
public class SubnetWhitelistEntry implements WhitelistEntry {

    private final SubnetInfo subnetInfo;
    
    public SubnetWhitelistEntry(String cidrNotation) {
        subnetInfo = new SubnetUtils(cidrNotation).getInfo();
    }
    
    public SubnetWhitelistEntry(String ip, String subnetMask) {
        subnetInfo = new SubnetUtils(ip, subnetMask).getInfo();
    }
    
    public boolean accepts(ServletRequest request) {
        final String remoteAddr = request.getRemoteAddr();
        return subnetInfo.isInRange(remoteAddr);
    }

}
