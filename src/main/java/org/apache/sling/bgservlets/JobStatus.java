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
package org.apache.sling.bgservlets;

import java.util.Date;

/** Provides info about a job */
public interface JobStatus {
    enum State {
        NEW, QUEUED, REJECTED, RUNNING, SUSPEND_REQUESTED, SUSPENDED, STOP_REQUESTED, STOPPED, DONE
    }
    
    /** Suffix used to build the job's stream path */
    String STREAM_PATH_SUFFIX = "/stream";

    /** Return the job's current state */
    State getState();
    
    /** Return the job's creation time */
    Date getCreationTime();

    /**
     * Request a change in the job's state, which might not take effect
     * immediately, or even be ignored.
     */
    void requestStateChange(State s);
    
    /** Indicate which state changes a human user can currently request,
     *  based on our state. For a human user, the only states that make sense
     *  to be requested are RUNNING, SUSPENDED and STOPPED, or a subset
     *  of those based on current state.
     *  @return empty array if no state change allowed 
     */
    State [] getAllowedHumanStateChanges();

    /** Path of the Resource that describes this job */
    String getPath();
    
    /** Full Path of the job's stream, including extension */
    String getStreamPath();
    
    /** Return the job's progress info */
    JobProgressInfo getProgressInfo();
}