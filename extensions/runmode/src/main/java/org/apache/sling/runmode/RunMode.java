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
package org.apache.sling.runmode;

/** Define "run modes" for Sling, and allow components to find
 *  out if they're active according to the current set of run modes.
 *  
 *  A "run mode" is simply a string like "author", "dmz", "development",...
 *  The service does not validate their values.
 *        
 */
public interface RunMode {
    /** Suggested name for the System property used to set run modes.
     *  If that's used, the service should accept a comma-separated
     *  list of values, each defining one run mode. 
     */
    String RUN_MODES_SYSTEM_PROPERTY = "sling.run.modes";
    
    /** Wildcard for run modes, means "accept all modes" */
    String RUN_MODE_WILDCARD = "*";
    
    /** True if at least one of the given runModes is contained
     *  in the set of current active run modes.
     */
    boolean isActive(String [] runModes);
    
    /** Return the current set of active run modes */
    String [] getCurrentRunModes();
}
