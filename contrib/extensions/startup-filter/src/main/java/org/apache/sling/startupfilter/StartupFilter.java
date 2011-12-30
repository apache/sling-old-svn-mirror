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
package org.apache.sling.startupfilter;

/** Servlet Filter that blocks access to the Sling main 
 *  servlet during startup, by returning an HTTP 503
 *  or other suitable status code.
 *  
 *  A typical use case is to start this filter before
 *  the Sling main servlet (by setting a lower start level
 *  on its bundle than on the Sling engine bundle), and
 *  deactivating once startup is finished.
 */
public interface StartupFilter {
    
    String DEFAULT_STATUS_MESSAGE = "Startup in progress";
    
    /** Clients can supply objects implementing this
     *  interface, to have the filter respond to HTTP
     *  requests with the supplied information message.
     */
    public interface ProgressInfoProvider {
        String getInfo();
    }
    
    /** This ProgressInfoProvider is active by default, it 
     *  must be removed for the filter to let requests pass through.
     */
    public static ProgressInfoProvider DEFAULT_INFO_PROVIDER = new ProgressInfoProvider() {
        @Override
        public String toString() {
            return "Default ProgressInfoProvider";
        }
        public String getInfo() { 
            return DEFAULT_STATUS_MESSAGE;
        }
    };
    
    /** Activate the supplied ProgressInfoProvider */
    public void addProgressInfoProvider(ProgressInfoProvider pip);
    
    /** Deactivate the supplied ProgressInfoProvider if it was
     *  currently active.
     *  Once all such providers are removed, the filter disables
     *  itself and lets requests pass through.
     */
    public void removeProgressInfoProvider(ProgressInfoProvider pip);
}