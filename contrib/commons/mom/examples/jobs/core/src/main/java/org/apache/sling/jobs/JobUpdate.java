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
package org.apache.sling.jobs;

import org.apache.sling.mom.TopicManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Job updates are messages that update the state of the job at all subscribers.
 * The distribution mechanism for JobUpdate messages must ensure that the messages are distributed appropriately so that
 * the queues that contain the messages allow the messages to be acted on in an appropriate manner. For instance if the
 * implementation decides that it will use a message queue to queue jobs as JobUpdate messages, then it may need to
 * put the JobUpdates that start a message on a separate queue from the JobUpdates that update, abort or stop a job, so that
 * those messages do not get delayed by a backlog of job start messages. Alternatively the implementation may decide to implement
 * a scheduler that consumes job start messages at full queue throughput and queue jobs into a separate implementation specific queue.
 * <p/>
 * The API does not specify the implementation, only that the various JobUpdate messages are delivered with an delay appropriate for the
 * type of message, identified by the JobUpdateCommand type.
 */
public interface JobUpdate {

    /**
     * The time of the update. The implementation may choose to adjust its concept of time to
     * ensure the that JobUpdates it emits guaranteed to be applied in the correct order. How
     * it achieves this is an implementation detail. Consumers of JobUpdates should define a policy
     * on how updates are applied. Out of sequence updates may or may not be applied to a job, depending on
     * the implementation.
     * @return the timestamp of the update.
     */
    long updateTimestamp();


    /**
     * Update messages may have a time to live. Where the update command is abort or stop and the job message doesn't
     * exist on the receiver the update message may be stored until the TTL expires before it is applied. This allows
     * messages sent out of order to wait till expires has been reached before applying the update. The rules determining
     * how expiring messages are applied is an implementation detail.
     * @return
     */
    long expires();

    /**
     * The Job type. A job type is used to identify the type of job and hence the JobConsumer (via JobTypeValve) that can
     * execute the Job.
     * @return the job type.
     */
    @Nonnull
    Types.JobType getJobType();

    enum JobUpdateCommand {
        START_JOB,       // Starts the job.
        UPDATE_JOB,     // Update the job.
        ABORT_JOB,      // Abort the job, if the receiver is running the job, then it must abort it.
        RETRY_JOB,      // Retry the job.
        STOP_JOB        // Stop the job from running, but if already running let the job complete
        ;

        public org.apache.sling.mom.Types.CommandName asCommandName() {
            return org.apache.sling.mom.Types.commandName(toString());
        }
    }


    /**
     * @return the type of update.
     */
    @Nonnull
    JobUpdateCommand  getCommand();


    /**
     * The job queue. - immutable.
     * @return The job queue
     */
    @Nonnull
    Types.JobQueue getQueue();

    /**
     * Unique job ID. immutable.
     * @return The unique job ID.
     */
    @Nonnull
    String getId();

    /**
     *
     * @return the new state of the job, may not have changed from the old state.
     */
    @Nonnull
    Job.JobState getState();

    enum JobPropertyAction {
        REMOVE
    }

    /**
     * The changed properties of the job. A property value of JobPropertyAction.REMOVE causes the property to be removed.
     * Property types cant be changed in 1 message.
     * @return the map of property changes, will be <code>null</code> if the update message does not contain property changes.
     */
    @Nonnull
    Map<String, Object> getProperties();

    /**
     * On first execution the value of this property is zero.
     * This property is managed by the job handling.
     */
    int getRetryCount();

    /**
     * The property to track the retry maximum retry count for jobs.
     * This property is managed by the job handling.
     */
    int getNumberOfRetries();


    /**
     * The time when the job started.
     */
    long getStarted();

    /**
     * @return  The time when the job was created.
     */
    long getCreated();


    /**
     * If the job is cancelled or succeeded, this method will return the finish date.
     * @return The finish date or <code>null</code>
     */
    long getFinished();

    /**
     * This method returns the message from the last job processing, regardless
     * whether the processing failed, succeeded or was cancelled. The message
     * is optional and can be set by a job consumer.
     * @return The result message or <code>null</code>
     */
    @Nullable
    String getResultMessage();

}
