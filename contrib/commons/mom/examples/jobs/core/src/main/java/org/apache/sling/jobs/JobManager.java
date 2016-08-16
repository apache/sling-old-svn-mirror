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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * Based on org.apache.sling.event.job.JobManager but trimmed down and adjusted to deal with distributed job queues.
 */
public interface JobManager {





    /**
     * Fluent API to create, start and schedule new jobs
     * @param queue Required queue
     * @param  jobType required job type.
     * @return A job builder
     */
    @Nonnull
    JobBuilder newJobBuilder(@Nonnull Types.JobQueue queue, @Nonnull Types.JobType jobType);

    /**
     * Return a job based on the unique id.
     *
     * The returned job object is a snapshot of the job state taken at the time of the call. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return A job or <code>null</code>
     */

    @Nullable
    Job getJobById(@Nonnull String jobId);


    /**
     * Find a job - either queued or active.
     *
     * This method searches for a job with the given topic and filter properties. If more than one
     * job matches, the first one found is returned which could be any of the matching jobs.
     *
     * The returned job object is a snapshot of the job state taken at the time of the call. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * Even if the method return null, the Job may exist remotely and has not yet sent any messages.
     *
     * @param queue Topic is required.
     * @param template The map acts like a template. The searched job
     *                    must match the template (AND query).
     * @return A job or <code>null</code>
     */
    @Nullable
    Job getJob(@Nonnull Types.JobQueue queue, @Nonnull Map<String, Object> template);

    /**
     * The requested job types for the query.
     * This can either be all (unfinished) jobs, all activated (started) or all queued jobs.
     */
    enum QueryType {
        ALL,      // all means all active and all queued
        ACTIVE,
        QUEUED,
        HISTORY,    // returns the complete history of cancelled and succeeded jobs (if available)
        CANCELLED,  // history of cancelled jobs (STOPPED, GIVEN_UP, ERROR, DROPPED)
        SUCCEEDED,  // history of succeeded jobs
        STOPPED,    // history of stopped jobs
        GIVEN_UP,   // history of given up jobs
        ERROR,      // history of jobs signaled CANCELLED or throw an exception
        DROPPED     // history of dropped jobs
    }
    /**
     * Return all jobs of a given type.
     *
     * Based on the type parameter, either the history of jobs can be returned or unfinished jobs. The type
     * parameter can further specify which category of jobs should be returned: for the history either
     * succeeded jobs, cancelled jobs or both in combination can be returned. For unfinished jobs, either
     * queued jobs, started jobs or the combination can be returned.
     * If the history is returned, the result set is sorted in descending order, listening the newest entry
     * first. For unfinished jobs, the result set is sorted in ascending order.
     *
     * The returned job objects are a snapshot of the jobs state taken at the time of the call. Updates
     * to the job states are not reflected and the client needs to get new job objects.
     *
     * The Jobs returned are only jobs known to the JobManager instance.
     *
     * @param type Required parameter for the type. See above.
     * @param queue Queue Name can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param limit A positive number indicating the maximum number of jobs returned by the iterator. A value
     *              of zero or less indicates that all jobs should be returned.
     * @param templates A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A collection of jobs - the collection might be empty.
     */
    @Nonnull
    Collection<Job> findJobs(@Nonnull QueryType type, @Nonnull Types.JobQueue queue, long limit, @Nullable Map<String, Object>... templates);

    /**
     * Stop a job, by sending a message.
     * When a job is stopped and the job consumer supports stopping the job processing, it is up
     * to the job consumer how the stopping is handled. The job can be marked as finished successful,
     * permanently failed or being retried.
     */
    void stopJobById(@Nonnull String jobId);

    /**
     * Aborts a job by sending an abort message, may or may not be successful.
     *
     *
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return <code>true</code> if the job could be removed or does not exist anymore.
     *         <code>false</code> if the job is not know to the job manager, however the abort message should be sent anyway.
     */
    boolean abortJob(@Nonnull String jobId);


    /**
     * Retry a cancelled job, by requeuing.
     * If a job has failed permanently it can be requeued with this method. The job will be
     * removed from the history and put into the queue again. The new job will get a new job id.
     * For all other jobs calling this method has no effect and it simply returns <code>null</code>.
     * @param jobId The job id.
     * @return If the job is requeued, the new job object otherwise <code>null</code>
     */
    @Nullable
    Job retryJobById(@Nonnull String jobId);


}
