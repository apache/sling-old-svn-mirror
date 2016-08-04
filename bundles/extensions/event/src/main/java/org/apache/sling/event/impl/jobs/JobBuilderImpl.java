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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.event.impl.jobs.scheduling.JobScheduleBuilderImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;

/**
 * Fluent builder API
 */
public class JobBuilderImpl implements JobBuilder {

    private final String topic;

    private final JobManagerImpl jobManager;

    private Map<String, Object> properties;

    public JobBuilderImpl(final JobManagerImpl manager, final String topic) {
        this.jobManager = manager;
        this.topic = topic;
    }


    @Override
    public JobBuilder properties(final Map<String, Object> props) {
        this.properties = props;
        return this;
    }

    @Override
    public Job add() {
        return this.add(null);
    }

    @Override
    public Job add(final List<String> errors) {
        return this.jobManager.addJob(this.topic, this.properties, errors);
    }

    @Override
    public ScheduleBuilder schedule() {
        return new JobScheduleBuilderImpl(
                this.topic,
                this.properties,
                UUID.randomUUID().toString(),
                this.jobManager.getJobScheduler());
    }
}
