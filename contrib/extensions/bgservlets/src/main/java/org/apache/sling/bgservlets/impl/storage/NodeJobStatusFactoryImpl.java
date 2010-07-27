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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobStatus;
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
        
        public NodeJobStatus(Node n) throws RepositoryException {
            path = n.getPath();
        }
    
        public String getPath() {
            return path;
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
                throw new JobStorageException("Job is not active, cannot change state, path=" + path);
            }
            j.requestStateChange(s);
        }
        
        private JobStatus getActiveJob() {
            if(executionEngine != null) {
                return executionEngine.getJobStatus(path);
            }
            return null;
        }
    };
    
    public JobStatus getJobStatus(Node n) throws RepositoryException {
        return new NodeJobStatus(n);
    }
}
