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

import com.google.common.base.Preconditions;
import org.apache.sling.jobs.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * JobImpl is a data object to hold the current state of the job in the current JVM as loaded into memory.
 * The JobImpl also listens for JobUpdates.
 * Created by ieb on 23/03/2016.
 */
public class JobImpl implements Job, JobUpdateListener {
    private final Types.JobQueue jobQueue;
    private final String id;
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private int retryCount;
    private int numberOfRetries;
    private long startedAt;
    private long createdAt;
    private long lastUpdate = 0;
    private JobState jobState;
    private long finishedAt;
    private String resultMessage;
    private JobController jobController;
    private Types.JobType jobType;

    public JobImpl(@Nonnull Types.JobQueue jobQueue, @Nonnull String id, @Nonnull Types.JobType jobType, @Nonnull Map<String, Object> properties) {
        this.jobQueue = jobQueue;
        this.jobType = jobType;
        this.id = id;
        this.resultMessage = "";
        this.createdAt = System.currentTimeMillis();
        this.jobState = JobState.CREATED;
        this.properties.putAll(properties);
    }

    public JobImpl(JobUpdate update) {
        this.jobQueue = update.getQueue();
        this.id = update.getId();
        update(update);
        updateProperties(update.getProperties());
    }

    @Nonnull
    @Override
    public Types.JobQueue getQueue() {
        return jobQueue;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public Types.JobType getJobType() {
        return jobType;
    }

    @Nonnull
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    @Override
    public long getStarted() {
        return startedAt;
    }

    @Override
    public long getCreated() {
        return createdAt;
    }

    @Nonnull
    @Override
    public JobState getJobState() {
        return jobState;
    }

    @Override
    public void setState(@Nonnull JobState newState) {
        jobState = newState;
    }

    @Override
    public long getFinished() {
        return finishedAt;
    }

    @Nullable
    @Override
    public String getResultMessage() {
        return resultMessage;
    }


    @Nullable
    @Override
    public JobController getController() {
        return jobController;
    }

    @Override
    public void setJobController(@Nonnull JobController jobController) {
        this.jobController = jobController;
    }

    @Override
    public void removeJobController() {
        jobController = null;
    }

    /**
     * Apply an job update to this job, checking that the update is valid for the job.
     * @param jobUpdate
     */
    @Override
    public void update(@Nonnull JobUpdate jobUpdate) {
        if  ( id.equals(jobUpdate.getId()) && ( jobQueue == Types.ANY_JOB_QUEUE || jobQueue.equals(jobUpdate.getQueue()))) {
            // Start Job commands always go onto a queue and dont expire.
            if ( jobUpdate.getCommand() != JobUpdate.JobUpdateCommand.START_JOB && jobUpdate.expires() < System.currentTimeMillis()) {
                throw new IllegalStateException(
                        "JobUpdate has expired, can't be applied. Expired at "+jobUpdate.expires()+
                                ", time now "+System.currentTimeMillis()+
                                " expired "+(System.currentTimeMillis()-jobUpdate.expires())+" ms ago.");
            }
            if (jobUpdate.updateTimestamp() < lastUpdate ) {
                throw new IllegalStateException("JobUpdate received out of sequence, cant be applied. Last Update was at "+lastUpdate+" this update is at "+jobUpdate.updateTimestamp());
            }
            lastUpdate = jobUpdate.updateTimestamp();
            switch(jobUpdate.getCommand()) {
                case START_JOB:
                    updateState(jobUpdate);
                    updateProperties(jobUpdate.getProperties());
                    break;
                case UPDATE_JOB:
                    // note, when job first comes into existence it is updated, then started.
                    // the start message is a queued message, the update is a jobQueue or pub sub message.
                    updateState(jobUpdate);
                    updateProperties(jobUpdate.getProperties());
                    break;
                case RETRY_JOB:
                    updateState(jobUpdate);
                    // Allow more retries.
                    numberOfRetries = retryCount + numberOfRetries;
                    // TODO: trigger retry if required.
                    updateProperties(jobUpdate.getProperties());
                    break;
                case STOP_JOB:
                    if (jobController != null) {
                        jobController.stop();
                    }
                    break;
                case ABORT_JOB:
                    if (jobController != null) {
                        jobController.abort();
                    }
                    break;
            }
        } else {
            throw new IllegalArgumentException("Cant update job with jobUpdate that doesn't match id and jobQueue ");
        }
    }

    /**
     * Update the properties taking into account any PropertyActions required.
     * @param properties the update properties.
     */
    private void updateProperties(@Nonnull Map<String, Object> properties) {
        Preconditions.checkNotNull(properties, "Properties cant be null.");
        for (Map.Entry<String, Object> e : properties.entrySet()) {
            if (e.getValue() instanceof JobUpdate.JobPropertyAction ) {
                switch(((JobUpdate.JobPropertyAction)e.getValue())) {
                    case REMOVE:
                        this.properties.remove(e.getKey());
                        break;
                }
            } else {
                this.properties.put(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Update the jobstate data for the job.
     * @param jobUpdate
     */
    private void updateState(@Nonnull JobUpdate jobUpdate) {
        retryCount = jobUpdate.getRetryCount();
        jobType = jobUpdate.getJobType();
        numberOfRetries = jobUpdate.getNumberOfRetries();
        startedAt = jobUpdate.getStarted();
        createdAt = jobUpdate.getCreated();
        finishedAt = jobUpdate.getFinished();
        resultMessage = jobUpdate.getResultMessage();
        jobState = jobUpdate.getState();
    }

    /**
     * Get a JobUpdateBuilder for this Job.
     * @return the job update builder.
     */
    @Nonnull
    @Override
    public JobUpdateBuilder newJobUpdateBuilder() {
        return new JobUpdateBuilderImpl(this);
    }

}
