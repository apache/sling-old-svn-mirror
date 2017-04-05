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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.sling.event.impl.support.ScheduleInfoImpl;
import org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder;
import org.apache.sling.event.jobs.ScheduledJobInfo;

/**
 * The builder implementation for scheduled jobs.
 */
public final class JobScheduleBuilderImpl implements ScheduleBuilder {

    private final String topic;

    private final Map<String, Object> properties;

    private final String scheduleName;

    private final JobSchedulerImpl jobScheduler;

    private volatile boolean suspend = false;

    private final List<ScheduleInfoImpl> schedules = new ArrayList<ScheduleInfoImpl>();

    public JobScheduleBuilderImpl(
            final String topic,
            final Map<String, Object> properties,
            final String name,
            final JobSchedulerImpl jobScheduler) {
        this.topic = topic;
        this.properties = properties;
        this.scheduleName = name;
        this.jobScheduler = jobScheduler;
    }

    @Override
    public ScheduleBuilder weekly(final int day, final int hour, final int minute) {
        schedules.add(ScheduleInfoImpl.WEEKLY(day, hour, minute));
        return this;
    }

    @Override
    public ScheduleBuilder daily(final int hour, final int minute) {
        schedules.add(ScheduleInfoImpl.DAILY(hour, minute));
        return this;
    }

    @Override
    public ScheduleBuilder hourly(final int minute) {
        schedules.add(ScheduleInfoImpl.HOURLY(minute));
        return this;
    }

    @Override
    public ScheduleBuilder at(final Date date) {
        schedules.add(ScheduleInfoImpl.AT(date));
        return this;
    }

    @Override
    public ScheduleBuilder monthly(final int day, final int hour, final int minute) {
        schedules.add(ScheduleInfoImpl.MONTHLY(day, hour, minute));
        return this;
    }

    @Override
    public ScheduleBuilder yearly(final int month, final int day, final int hour, final int minute) {
        schedules.add(ScheduleInfoImpl.YEARLY(month, day, hour, minute));
        return this;
    }

    @Override
    public ScheduleBuilder cron(final String expression) {
        schedules.add(ScheduleInfoImpl.CRON(expression));
        return this;
    }

    @Override
    public ScheduledJobInfo add() {
        return this.add(null);
    }

    @Override
    public ScheduledJobInfo add(final List<String> errors) {
        return this.jobScheduler.addScheduledJob(topic,
                properties,
                scheduleName,
                suspend,
                schedules,
                errors);
    }

    @Override
    public ScheduleBuilder suspend() {
        this.suspend = true;
        return this;
    }
}
