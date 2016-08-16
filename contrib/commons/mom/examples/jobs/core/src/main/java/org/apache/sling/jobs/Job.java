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
package org.apache.sling.jobs;


import com.google.common.collect.ImmutableSet;
import org.apache.sling.mom.TopicManager;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Largely based on org.apache.sling.event.Job.
 * A Job contains the complete state of the Job as known in the current JVM.
 */
public interface Job {



    /**
     * The current job state.
     */
    enum JobState {
        QUEUED,     // waiting in queue after adding or for restart after failing
        ACTIVE,     // job is currently in processing
        SUCCEEDED,  // processing finished successfully
        STOPPED,    // processing was stopped by a user
        GIVEN_UP,   // number of retries reached
        ERROR,      // processing signaled CANCELLED or throw an exception
        ANY_STATE,  // Any active state.
        CREATED, DROPPED     // dropped jobs
    }

    /**
     * The job queue. - immutable.
     * @return The job queue name
     */
    @Nonnull
    Types.JobQueue getQueue();

    /**
     * Unique job ID. immutable.
     * @return The unique job ID.
     */
    @Nonnull
    String getId();


    /**
     * The job type. A job has a type, which is used by the Job Manager to offer up to JobConsumers implementing a JobTypeValve
     * to accept.
     * @return the job type.
     */
    @Nonnull
    Types.JobType getJobType();

    /**
     * @return a map of all properties.
     */
    @Nonnull
    Map<String,Object> getProperties();

    /**
     * On first execution the value of this property is zero.
     * This property is managed by the job handling.
     */
    int getRetryCount();

    /**
     * The property to track the retry maximum retry count for jobs.
     * This property is managed by the job handling.
     */
    int getNumberOfRetries();


    /**
     * The time when the job started.
     */
    long getStarted();

    /**
     * @return  The time when the job was created.
     */
    long getCreated();


    /**
     * Get the job state
     */
    @Nonnull
    JobState getJobState();

    /**
     * Set the new state.
     * @param newState
     */
    void setState(@Nonnull JobState newState);


    /**
     * If the job is cancelled or succeeded, this method will return the finish date.
     * @return The finish date or <code>null</code>
     */
    long getFinished();

    /**
     * This method returns the message from the last job processing, regardless
     * whether the processing failed, succeeded or was cancelled. The message
     * is optional and can be set by a job consumer.
     * @return The result message or <code>null</code>
     */
    @Nullable
    String getResultMessage();

    @Nonnull
    JobUpdateBuilder newJobUpdateBuilder();


    /**
     * @return the controller associated with this Job, if one is present in the current JVM otherwise null.
     */
    @Nullable
    JobController getController();


    /**
     * Set the current job controller.
     */
    void setJobController(@Nonnull JobController jobController);


    /**
     * Remove the current job controller.
     */
    void removeJobController();



}
