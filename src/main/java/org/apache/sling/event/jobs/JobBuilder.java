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

import java.util.Date;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * This is a builder interface to build jobs and scheduled jobs.
 * Instances of this class can be retrieved using {@link JobManager#createJob(String)}
 *
 * @since 1.3
 */
@ProviderType
public interface JobBuilder {

    /**
     * Set the optional job name
     */
    JobBuilder name(final String name);

    /**
     * Set the optional configuration properties for the job.
     */
    JobBuilder properties(final Map<String, Object> props);

    /**
     * Add the job.
     * @return The job or <code>null</code>
     * @see JobManager#addJob(String, Map)
     */
    Job add();

    /**
     * Add the job.
     * @param errors Optional list which will be filled with error messages.
     * @return The job or <code>null</code>
     * @see JobManager#addJob(String, Map)
     */
    Job add(final List<String> errors);

    /**
     * Schedule the job
     * If a job scheduler with the same name already exists, it is updated
     * with the new information.
     * If no name is provided (empty name or null), the job can't be scheduled.
     * @param name Unique name for the scheduler.
     * @return A schedule builder to schedule the jobs
     */
    ScheduleBuilder schedule(final String name);

    public interface ScheduleBuilderAdder {

        /**
         * Finally add the job to the schedule
         * @return Returns the info object if the job could be scheduled, <code>null</code>otherwise.
         */
        ScheduledJobInfo add();

        /**
         * Finally add the job to the schedule
         * @param errors Optional list which will be filled with error messages.
         * @return Returns the info object if the job could be scheduled, <code>null</code>otherwise.
         */
        ScheduledJobInfo add(final List<String> errors);
    }

    /**
     * This is a builder interface for creating schedule information
     */
    public interface ScheduleBuilder {

        /**
         * Suspend this scheduling by default.
         * Invoking this method several times has the same effect as calling it just once.
         */
        ScheduleBuilder suspend();

        /**
         * Schedule the job hourly at the given minute.
         * If the minutes argument is less than 0 or higher than 59, the job can't be scheduled.
         * @param minute Between 0 and 59.
         */
        MinuteBuilder hourly(final int minute);

        /**
         * Schedule the job daily at the given time.
         * If a value less than zero for hour or minute is specified or a value higher than 23 for hour or
         * a value higher than 59 for minute than the job can't be scheduled.
         * @param hour  Hour of the day ranging from 0 to 23.
         * @param minute Minute of the hour ranging from 0 to 59.
         */
        DayBuilder dayly(final int hour, final int minute);

        /**
         * Schedule the job weekly, the time needs to be specified in addition.
         * If a value lower than 1 or higher than 7 is used for the day, the job can't be scheduled.
         * If a value less than zero for hour or minute is specified or a value higher than 23 for hour or
         * a value higher than 59 for minute than the job can't be scheduled.
         * @param day Day of the week, 1:Sunday, 2:Monday, ... 7:Saturday.
         * @param hour  Hour of the day ranging from 0 to 23.
         * @param minute Minute of the hour ranging from 0 to 59.
         */
        WeekBuilder weekly(final int day, final int hour, final int minute);

        /**
         * Schedule the job for a specific date.
         * If no date or a a date in the past is provided, the job can't be scheduled.
         * @param date The date
         */
        DateBuilder at(final Date date);
    }

    public interface WeekBuilder extends ScheduleBuilderAdder {

        /**
         * Schedule the job for the given day, hour and minute.
         * If a value lower than 1 or higher than 7 is used for the day, the job can't be scheduled.
         * If a value less than zero for hour or minute is specified or a value higher than 23 for hour or
         * a value higher than 59 for minute than the job can't be scheduled.
         * @param day Day of the week, 1:Sunday, 2:Monday, ... 7:Saturday.
         * @param hour  Hour of the day ranging from 0 to 23.
         * @param minute Minute of the hour ranging from 0 to 59.
         */
        WeekBuilder at(final int day, final int hour, final int minute);
    }

    public interface DayBuilder extends ScheduleBuilderAdder  {

        /**
         * Schedule the job for the given hour and minute.
         * If a value less than zero for hour or minute is specified or a value higher than 23 for hour or
         * a value higher than 59 for minute than the job can't be scheduled.
         * @param hour  Hour of the day ranging from 0 to 23.
         * @param minute Minute of the hour ranging from 0 to 59.
         */
        DayBuilder at(final int hour, final int minute);
    }

    public interface DateBuilder extends ScheduleBuilderAdder {

        /**
         * Schedule the job for a specific date.
         * If no date or a a date in the past is provided, the job can't be scheduled.
         * @param date The date
         */
        DateBuilder at(final Date date);
    }

    public interface MinuteBuilder extends ScheduleBuilderAdder {

        /**
         * Schedule the job hourly at the given minute.
         * If the minutes argument is less than 0 or higher than 59, the job can't be scheduled.
         * @param minute Between 0 and 59.
         */
        MinuteBuilder at(final int minute);
    }
}