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
package org.apache.sling.jobs.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobBuilder;
import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.JobUpdateListener;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStarter;
import org.apache.sling.jobs.impl.spi.JobStorage;

/**
 * Implements a JobManager, storing Jobs in a JobStorage implementation.
 * Implements a JobStarter so it can start Jobs and queue them by sending a message.
 * Implements a JobUpdateListener so it can store inbound update messages.
 * Requires a JobStorage implementation to store jobs and a JobListener to process update messages.
 * Requires a JobUpdateLister which it uses to send JobUpdates to.
 * Does not run jobs, that is performed by implementations of JobConsumer listening to queues.
 */
public  class JobManagerImpl implements JobManager, JobStarter, JobUpdateListener {


    /**
     * Storage for jobs.
     */
    private final JobStorage jobStorage;
    /**
     * A listener for job updates.
     */
    private final JobUpdateListener messageSender;

    /**
     *
     * @param jobStorage job storage used by the manager.
     * @param messageSender the message sender that listens for updates.
     */
    public JobManagerImpl(JobStorage jobStorage, JobUpdateListener messageSender) {
        this.jobStorage = jobStorage;
        this.messageSender = messageSender;
    }

    @Nonnull
    @Override
    public JobBuilder newJobBuilder(@Nonnull Types.JobQueue queue, @Nonnull Types.JobType jobType) {
        return new JobBuilderImpl(this, queue, jobType);
    }

    @Nullable
    @Override
    public Job getJobById(@Nonnull String jobId) {
        return jobStorage.get(jobId);
    }

    @Override
    public void stopJobById(@Nonnull String jobId) {
        Job job = getJobById(jobId);
        if ( job != null) {
            messageSender.update(job.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.STOP_JOB).build());
        }
    }

    @Override
    public boolean abortJob(@Nonnull String jobId) {
        Job job = getJobById(jobId);
        if ( job != null) {
            messageSender.update(job.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.ABORT_JOB).build());
            return true;
        } else {
            messageSender.update(new JobUpdateBuilderImpl(jobId).command(JobUpdate.JobUpdateCommand.ABORT_JOB).build());
            return false;
        }
    }

    @Nullable
    @Override
    public Job retryJobById(@Nonnull String jobId) {
        Job job = getJobById(jobId);
        if (job != null) {
            messageSender.update(job.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.RETRY_JOB).build());
        }
        return job;
    }


    @Override
    public Job start(Job j) {
        j.setState(Job.JobState.QUEUED);
        messageSender.update(j.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.START_JOB).putAll(j.getProperties()).build());
        return jobStorage.put(j);
    }


    @Override
    public void update(@Nonnull JobUpdate update) {
        Job j = jobStorage.get(update.getId());
        if ( j instanceof JobUpdateListener ) {
            ((JobUpdateListener) j).update(update);
        } else {
            jobStorage.put(new JobImpl(update));
        }
    }

    public void dispose() {
    }
}
