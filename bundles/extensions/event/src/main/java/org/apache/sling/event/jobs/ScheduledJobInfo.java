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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * Information about a scheduled job
 * @since 1.3
 */
@ProviderType
public interface ScheduledJobInfo {

    /**
     * Get all schedules for this job
     * @return A non null and non empty list of schedules.
     */
    Collection<ScheduleInfo> getSchedules();

    /**
     * Return the next scheduled execution date.
     */
    Date getNextScheduledExecution();

    /**
     * Return the job topic.
     * @return The job topic
     */
    String getJobTopic();

    /**
     * Return the optional job topics.
     * @return The job topics or <code>null</code>
     */
    Map<String, Object> getJobProperties();

    /**
     * Unschedule this scheduled job.
     */
    void unschedule();

    /**
     * Reschedule this job with a new rescheduling information.
     * If rescheduling fails (due to wrong arguments), the job
     * schedule is left as is.
     */
    JobBuilder.ScheduleBuilder reschedule();

    /**
     * Suspend this job scheduling.
     * Job scheduling can be resumed with {@link #resume()}.
     * This information is persisted and survives a restart.
     */
    void suspend();

    /**
     * Resume job processing. {@link #suspend()}. If the queue is not
     * suspended, calling this method has no effect.
     */
    void resume();

    /**
     * Is the processing currently suspended?
     */
    boolean isSuspended();
}
