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

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;

/**
 * Fluent builder API
 */
public class JobBuilderImpl implements JobBuilder {

    private final String topic;

    private final JobManager jobManager;

    private String name;

    private Map<String, Object> properties;

    public JobBuilderImpl(final JobManager manager, final String topic) {
        this.jobManager = manager;
        this.topic = topic;
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
    public boolean scheduleNow() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scheduleNow(int times, long period) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scheduleAt(Date date) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scheduleAt(Date date, int times, long period) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean schedule(String expression) {
        // TODO Auto-generated method stub
        return false;
    }

}
