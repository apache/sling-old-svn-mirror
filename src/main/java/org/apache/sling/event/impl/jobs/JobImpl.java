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

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.JobUtil.JobPriority;
import org.apache.sling.event.jobs.Queue;

/**
 * This object encapsulates all information about a job.
 */
public class JobImpl implements Job {

    /** Internal job property containing the resource path. */
    public static final String PROPERTY_RESOURCE_PATH = "slingevent:path";

    /** Internal job property if this is an bridged event (event admin). */
    public static final String PROPERTY_BRIDGED_EVENT = "slingevent:eventadmin";

    private final ValueMap properties;

    private final String topic;

    private final String path;

    private final String name;

    private final String jobId;

    private final boolean isBridgedEvent;

    private final List<Exception> readErrorList;

    /**
     * Create a new job instance
     *
     * @param topic The job topic
     * @param name  The unique job name (optional)
     * @param jobId The unique (internal) job id
     * @param properties Non-null map of properties, at least containing {@link #PROPERTY_RESOURCE_PATH}
     */
    @SuppressWarnings("unchecked")
    public JobImpl(final String topic,
                   final String name,
                   final String jobId,
                   final Map<String, Object> properties) {
        this.topic = topic;
        this.name = name;
        this.jobId = jobId;
        this.path = (String)properties.remove(PROPERTY_RESOURCE_PATH);
        this.isBridgedEvent = properties.remove(PROPERTY_BRIDGED_EVENT) != null;
        this.readErrorList = (List<Exception>) properties.remove(ResourceHelper.PROPERTY_MARKER_READ_ERROR_LIST);

        this.properties = new ValueMapDecorator(properties);
        this.properties.put(JobUtil.NOTIFICATION_PROPERTY_JOB_ID, jobId);
    }

    /**
     * Get the full resource path.
     */
    public String getResourcePath() {
        return this.path;
    }

    /**
     * Is this a bridged event?
     */
    public boolean isBridgedEvent() {
        return this.isBridgedEvent;
    }

    /**
     * Did we have read errors?
     */
    public boolean hasReadErrors() {
        return this.readErrorList != null;
    }

    /**
     * Get all properties
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Update the information for a retry
     */
    public void retry() {
        final int retries = this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT, Integer.class);
        this.properties.put(Job.PROPERTY_JOB_RETRY_COUNT, retries + 1);
        this.properties.remove(Job.PROPERTY_JOB_STARTED_TIME);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getTopic()
     */
    @Override
    public String getTopic() {
        return this.topic;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getId()
     */
    @Override
    public String getId() {
        return this.jobId;
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(final String name) {
        return this.properties.get(name);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getProperty(final String name, final Class<T> type) {
        return this.properties.get(name, type);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T getProperty(final String name, final T defaultValue) {
        return this.properties.get(name, defaultValue);
    }

    /**
     * @see org.apache.sling.event.jobs.Job#getPropertyNames()
     */
    @Override
    public Set<String> getPropertyNames() {
        return this.properties.keySet();
    }

    @Override
    public JobPriority getJobPriority() {
        return (JobPriority)this.getProperty(Job.PROPERTY_JOB_PRIORITY);
    }

    @Override
    public int getRetryCount() {
        return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
    }

    @Override
    public int getNumberOfRetries() {
        return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRIES);
    }

    @Override
    public String getQueueName() {
        return (String)this.getProperty(Job.PROPERTY_JOB_QUEUE_NAME);
    }

    @Override
    public String getTargetInstance() {
        return (String)this.getProperty(Job.PROPERTY_JOB_TARGET_INSTANCE);
    }

    @Override
    public Calendar getProcessingStarted() {
        return (Calendar)this.getProperty(Job.PROPERTY_JOB_STARTED_TIME);
    }

    @Override
    public Calendar getCreated() {
        return (Calendar)this.getProperty(Job.PROPERTY_JOB_CREATED);
    }

    @Override
    public String getCreatedInstance() {
        return (String)this.getProperty(Job.PROPERTY_JOB_CREATED_INSTANCE);
    }

    /**
     * Update information about the queue.
     */
    public void updateQueue(final Queue queue) {
        this.properties.put(Job.PROPERTY_JOB_QUEUE_NAME, queue.getName());
        this.properties.put(Job.PROPERTY_JOB_RETRIES, queue.getConfiguration().getMaxRetries());
        this.properties.put(Job.PROPERTY_JOB_PRIORITY, queue.getConfiguration().getPriority());
    }

    public void setProperty(final String name, final Object value) {
        if ( value == null ) {
            this.properties.remove(name);
        } else {
            this.properties.put(name, value);
        }
    }

    @Override
    public String toString() {
        return "JobImpl [properties=" + properties + ", topic=" + topic
                + ", path=" + path + ", name=" + name + ", jobId=" + jobId
                + ", isBridgedEvent=" + isBridgedEvent + "]";
    }
}
