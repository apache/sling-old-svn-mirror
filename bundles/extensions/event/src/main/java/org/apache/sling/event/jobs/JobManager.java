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


/**
 * The job manager is the heart of the job event handling.
 * It can be used to manage and monitor the queues.
 * @since 3.0
 */
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
     * This can either be all jobs, all activated (started) or all queued jobs.
     */
    enum QueryType {
        ALL,
        ACTIVE,
        QUEUED
    }

    /**
     * Return all jobs either running or scheduled.
     *
     * @param type Required parameter for the type: either all jobs, only queued or only started can be returned.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param templates A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @deprecated
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
     * @deprecated
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
     * @deprecated
     */
    @Deprecated
    Event findJob(String topic, Map<String, Object> template);

    /**
     * Cancel this job.
     * Canceling a job might fail if the job is currently in processing.
     * @param jobId The unique identifier as found in the property {@link JobUtil#JOB_ID}.
     * @return <code>true</code> if the job could be cancelled or does not exist anymore.
     *         <code>false</code> otherwise.
     * @deprecated
     */
    @Deprecated
    boolean removeJob(String jobId);

    /**
     * Cancel this job.
     * This method acts like {@link #removeJob(String)} with the exception that it waits
     * for a job to finish. The job will be removed when this method returns - however
     * this method blocks until the job is finished!
     * @param jobId The unique identifier as found in the property {@link JobUtil#JOB_ID}.
     * @deprecated
     */
    @Deprecated
    void forceRemoveJob(String jobId);

    /**
     * Restart the job manager.
     * This method restarts the job manager and all queues - currently processed jobs will be finished.
     * The job manager should only be restarted if really necessary!
     */
    void restart();

    /**
     * Is job processing enabled?
     * It is possible to completely turn off job processing.
     * @deprecated This method always returns true
     */
    @Deprecated
    boolean isJobProcessingEnabled();

    /**
     * Add a new job
     * @param topic The job topic,
     * @param name  Optional unique job name
     * @param properties Optional job properties
     * @return The new job
     * @since 1.2
     */
    Job addJob(String topic, String name, Map<String, Object> properties);

    /**
     * @return A job or <code>null</code>
     * @since 1.2
     */
    Job getJobByName(String name);

    /**
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return A job or <code>null</code>
     * @since 1.2
     */
    Job getJobById(String jobId);

    /**
     * Removes the job even if it is currently in processing.
     * If the job exists and is not in processing, it gets removed from the processing queue.
     * If the job exists and is in processing, it is removed from the persistence layer,
     * however processing is not stopped.
     * @param jobId The unique identifier from {@link Job#getId()}
     * @return <code>true</code> if the job could be removed or does not exist anymore.
     *         <code>false</code> otherwise.
     * @since 1.2
     */
    boolean removeJobById(String jobId);

    /**
     * Find a job - either scheduled or active.
     * This method searches for an event with the given topic and filter properties. If more than one
     * job matches, the first one found is returned which could be any of the matching jobs.
     *
     * @param topic Topic is required.
     * @param template The map acts like a template. The searched job
     *                    must match the template (AND query).
     * @return A job or <code>null</code>
     * @since 1.2
     */
    Job getJob(String topic, Map<String, Object> template);

    /**
     * Return all jobs either running or scheduled.
     *
     * @param type Required parameter for the type: either all jobs, only queued or only started can be returned.
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
}
