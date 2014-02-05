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

import org.apache.sling.discovery.impl.common.WildcardHelper;

/**
 * Implementation of a WhitelistEntry which can accept
 * wildcards (* and ?) in both IP and hostname
 */
public class WildcardWhitelistEntry implements WhitelistEntry {

    private final String hostOrAddressWithWildcard;
    
    public WildcardWhitelistEntry(String hostOrAddressWithWildcard) {
        this.hostOrAddressWithWildcard = hostOrAddressWithWildcard;
    }
    
    public boolean accepts(ServletRequest request) {
        if (WildcardHelper.matchesWildcard(request.getRemoteAddr(), hostOrAddressWithWildcard)) {
            return true;
        }
        if (WildcardHelper.matchesWildcard(request.getRemoteHost(), hostOrAddressWithWildcard)) {
            return true;
        }
        return false;
    }

}
