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

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.apache.sling.event.impl.support.ScheduleInfo;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder;
import org.apache.sling.event.jobs.ScheduledJobInfo;

public class ScheduledJobInfoImpl implements ScheduledJobInfo, Serializable {

    private static final long serialVersionUID = 1L;

    private final String scheduleName;

    private final String jobName;

    private final String jobTopic;

    private final Map<String, Object> jobProperties;

    private final ScheduleInfo scheduleInfo;

    private final JobSchedulerImpl jobScheduler;

    public ScheduledJobInfoImpl(final JobSchedulerImpl jobScheduler,
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final String scheduleName,
            final ScheduleInfo scheduleInfo) {
        this.jobScheduler = jobScheduler;
        this.scheduleName = scheduleName;
        this.jobName = jobName;
        this.jobTopic = jobTopic;
        this.jobProperties = jobProperties;
        this.scheduleInfo = scheduleInfo;
    }

    @Override
    public String getName() {
        return this.scheduleName;
    }

    @Override
    public ScheduleType getScheduleType() {
        return this.scheduleInfo.getScheduleType();
    }

    @Override
    public Date getNextScheduledExecution() {
        if ( this.scheduleInfo.getScheduleType() == ScheduleType.DATE ) {
            return this.scheduleInfo.getAt();
        }
        return null;
    }

    @Override
    public int getDayOfWeek() {
        return this.scheduleInfo.getDayOfWeek();
    }

    @Override
    public int getHourOfDay() {
        return this.scheduleInfo.getHourOfDay();
    }

    @Override
    public int getMinuteOfHour() {
        return this.scheduleInfo.getPeriod();
    }

    @Override
    public int getPeriod() {
        return this.scheduleInfo.getPeriod();
    }

    @Override
    public String getJobTopic() {
        return this.jobTopic;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public Map<String, Object> getJobProperties() {
        return this.jobProperties;
    }

    @Override
    public void unschedule() {
        this.jobScheduler.unschedule(this);
    }

    @Override
    public ScheduleBuilder reschedule() {
        return this.jobScheduler.createJobBuilder(this);
    }

    /**
     * Get the scheduler job id
     */
    public String getSchedulerJobId() {
        return Job.class.getName() + ":" + this.scheduleName;
    }
}
