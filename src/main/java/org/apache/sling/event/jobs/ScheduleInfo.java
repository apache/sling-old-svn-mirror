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

import aQute.bnd.annotation.ProviderType;

/**
 * Scheduling information.
 * @since 1.3
 */
@ProviderType
public interface ScheduleInfo {

    enum ScheduleType {
        DATE,         // scheduled for a date
        HOURLY,       // scheduled hourly
        DAILY,        // scheduled once a day
        WEEKLY,       // scheduled once a week
        MONTHLY,      // scheduled once a month
        YEARLY,       // scheduled once a year,
        CRON          // scheduled according to the cron expression
    }

    /**
     * Return the scheduling type
     * @return The scheduling type
     */
    ScheduleType getType();

    /**
     * Return the scheduled execution date for a schedule of type date.
     */
    Date getAt();

    /**
     * If the schedule is a cron expression, return the expression.
     * @return The cron expression or <code>null</code>
     */
    String getExpression();

    /**
     * If the job is scheduled yearly, returns the month of the year
     * @return The day of the year (from 1 to 12) or -1
     */
    int getMonthOfYear();

    /**
     * If the job is scheduled monthly, returns the day of the month
     * @return The day of the month (from 1 to 28) or -1
     */
    int getDayOfMonth();

    /**
     * If the job is scheduled weekly, returns the day of the week
     * @return The day of the week (from 1 to 7) or -1
     */
    int getDayOfWeek();

    /**
     * Return the hour of the day for daily and weekly scheduled jobs
     * @return The hour of the day (from 0 to 23) or -1
     */
    int getHourOfDay();

    /**
     * Return the minute of the hour for daily, weekly and hourly scheduled jobs.
     * @return The minute of the hour (from 0 to 59) or -1
     */
    int getMinuteOfHour();
}
