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
package org.apache.sling.bgservlets.impl.storage;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.JobProgressInfo;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.JobStatus.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JobStatus that gets its data from a Node created
 *  by the JobDataImpl class. The state of such a job
 *  can be changed only if the job is currently active
 *  in the ExecutionEngine. */
@Component
@Service
public class NodeJobStatusFactoryImpl implements NodeJobStatusFactory { 

    private Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private ExecutionEngine executionEngine;
    
    private class NodeJobStatus implements JobStatus {
        private final String path;
        private final String streamPath;
        private final Date creationTime;
        
        public NodeJobStatus(Node n) throws RepositoryException {
            path = n.getPath();
            if(n.hasProperty(JobData.PROP_EXTENSION)) {
                streamPath = path + JobStatus.STREAM_PATH_SUFFIX + "." 
                    + n.getProperty(JobData.PROP_EXTENSION).getString();
            } else {
                streamPath = path + JobStatus.STREAM_PATH_SUFFIX;
            }
            creationTime = new JobDataImpl(n).getCreationTime();
        }
    
        public String getPath() {
            return path;
        }
    
        public String getStreamPath() {
            return streamPath;
        }
        
        public Date getCreationTime() {
            return creationTime;
        }

        public State getState() {
            final JobStatus j = getActiveJob();
            if(j == null) {
                log.debug("Job {} not found by getActiveJob, assuming status==DONE", path);
                return State.DONE;
            }
            return j.getState();
        }
    
        public void requestStateChange(State s) {
            final JobStatus j = getActiveJob();
            if(j == null) {
                log.debug("Job {} is not active, cannot change state", path);
            } else {
                j.requestStateChange(s);
            }
        }
        
        /** @inheritDoc */
        public State [] getAllowedHumanStateChanges() {
            final JobStatus j = getActiveJob();
            if(j == null) {
                return new State[] {};
            }
            return j.getAllowedHumanStateChanges();
        }
        
        private JobStatus getActiveJob() {
            if(executionEngine != null) {
                return executionEngine.getJobStatus(path);
            }
            return null;
        }

        public JobProgressInfo getProgressInfo() {
            // If job is active, return its info, else
            // return info from our job node
            final JobStatus active = getActiveJob();
            if(active != null) {
                return active.getProgressInfo();
            } else {
                return new JobProgressInfo() {
                    public String getProgressMessage() {
                        return getState().toString();
                    }
                    
                    public Date getEstimatedCompletionTime() {
                        return null;
                    }
                };
            }
        }
    };
    
    public JobStatus getJobStatus(Node n) throws RepositoryException {
        return new NodeJobStatus(n);
    }
}
