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
package org.apache.sling.event.impl.jobs.scheduling;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder;
import org.apache.sling.event.jobs.ScheduleInfo;
import org.apache.sling.event.jobs.ScheduledJobInfo;

/**
 * The job schedule information.
 * It holds all required information like
 * - the name of the schedule
 * - the job topic
 * - the job properties
 * - scheduling information
 */
public class ScheduledJobInfoImpl implements ScheduledJobInfo, Serializable {

    private static final long serialVersionUID = 1L;

    private final String scheduleName;

    private final JobSchedulerImpl jobScheduler;

    private final AtomicBoolean isSuspended = new AtomicBoolean(false);

    private volatile List<ScheduleInfo> scheduleInfos;

    private volatile String jobTopic;

    private volatile Map<String, Object> jobProperties;

    /**
     * Create a new info object
     * @param jobScheduler The job scheduler
     * @param scheduleName The unique name
     */
    public ScheduledJobInfoImpl(final JobSchedulerImpl jobScheduler,
            final String scheduleName) {
        this.jobScheduler = jobScheduler;
        this.scheduleName = scheduleName;
    }

    /**
     * Update/set the job related information
     * @param jobTopic      The job topic
     * @param jobProperties The job properties
     */
    public void update(final String jobTopic,
            final Map<String, Object> jobProperties) {
        final boolean isSuspended = jobProperties.remove(ResourceHelper.PROPERTY_SCHEDULE_SUSPENDED) != null;
        @SuppressWarnings("unchecked")
        final List<ScheduleInfo> scheduleInfos = (List<ScheduleInfo>) jobProperties.remove(ResourceHelper.PROPERTY_SCHEDULE_INFO);

        this.jobTopic = jobTopic;
        this.jobProperties = jobProperties;
        this.scheduleInfos = Collections.unmodifiableList(scheduleInfos);

        this.isSuspended.set(isSuspended);
    }

    /**
     * Update the scheduling information
     * @param scheduleInfos The new schedule
     */
    public void update(final List<ScheduleInfo> scheduleInfos) {
        this.scheduleInfos =  Collections.unmodifiableList(scheduleInfos);
    }

    /**
     * Get the schedule name
     */
    public String getName() {
        return this.scheduleName;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getSchedules()
     */
    @Override
    public Collection<ScheduleInfo> getSchedules() {
        return this.scheduleInfos;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getNextScheduledExecution()
     */
    @Override
    public Date getNextScheduledExecution() {
        Date result = null;
        for(final ScheduleInfo info : this.scheduleInfos) {
            final Date newResult = ((ScheduleInfoImpl)info).getNextScheduledExecution();
            if ( result == null || result.getTime() > newResult.getTime() ) {
                result = newResult;
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getJobTopic()
     */
    @Override
    public String getJobTopic() {
        return this.jobTopic;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getJobProperties()
     */
    @Override
    public Map<String, Object> getJobProperties() {
        return this.jobProperties;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#unschedule()
     */
    @Override
    public void unschedule() {
        this.jobScheduler.removeJob(this);
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#reschedule()
     */
    @Override
    public ScheduleBuilder reschedule() {
        return this.jobScheduler.createJobBuilder(this);
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#suspend()
     */
    @Override
    public void suspend() {
        if ( this.isSuspended.compareAndSet(false, true) ) {
            this.jobScheduler.setSuspended(this, true);
        }
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#resume()
     */
    @Override
    public void resume() {
        if ( this.isSuspended.compareAndSet(true, false) ) {
            this.jobScheduler.setSuspended(this, false);
        }
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        return this.isSuspended.get();
    }

    /**
     * Get the scheduler job id
     */
    public String getSchedulerJobId() {
        return Job.class.getName() + ":" + this.scheduleName;
    }
}
