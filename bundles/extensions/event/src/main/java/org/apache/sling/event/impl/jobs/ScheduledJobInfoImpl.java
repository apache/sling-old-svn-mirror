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
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final JobSchedulerImpl jobScheduler;

    private ScheduleInfo scheduleInfo;

    private AtomicBoolean isSuspended;

    public ScheduledJobInfoImpl(final JobSchedulerImpl jobScheduler,
            final String jobTopic,
            final String jobName,
            final Map<String, Object> jobProperties,
            final String scheduleName) {
        this.jobScheduler = jobScheduler;
        this.scheduleName = scheduleName;
        this.jobName = jobName;
        this.jobTopic = jobTopic;
        this.jobProperties = jobProperties;
    }

    public void update(final boolean isSuspended,
            final ScheduleInfo scheduleInfo) {
        this.scheduleInfo = scheduleInfo;
        this.isSuspended = new AtomicBoolean(isSuspended);
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getName()
     */
    @Override
    public String getName() {
        return this.scheduleName;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getScheduleType()
     */
    @Override
    public ScheduleType getScheduleType() {
        return this.scheduleInfo.getScheduleType();
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getNextScheduledExecution()
     */
    @Override
    public Date getNextScheduledExecution() {
        final Calendar now = Calendar.getInstance();
        switch ( this.scheduleInfo.getScheduleType() ) {
            case DATE : return this.scheduleInfo.getAt();
            case DAILY : final Calendar next = Calendar.getInstance();
                         next.set(Calendar.HOUR_OF_DAY, this.getHourOfDay());
                         next.set(Calendar.MINUTE, this.getMinuteOfHour());
                         if ( next.before(now) ) {
                             next.add(Calendar.DAY_OF_WEEK, 1);
                         }
                         return next.getTime();
            case WEEKLY : final Calendar nextW = Calendar.getInstance();
                          nextW.set(Calendar.HOUR_OF_DAY, this.getHourOfDay());
                          nextW.set(Calendar.MINUTE, this.getMinuteOfHour());
                          nextW.set(Calendar.DAY_OF_WEEK, this.getDayOfWeek());
                          if ( nextW.before(now) ) {
                              nextW.add(Calendar.WEEK_OF_YEAR, 1);
                          }
                          return nextW.getTime();
            case PERIODICALLY : final Calendar nextP = Calendar.getInstance();
                                nextP.add(Calendar.MINUTE, this.getPeriod()); // TODO - this is not correct
                                return nextP.getTime();
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getDayOfWeek()
     */
    @Override
    public int getDayOfWeek() {
        return this.scheduleInfo.getDayOfWeek();
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getHourOfDay()
     */
    @Override
    public int getHourOfDay() {
        return this.scheduleInfo.getHourOfDay();
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getMinuteOfHour()
     */
    @Override
    public int getMinuteOfHour() {
        return this.scheduleInfo.getPeriod();
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getPeriod()
     */
    @Override
    public int getPeriod() {
        return this.scheduleInfo.getPeriod();
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getJobTopic()
     */
    @Override
    public String getJobTopic() {
        return this.jobTopic;
    }

    /**
     * @see org.apache.sling.event.jobs.ScheduledJobInfo#getJobName()
     */
    @Override
    public String getJobName() {
        return this.jobName;
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
        this.jobScheduler.unschedule(this);
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

    /**
     * If the job is scheduled daily or weekly, return the cron expression
     */
    public String getCronExpression() {
        if ( this.scheduleInfo.getScheduleType() == ScheduleType.DAILY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(this.scheduleInfo.getMinuteOfHour());
            sb.append(' ');
            sb.append(this.scheduleInfo.getHourOfDay());
            sb.append(" * * *");
            return sb.toString();
        } else if ( this.scheduleInfo.getScheduleType() == ScheduleType.WEEKLY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(this.scheduleInfo.getMinuteOfHour());
            sb.append(' ');
            sb.append(this.scheduleInfo.getHourOfDay());
            sb.append(" * * ");
            sb.append(this.scheduleInfo.getDayOfWeek());
            return sb.toString();
        }
        return null;
    }
}
