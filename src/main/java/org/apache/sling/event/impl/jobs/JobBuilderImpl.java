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

import java.util.Date;
import java.util.Map;

import org.apache.sling.event.impl.support.ScheduleInfo;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.slf4j.Logger;

/**
 * Fluent builder API
 */
public class JobBuilderImpl implements JobBuilder {

    private final String topic;

    private final JobManagerImpl jobManager;

    private final Logger logger;

    private String name;

    private Map<String, Object> properties;

    public JobBuilderImpl(final JobManagerImpl manager, final Logger logger, final String topic) {
        this.jobManager = manager;
        this.topic = topic;
        this.logger = logger;
    }

    @Override
    public JobBuilder name(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public JobBuilder properties(final Map<String, Object> props) {
        this.properties = props;
        return this;
    }

    @Override
    public Job add() {
        return this.jobManager.addJob(this.topic, this.name, this.properties);
    }

    @Override
    public ScheduleBuilder schedule(final String name) {
        return new ScheduleBuilderImpl(name);
    }

    public final class ScheduleBuilderImpl implements ScheduleBuilder {

        private final String scheduleName;

        private boolean suspend = false;

        public ScheduleBuilderImpl(final String name) {
            this.scheduleName = name;
        }

        private boolean check() {
            if ( this.scheduleName == null || this.scheduleName.length() == 0 ) {
                logger.warn("Discarding scheduled job - schedule name not specified");
                return false;
            }
            final String errorMessage = Utility.checkJob(topic, properties);
            if ( errorMessage != null ) {
                logger.warn("{}", errorMessage);
                return false;
            }
            return true;
        }

        @Override
        public boolean hourly(final int minutes) {
            if ( check() ) {
                if ( minutes > 0 ) {
                    final ScheduleInfo info = ScheduleInfo.HOURLY(minutes);
                    return jobManager.addScheduledJob(topic, name, properties, scheduleName, suspend, info);
                }
                logger.warn("Discarding scheduled job - minutes must be between 0 and 59 : {}", minutes);
            }
            return false;
        }

        @Override
        public TimeBuilder daily() {
            return new TimeBuilderImpl(ScheduledJobInfo.ScheduleType.DAILY, -1);
        }

        @Override
        public TimeBuilder weekly(final int day) {
            return new TimeBuilderImpl(ScheduledJobInfo.ScheduleType.WEEKLY, day);
        }

        @Override
        public boolean at(final Date date) {
            if ( check() ) {
                if ( date != null && date.getTime() > System.currentTimeMillis() ) {
                    final ScheduleInfo info = ScheduleInfo.AT(date);
                    return jobManager.addScheduledJob(topic, name, properties, scheduleName, suspend, info);
                }
                logger.warn("Discarding scheduled job - date must be in the future : {}", date);
            }
            return false;
        }

        @Override
        public ScheduleBuilder suspend(final boolean flag) {
            this.suspend = flag;
            return this;
        }

        public final class TimeBuilderImpl implements TimeBuilder {

            private final ScheduledJobInfo.ScheduleType scheduleType;

            private final int day;

            public TimeBuilderImpl(ScheduledJobInfo.ScheduleType scheduleType, final int day) {
                this.scheduleType = scheduleType;
                this.day = day;
            }

            @Override
            public boolean at(final int hour, final int minute) {
                if ( check() ) {
                    boolean valid = true;
                    if ( scheduleType == ScheduledJobInfo.ScheduleType.WEEKLY ) {
                        if ( day < 1 || day > 7 ) {
                            valid = false;
                            logger.warn("Discarding scheduled job - day must be between 1 and 7 : {}", day);
                        }
                    }
                    if ( valid ) {
                        if ( hour >= 0 && hour < 24 && minute >= 0 && minute < 60 ) {
                            final ScheduleInfo info;
                            if ( scheduleType == ScheduledJobInfo.ScheduleType.WEEKLY ) {
                                info = ScheduleInfo.WEEKLY(this.day, hour, minute);
                            } else {
                                info = ScheduleInfo.DAYLY(hour, minute);
                            }
                            return jobManager.addScheduledJob(topic, name, properties, scheduleName, suspend, info);
                        }
                        logger.warn("Discarding scheduled job - wrong time information : {}…{}", hour, minute);
                    }
                }
                return false;
            }
        }
    }
}
