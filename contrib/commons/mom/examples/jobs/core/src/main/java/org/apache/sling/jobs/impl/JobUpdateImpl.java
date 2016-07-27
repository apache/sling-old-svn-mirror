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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.MapValueAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents messages sent to the Job via a message queue.
 * Abort, stop and update messages should be sent via a priority queue.
 * Start messages should be sent by a processing queue.
 * Created by ieb on 23/03/2016.
 */
public class JobUpdateImpl implements MapValueAdapter,  JobUpdate {
    private static final long TTL = 1000 * 60;
    private long updateTimestamp;
    private long expires;
    private JobUpdateCommand command;
    private Types.JobQueue jobQueue;
    private String id;
    private Job.JobState jobState;
    private Map<String, Object> properties;
    private int retryCount;
    private int numberOfRetries;
    private long startedAt;
    private long createdAt;
    private long finishedAt;
    private String resultMessage;
    private Types.JobType jobType;


    /**
     * Create an update message using a job, command and update properties. Only the update properties will in the update.
     * The job will be used to specify the job jobQueue, job ID and job state of the update message.
     * @param job the job
     * @param command the command
     * @param properties properties in the update message.
     */
    public JobUpdateImpl(@Nonnull Job job, @Nonnull JobUpdateCommand command, @Nonnull Map<String, Object> properties) {
        Preconditions.checkNotNull(job, "Job argument cant be null");
        Preconditions.checkNotNull(command, "JobCommand argument cant be null");
        Preconditions.checkNotNull(properties, "Map of properties cant be null");

        jobQueue = job.getQueue();
        jobType = job.getJobType();
        id = job.getId();
        startedAt = job.getStarted();
        createdAt = job.getCreated();
        finishedAt = job.getFinished();
        retryCount = job.getRetryCount();
        jobState = job.getJobState();
        resultMessage = job.getResultMessage();
        numberOfRetries = job.getNumberOfRetries();
        updateTimestamp = System.currentTimeMillis();
        expires = updateTimestamp + TTL;
        this.command = command;
        this.properties = properties;
    }

    /**
     * Create a JobUpdateImpl based on a inbound message in the form of a Map.
     * @param message a inbound message in map form.
     */
    public JobUpdateImpl(@Nonnull Map<String, Object> message) {
        Preconditions.checkNotNull(message, "Message cant be null");
        fromMapValue(message);
    }

    public JobUpdateImpl(@Nonnull String jobId, @Nonnull JobUpdateCommand command) {
        Preconditions.checkNotNull(jobId, "JobId argument cant be null");
        Preconditions.checkNotNull(command, "JobUpdateCommand argument cant be null");
        jobQueue = Types.ANY_JOB_QUEUE;
        id = jobId;
        updateTimestamp = System.currentTimeMillis();
        expires = updateTimestamp + TTL;
        jobState = Job.JobState.ANY_STATE;
        this.command = command;
        this.properties = ImmutableMap.of();

    }


    @Override
    public long updateTimestamp() {
        return updateTimestamp;
    }

    @Override
    public long expires() {
        return expires;
    }

    @Nonnull
    @Override
    public Types.JobType getJobType() {
        return jobType;
    }

    @Nonnull
    @Override
    public JobUpdateCommand getCommand() {
        return command;
    }

    @Nonnull
    @Override
    public Types.JobQueue getQueue() {
        return jobQueue;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public Job.JobState getState() {
        return jobState;
    }

    @Nonnull
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    @Override
    public long getStarted() {
        return startedAt;
    }

    @Override
    public long getCreated() {
        return createdAt;
    }

    @Override
    public long getFinished() {
        return finishedAt;
    }

    @Override
    public String getResultMessage() {
        return resultMessage;
    }


    @Override
    public void fromMapValue(@Nullable Object mapValue) {
        if (mapValue != null && mapValue instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) mapValue;
            jobQueue = Types.jobQueue((String) Utils.getRequired(m, "tp"));
            jobType = Types.jobType((String)Utils.getRequired(m, "jt"));
            id = Utils.getRequired(m, "id");
            command = JobUpdateCommand.valueOf((String) Utils.getRequired(m, "cm"));
            updateTimestamp = Utils.getRequired(m, "ts");
            expires =  Utils.getRequired(m, "ex");
            if (command == JobUpdateCommand.UPDATE_JOB || command == JobUpdateCommand.START_JOB || command == JobUpdateCommand.RETRY_JOB ) {
                startedAt = Utils.getOptional(m, "startedAt", 0L);
                createdAt = Utils.getOptional(m, "createdAt", 0L);
                finishedAt = Utils.getOptional(m, "finishedAt", 0L);
                retryCount = Utils.getOptional(m, "retryCount", 0);
                numberOfRetries = Utils.getOptional(m, "nRetries", 10);
                jobState = Job.JobState.valueOf(Utils.getOptional(m, "jobState", Job.JobState.QUEUED.toString()));
                resultMessage = Utils.getOptional(m, "resultMessage", null);
                properties = Utils.getOptional(m, "properties", new HashMap<String, Object>());
            } else {
                properties = new HashMap<String, Object>();
            }
        } else {
            throw new IllegalArgumentException("Cant populate JobImpl from "+mapValue);
        }
    }

    @Override
    @Nonnull
    public Object toMapValue() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        builder.put("tp", jobQueue.toString());
        builder.put("jt",jobType.toString());
        builder.put("id",id);
        builder.put("cm", command.toString());
        builder.put("ts", this.updateTimestamp);
        builder.put("ex", expires);
        if ( command == JobUpdateCommand.UPDATE_JOB || command == JobUpdateCommand.START_JOB || command == JobUpdateCommand.RETRY_JOB ) {
            builder.put("retryCount", retryCount);
            builder.put("nRetries", numberOfRetries);
            builder.put("startedAt", startedAt);
            builder.put("createdAt", createdAt);
            builder.put("finishedAt", finishedAt);
            builder.put("jobState", jobState.toString());
            builder.put("resultMessage", resultMessage);
            builder.put("properties", ImmutableMap.builder().putAll(properties).build());

        }
        return builder.build();
    }
}
