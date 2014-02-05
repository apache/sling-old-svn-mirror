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

/**
 * A WhitelistEntry is capable of accepting certain requests
 * depending on a configuration.
 */
public interface WhitelistEntry {

    /**
     * @param request the incoming request which should be accepted or rejected
     * @return true if the request is accepted by this WhitelistEntry
     */
    public boolean accepts(ServletRequest request);
    
}
