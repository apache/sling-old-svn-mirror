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

import java.util.Iterator;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/** Back-end for management consoles that 
 *  give access to background jobs.
 */
public interface JobConsole {
    /** Return Iterator on JobStatus, in descending order of 
     *  creation date.
     * 
     *  @param session not used if activeOnly = true
     *  @param activeOnly if true, only jobs that are currently
     *  active in the ExecutionEngine are returned. 
     */
    Iterator<JobStatus> getJobStatus(Session session, boolean activeOnly);
    
    /** Return a single JobStatus, null if not found.
     * 
     *  @param session Session to use if reading from persistent storage
     *  @param path the job path 
     */
    JobStatus getJobStatus(Session session, String path);
    
    /** Return the full path, including extension, to the job's status page. */
    String getJobStatusPagePath(HttpServletRequest request, JobStatus jobStatus, String extension);
    
    /** Return the full path, including extension, to the job's stream */
    String getJobStreamPath(HttpServletRequest request, JobStatus jobStatus);
}
