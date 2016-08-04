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
package org.apache.sling.jobs.impl;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobBuilder;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ieb on 29/03/2016.
 * Provides an implementation of a JobBuilder.
 */
public class JobBuilderImpl implements JobBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobBuilderImpl.class);
    private final String id;
    private final Map<String, Object> properties;
    private final JobStarter jobStarter;
    private final Types.JobQueue topic;
    private final Types.JobType jobType;


    public JobBuilderImpl(JobStarter jobStarter, Types.JobQueue topic, Types.JobType jobType) {
        this.jobStarter = jobStarter;
        this.topic = topic;
        this.jobType = jobType;
        this.id = Utils.generateId();
        properties = new HashMap<String, Object>();
    }


    @Nonnull
    @Override
    public JobBuilder addProperties(@Nonnull Map<String, Object> props) {
        this.properties.putAll(props);
        return this;
    }

    @Nonnull
    @Override
    public Job add() {
        return jobStarter.start(new JobImpl(topic, id, jobType, properties));
    }



}
