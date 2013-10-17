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
package org.apache.sling.event.jobs;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.event.Event;

import aQute.bnd.annotation.ProviderType;


/**
 * The job manager is the heart of the job event handling.
 * It can be used to manage and monitor the queues.
 * @since 3.0
 */
@ProviderType
public interface JobManager {

    /**
     * Return statistics information about all queues.
     */
    Statistics getStatistics();

    /**
     * Return statistics information about job topics.
     */
    Iterable<TopicStatistics> getTopicStatistics();

    /**
     * Return a queue with a specific name (if running)
     * @param name The queue name
     * @return The queue or <code>null</code>
     */
    Queue getQueue(String name);

    /**
     * Return an iterator for all available queues.
     */
    Iterable<Queue> getQueues();

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
     * Restart the job manager.
     * This method restarts the job manager and all queues - currently processed jobs will be finished.
     * The job manager should only be restarted if really necessary!
     */
    void restart();

    /**
     * Add a new job
     *
     * If the topic is <code>null</code> or illegal, no job is created and <code>null</code> is returned.
     * If properties are provided, all of them must be serializable. If there are non serializable
     * objects in the properties, no job is created and <code>null</code> is returned.
     * A job topic is a hierarchical name separated by dashes, each part has to start with a letter,
     * allowed characters are letters, numbers and the underscore.
     *
     * The returned job object is a snapshot of the job state taken at the time of creation. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * If the queue for processing this job is configured to drop the job, <code>null</code> is returned
     * as well.
     *
     * @param topic The required job topic.
     * @param properties Optional job properties. The properties must be serializable.
     * @return The new job - or <code>null</code> if the job could not be created.
     * @since 1.2
     */
    Job addJob(String topic, Map<String, Object> properties);

    /**
     * Return a job based on the unique id.
     *
     * The returned job object is a snapshot of the job state taken at the time of the call. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return A job or <code>null</code>
     * @since 1.2
     */
    Job getJobById(String jobId);

    /**
     * Removes the job even if it is currently in processing.
     *
     * If the job exists and is not in processing, it gets removed from the processing queue.
     * If the job exists and is in processing, it is removed from the persistence layer,
     * however processing is not stopped.
     *
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return <code>true</code> if the job could be removed or does not exist anymore.
     *         <code>false</code> otherwise.
     * @since 1.2
     */
    boolean removeJobById(String jobId);

    /**
     * Find a job - either scheduled or active.
     *
     * This method searches for an event with the given topic and filter properties. If more than one
     * job matches, the first one found is returned which could be any of the matching jobs.
     *
     * The returned job object is a snapshot of the job state taken at the time of the call. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * @param topic Topic is required.
     * @param template The map acts like a template. The searched job
     *                    must match the template (AND query).
     * @return A job or <code>null</code>
     * @since 1.2
     */
    Job getJob(String topic, Map<String, Object> template);

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
     * @param type Required parameter for the type. See above.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param limit A positive number indicating the maximum number of jobs returned by the iterator. A value
     *              of zero or less indicates that all jobs should be returned.
     * @param templates A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A collection of jobs - the collection might be empty.
     * @since 1.2
     */
    Collection<Job> findJobs(QueryType type, String topic, long limit, Map<String, Object>... templates);

    /**
     * Stop a job.
     * When a job is stopped and the job consumer supports stopping the job processing, it is up
     * to the job consumer how the stopping is handled. The job can be marked as finished successful,
     * permanently failed or being retried.
     * @since 1.3
     */
    void stopJobById(String jobId);

    /**
     * Retry a cancelled job.
     * If a job has failed permanently it can be requeued with this method. The job will be
     * removed from the history and put into the queue again. The new job will get a new job id.
     * For all other jobs calling this method has no effect and it simply returns <code>null</code>.
     * @param jobId The job id.
     * @return If the job is requeued, the new job object otherwise <code>null</code>
     */
    Job retryJobById(String jobId);

    /**
     * Fluent API to create, start and schedule new jobs
     * @param topic Required topic
     * @return A job builder
     * @since 1.3
     */
    JobBuilder createJob(final String topic);

    /**
     * Return all available job schedules.
     * @since 1.3
     */
    Collection<ScheduledJobInfo> getScheduledJobs();

    /**
     * Add a new job
     *
     * If the topic is <code>null</code> or illegal, no job is created and <code>null</code> is returned.
     * If properties are provided, all of them must be serializable. If there are non serializable
     * objects in the properties, no job is created and <code>null</code> is returned.
     * A job topic is a hierarchical name separated by dashes, each part has to start with a letter,
     * allowed characters are letters, numbers and the underscore.
     *
     * This method allows to specify a job name which should uniquely identify this job. If a job with
     * the same name is started on different instances, the job is still processed only once. However,
     * the topology api in combination with the leader selection provides a better way for
     * dealing with this situation and as jobs with name come with a heavy processing overhead
     * these should be avoided.
     *
     * The returned job object is a snapshot of the job state taken at the time of creation. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * If the queue for processing this job is configured to drop the job, <code>null</code> is returned
     * as well.
     *
     * @param topic The required job topic.
     * @param name  Optional unique job name
     * @param properties Optional job properties. The properties must be serializable.
     * @return The new job - or <code>null</code> if the job could not be created.
     * @since 1.2
     * @deprecated
     */
    @Deprecated
    Job addJob(String topic, String name, Map<String, Object> properties);

    /**
     * Return a job based on the unique job name.
     *
     * The returned job object is a snapshot of the job state taken at the time of the call. Updates
     * to the job state are not reflected and the client needs to get a new job object using the job id.
     *
     * @return A job or <code>null</code>
     * @since 1.2
     * @deprecated
     */
    @Deprecated
    Job getJobByName(String name);

    /**
     * Return all jobs either running or scheduled.
     *
     * @param type Required parameter for the type: either all jobs, only queued or only started can be returned.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param templates A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @deprecated Use {@link #findJobs(QueryType, String, long, Map...)}
     */
    @Deprecated
    JobsIterator queryJobs(QueryType type, String topic, Map<String, Object>... templates);

    /**
     * Return all jobs either running or scheduled.
     *
     * @param type Required parameter for the type: either all jobs, only queued or only started can be returned.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param limit A positive number indicating the maximum number of jobs returned by the iterator.
     * @param templates A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @since 1.1
     * @deprecated Use {@link #findJobs(QueryType, String, long, Map...)}
     */
    @Deprecated
    JobsIterator queryJobs(QueryType type, String topic, long limit, Map<String, Object>... templates);

    /**
     * Find a job - either scheduled or active.
     * This method searches for an event with the given topic and filter properties. If more than one
     * job matches, the first one found is returned which could be any of the matching jobs.
     *
     * @param topic Topic is required.
     * @param template The map acts like a template. The searched job
     *                    must match the template (AND query).
     * @return An event or <code>null</code>
     * @deprecated Use {@link #getJob(String, Map)}
     */
    @Deprecated
    Event findJob(String topic, Map<String, Object> template);

    /**
     * Cancel this job.
     * Canceling a job might fail if the job is currently in processing.
     * @param jobId The unique identifier as found in the property {@link JobUtil#JOB_ID}.
     * @return <code>true</code> if the job could be cancelled or does not exist anymore.
     *         <code>false</code> otherwise.
     * @deprecated Use {@link #removeJobById(String)}
     */
    @Deprecated
    boolean removeJob(String jobId);

    /**
     * Cancel this job.
     * This method acts like {@link #removeJob(String)} with the exception that it waits
     * for a job to finish. The job will be removed when this method returns - however
     * this method blocks until the job is finished!
     * @param jobId The unique identifier as found in the property {@link JobUtil#JOB_ID}.
     * @deprecated Use {@link #removeJobById(String)}
     */
    @Deprecated
    void forceRemoveJob(String jobId);

    /**
     * Is job processing enabled?
     * It is possible to completely turn off job processing.
     * @deprecated This method always returns true
     */
    @Deprecated
    boolean isJobProcessingEnabled();
}
