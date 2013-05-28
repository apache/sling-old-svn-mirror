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
package org.apache.sling.event.impl.jobs;


/**
 * This object adds actions to a {@link JobImpl}.
 */
public class JobHandler {

    private final JobImpl job;

    public long queued = -1;
    public long started = -1;

    private final JobManagerImpl jobManager;

    public JobHandler(final JobImpl job, final JobManagerImpl jobManager) {
        this.job = job;
        this.jobManager = jobManager;
    }

    public JobImpl getJob() {
        return this.job;
    }

    public boolean start() {
        return this.jobManager.start(this);
    }

    public void finished() {
        this.jobManager.finished(this);
    }

    public boolean reschedule() {
        return this.jobManager.reschedule(this);
    }

    public boolean remove() {
        return this.jobManager.remove(this);
    }

    public void reassign() {
        this.jobManager.reassign(this);
    }

    @Override
    public int hashCode() {
        return this.job.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof JobHandler) ) {
            return false;
        }
        return this.job.getId().equals(((JobHandler)obj).job.getId());
    }

    @Override
    public String toString() {
        return "JobHandler(" + this.job.getId() + ")";
    }
}