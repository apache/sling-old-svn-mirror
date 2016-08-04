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

import com.google.common.collect.ImmutableMap;
import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.JobUpdateBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created by ieb on 23/03/2016.
 */
public class JobUpdateBuilderImpl implements JobUpdateBuilder {

    private final String jobId;
    private final Job job;
    private JobUpdate.JobUpdateCommand command;
    private final ImmutableMap.Builder<String, Object> updateProperties = ImmutableMap.builder();

    /**
     * Create a JobUpdateBuilder from a job.
     * @param job
     */
    public JobUpdateBuilderImpl(@Nonnull Job job) {
        this.job = job;
        this.jobId = null;
    }

    public JobUpdateBuilderImpl(@Nonnull String jobId) {
        this.jobId = jobId;
        this.job = null;

    }

    /**
     * Set the JobUpdateCommand
     * @param command the command.
     * @return this JobBuilder instance.
     */
    @Nonnull
    @Override
    public JobUpdateBuilder command(@Nonnull JobUpdate.JobUpdateCommand command) {
        this.command = command;
        return this;
    }

    /**
     * Set a property to update.
     * @param name the name of the property
     * @param value the value of the property which may be null. To remove the property set the value to JobUpdate.JobPropertyAction.REMOVE.
     * @return this JobBuilder instance.
     */
    @Nonnull
    @Override
    public JobUpdateBuilder put(@Nonnull String name, @Nullable Object value) {
        if ( value == null) {
            this.updateProperties.put(name, JobUpdate.JobPropertyAction.REMOVE);
        } else {
            this.updateProperties.put(name, value);
        }
        return this;
    }

    @Nonnull
    @Override
    public JobUpdateBuilder putAll(@Nonnull Map<String, Object> properties) {
        this.updateProperties.putAll(properties);
        return this;
    }


    /**
     * Build the JobUpdate.
     * @return the JobUpdate.
     */
    @Nonnull
    @Override
    public JobUpdate build() {
        if ( job != null) {
            return new JobUpdateImpl(job, command, updateProperties.build());
        } else if ( command == JobUpdate.JobUpdateCommand.ABORT_JOB || command == JobUpdate.JobUpdateCommand.STOP_JOB) {
            return new JobUpdateImpl(jobId, command);
        } else {
            throw new IllegalStateException("Only possible to abort or stop a job by ID alone ");
        }
    }

}
