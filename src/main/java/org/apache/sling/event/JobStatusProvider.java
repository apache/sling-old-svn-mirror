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

/**
 * This service provides the current job processing status.
 */
public interface JobStatusProvider {

    /**
     * This is a unique identifer which can be used to cancel the job.
     */
    String PROPERTY_EVENT_ID = "slingevent:eventId";

    /**
     * @deprecated Use {@link #getScheduledJobs(String)} instead.
     */
    @Deprecated
    Collection<Event> scheduledJobs(String topic);

    /**
     * Return a list of currently schedulded jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     */
    Collection<Event> getScheduledJobs(String topic);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @return A non null collection.
     */
    Collection<Event> getCurrentJobs(String topic);

    /**
     * Return a list of currently schedulded jobs.
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps An optional map of filter props that act like a template.
     * @return A non null collection.
     */
    Collection<Event> getScheduledJobs(String topic, Map<String, Object> filterProps);

    /**
     * Return the jobs which are currently in processing. If there are several application nodes
     * in the cluster, there could be more than one job in processing
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps An optional map of filter props that act like a template.
     * @return A non null collection.
     */
    Collection<Event> getCurrentJobs(String topic, Map<String, Object> filterProps);

    /**
     * Return all jobs either running or scheduled.
     * This is actually a convenience method and collects the results from {@link #getScheduledJobs(String, Map)}
     * and {@link #getCurrentJobs(String, Map)}
     * @param topic Topic can be used as a filter, if it is non-null, only jobs with this topic will be returned.
     * @param filterProps An optional map of filter props that act like a template.
     * @return A non null collection.
     */
    Collection<Event> getAllJobs(String topic, Map<String, Object> filterProps);

    /**
     * Cancel this job.
     * @param jobId The unique identifer as found in the property {@link #PROPERTY_EVENT_ID}.
     */
    void cancelJob(String jobId);

    /**
     * Cancel this job.
     * This method can be used if the topic and the provided job id is known.
     * @param topic The job topic as put into the property {@link EventUtil#PROPERTY_JOB_TOPIC}.
     * @param jobId The unique identifer as put into the property {@link EventUtil#PROPERTY_JOB_ID}.
     */
    void cancelJob(String topic, String jobId);
}
