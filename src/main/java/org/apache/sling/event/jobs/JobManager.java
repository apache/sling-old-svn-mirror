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
     */
    JobsIterator queryJobs(QueryType type, String topic, Map<String, Object>... templates);

    /**
     * Find a job - either scheduled or active.
     * This method searches for an event with the given topic and filter properties. If more than one
     * job matches, the first one found is returned which could be any of the matching jobs.
     *
     * @param topic Topic is required.
     * @param template The map acts like a template. The searched job
     *                    must match the template (AND query).
     * @return An event or <code>null</code>
     */
    Event findJob(String topic, Map<String, Object> template);

    /**
     * Cancel this job.
     * Cancelling a job might fail if the job is currently in processing.
     * @param jobId The unique identifer as found in the property {@link JobUtil#JOB_ID}.
     * @return <code>true</code> if the job could be cancelled or does not exist anymore.
     *         <code>false</code> otherwise.
     */
    boolean removeJob(String jobId);

    /**
     * Cancel this job.
     * This method acts like {@link #removeJob(String)} with the exception that it waits
     * for a job to finish. The job will be removed when this method returns - however
     * this method blocks until the job is finished!
     * @param jobId The unique identifer as found in the property {@link JobUtil#JOB_ID}.
     */
    void forceRemoveJob(String jobId);

    /**
     * Restart the job manager.
     * This method restarts the job manager and all queues - currently processed jobs will be finished.
     * The job manager should only be restarted if really necessary!
     */
    void restart();
}
