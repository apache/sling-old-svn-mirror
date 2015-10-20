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
package org.apache.sling.discovery.base.commons;

/**
 * This exception is thrown when the ClusterViewService
 * does not have a cluster view that is valid. 
 * That can either be because it cannot access the repository
 * (login or other repository exception) or that there is
 * no established view yet at all (not yet voted case) - 
 * or that there is an established view but it doesn't include 
 * the local instance (isolated case)
 */
@SuppressWarnings("serial")
public class UndefinedClusterViewException extends Exception {

    public static enum Reason {
        /** used when the local instance is isolated from the topology
         * (which is noticed by an established view that does not include
         * the local instance)
         */
        ISOLATED_FROM_TOPOLOGY,
        
        /** used when there is no established view yet
         * (happens on a fresh installation)
         */
        NO_ESTABLISHED_VIEW,
        
        /** used when we couldn't reach the repository **/
        REPOSITORY_EXCEPTION
    }

    private final Reason reason;
    
    public UndefinedClusterViewException(Reason reason) {
        super();
        this.reason = reason;
    }

    public UndefinedClusterViewException(Reason reason, String msg) {
        super(msg);
        this.reason = reason;
    }
    
    public Reason getReason() {
        return reason;
    }
}
