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
     * @see JobManager#addJob(String, Map)
     * @return The job or <code>null</code>
     */
    Job add();

    /**
     * Schedule the job
     * If a job scheduler with the same name already exists, it is updated
     * with the new information.
     * @param name Unique name for the scheduler.
     * @return A schedule builder to schedule the jobs
     */
    ScheduleBuilder schedule(final String name);

    /**
     * This is a builder interface for creating schedule information
     */
    public interface ScheduleBuilder {

        /**
         * Schedule the job periodically every N minutes
         * @param minutes Positive number of minutes
         * @return <code>true</code> if the job could be scheduled, <code>false</code>otherwise.
         */
        boolean periodically(final int minutes);

        /**
         * Schedule the job daily, the time needs to be specified in addition.
         */
        TimeBuilder daily();

        /**
         * Schedule the job weekly, the time needs to be specified in addition.
         * @param day Day of the week, Sunday being one, Monday two, up to Saturday being seven.
         */
        TimeBuilder weekly(final int day);

        /**
         * Schedule the job for a specific date.
         * @param date The date
         * @return <code>true</code> if the job could be scheduled, <code>false</code>otherwise.
         */
        boolean at(final Date date);
    }

    public interface TimeBuilder {

        /**
         * Schedule the job for the given hour and minute.
         * @param hour  Hour of the day ranging from 0 to 23.
         * @param minute Minute of the hour ranging from 0 to 59.
         * @return <code>true</code> if the job could be scheduled, <code>false</code>otherwise.
         */
        boolean at(final int hour, final int minute);
    }
}