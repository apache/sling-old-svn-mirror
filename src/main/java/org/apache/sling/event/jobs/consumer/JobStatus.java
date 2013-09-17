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
package org.apache.sling.event.jobs.consumer;

/**
 * The status of a job after it has been processed by a {@link JobExecutor}.
 *
 * @since 1.1
 */
public final class JobStatus {

    /** Constant for the {@link JobState#OK} status. */
    public static final JobStatus OK = new JobStatus(JobState.OK, null);

    /** Constant for the {@link JobState#FAILED} status. */
    public static final JobStatus FAILED = new JobStatus(JobState.FAILED, null);

    /** Constant for the {@link JobState#CANCEL} status. */
    public static final JobStatus CANCEL = new JobStatus(JobState.CANCEL, null);

    /** The state of the job after processing. */
    private final JobState state;

    /** Optional message for this processing of the job. */
    private final String message;

    /** Optional override for the retry delay. */
    private final Long retryDelay;

    /**
     * Create a new job status with a state and a message.
     * @param state   The job state
     * @param message The message
     * @throws IllegalArgumentException If state is null
     */
    public JobStatus(final JobState state, final String message) {
        this(state, message, null);
    }

    /**
     * Create a new job status with a state, a message and an override for the retry delay
     * @param state   The job state
     * @param message The message
     * @param retryDelayInMs The new retry delay in ms.
     * @throws IllegalArgumentException If state is null or if retryDelayInMs is negative.
     */
    public JobStatus(final JobState state, final String message, final Long retryDelayInMs) {
        if ( state == null ) {
            throw new IllegalArgumentException("State must not be null.");
        }
        if ( retryDelayInMs != null && retryDelayInMs < 0 ) {
            throw new IllegalArgumentException("Retry delay must not be negative.");
        }
        this.state = state;
        this.message = message;
        this.retryDelay = retryDelayInMs;
    }

    /**
     * Return the state of the job
     * @return The job state.
     */
    public JobState getState() {
        return this.state;
    }

    /**
     * Return the optional message.
     * @return The message or <code>null</code>
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Return the retry delay in ms
     * @return The new retry delay (>= 0) or <code>null</code>
     */
    public Long getRetryDelayInMs() {
        return this.retryDelay;
    }

    @Override
    public String toString() {
        return "JobStatus [state=" + state + ", message=" + message
                + ", retryDelay=" + retryDelay + "]";
    }
}
