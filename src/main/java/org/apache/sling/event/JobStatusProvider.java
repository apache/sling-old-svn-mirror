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
package org.apache.sling.event;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.event.Event;

import aQute.bnd.annotation.ProviderType;

/**
 * This service provides the current job processing status.
 * @deprecated
 */
@Deprecated
@ProviderType
public interface JobStatusProvider {

    /**
     * Cancel this job.
     * Cancelling a job might fail if the job is currently in processing.
     * @param jobId The unique identifer as found in the property {@link #PROPERTY_EVENT_ID}.
     * @return <code>true</code> if the job could be cancelled or does not exist anymore.
     *         <code>false</code> otherwise.
     * @since 2.4.0
     */
    boolean removeJob(String jobId);

    /**
     * Cancel this job.
     * Cancelling a job might fail if the job is currently in processing.
     * This method can be used if the topic and the provided job id is known.
     * @param topic The job topic as put into the property {@link org.apache.sling.event.jobs.JobUtil#PROPERTY_JOB_TOPIC}.
     * @param jobId The unique identifer as put into the property {@link org.apache.sling.event.jobs.JobUtil#PROPERTY_JOB_NAME}.
     * @return <code>true</code> if the job could be cancelled or does not exist anymore.
     *         <code>false</code> otherwise.
     * @since 2.4.0
     */
    boolean removeJob(String topic, String jobId);

    /**
     * Cancel this job.
     * This method acts like {@link #removeJob(String)} with the exception that it waits
     * for a job to finish. The job will be removed when this method returns - however
     * this method blocks until the job is finished!
     * @param jobId The unique identifer as found in the property {@link #PROPERTY_EVENT_ID}.
     * @since 2.4.0
     */
    void forceRemoveJob(String jobId);

    /**
     * Cancel this job.
     * This method acts like {@link #removeJob(String, String)} with the exception that it waits
     * for a job to finish. The job will be removed when this method returns - however
     * this method blocks until the job is finished!
     * This method can be used if the topic and the provided job id is known.
     * @param topic The job topic as put into the property {@link org.apache.sling.event.jobs.JobUtil#PROPERTY_JOB_TOPIC}.
     * @param jobId The unique identifer as put into the property {@link org.apache.sling.event.jobs.JobUtil#PROPERTY_JOB_NAME}.
     * @since 2.4.0
     */
    void forceRemoveJob(String topic, String jobId);

    /**
     * Wake up the named job queue.
     * If a job failed, the job queue waits (sleeps) for a configured time. By calling this
     * method, the job queue can be woken up and force an immediate reprocessing.
     * @param jobQueueName The name of the queue.
     */
    void wakeUpJobQueue(final String jobQueueName);

    /**
     * Return a list of currently scheduled jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @since 2.4
     */
    JobsIterator queryScheduledJobs(String topic, Map<String, Object>... filterProps);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @since 2.4
     */
    JobsIterator queryCurrentJobs(String topic, Map<String, Object>... filterProps);

    /**
     * Return all jobs either running or scheduled.
     * This is actually a convenience method and collects the results from {@link #queryScheduledJobs(String, Map...)}
     * and {@link #queryCurrentJobs(String, Map...)}
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @since 2.4
     */
    JobsIterator queryAllJobs(String topic, Map<String, Object>... filterProps);

    /**
     * This is a unique identifer which can be used to cancel the job.
     * @deprecated Use {@link org.apache.sling.event.jobs.JobUtil#JOB_ID}
     */
    @Deprecated
    String PROPERTY_EVENT_ID = "slingevent:eventId";

    /**
     * Return a list of currently scheduled jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     * @deprecated Use {@link #queryScheduledJobs(String, Map...)} instead.
     */
    @Deprecated
    Collection<Event> getScheduledJobs(String topic);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     * @deprecated Use {@link #queryCurrentJobs(String, Map...)} instead.
     */
    @Deprecated
    Collection<Event> getCurrentJobs(String topic);

    /**
     * Return a list of currently scheduled jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @deprecated Use {@link #queryScheduledJobs(String, Map...)} instead.
     */
    @Deprecated
    Collection<Event> getScheduledJobs(String topic, Map<String, Object>... filterProps);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @deprecated Use {@link #queryCurrentJobs(String, Map...)} instead.
     */
    @Deprecated
    Collection<Event> getCurrentJobs(String topic, Map<String, Object>... filterProps);

    /**
     * Return all jobs either running or scheduled.
     * This is actually a convenience method and collects the results from {@link #getScheduledJobs(String, Map...)}
     * and {@link #getCurrentJobs(String, Map...)}
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps A list of filter property maps. Each map acts like a template. The searched job
     *                    must match the template (AND query). By providing several maps, different filters
     *                    are possible (OR query).
     * @return A non null collection.
     * @deprecated Use {@link #queryAllJobs(String, Map...)} instead.
     */
    @Deprecated
    Collection<Event> getAllJobs(String topic, Map<String, Object>... filterProps);

    /**
     * Cancel this job.
     * Cancelling a job might fail if the job is currently in processing.
     * @param jobId The unique identifer as found in the property {@link #PROPERTY_EVENT_ID}.
     * @deprecated Use {@link #removeJob(String)} instead.
     */
    @Deprecated
    void cancelJob(String jobId);

    /**
     * Cancel this job.
     * Cancelling a job might fail if the job is currently in processing.
     * This method can be used if the topic and the provided job id is known.
     * @param topic The job topic as put into the property {@link EventUtil#PROPERTY_JOB_TOPIC}.
     * @param jobId The unique identifer as put into the property {@link EventUtil#PROPERTY_JOB_ID}.
     * @deprecated Use {@link #removeJob(String, String)} instead.
     */
    @Deprecated
    void cancelJob(String topic, String jobId);
}
