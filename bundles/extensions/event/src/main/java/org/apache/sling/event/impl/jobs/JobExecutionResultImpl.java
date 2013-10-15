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

import org.apache.sling.event.jobs.consumer.JobExecutionResult;

public class JobExecutionResultImpl implements JobExecutionResult {

    public static final JobExecutionResultImpl SUCCEEDED = new JobExecutionResultImpl(InternalJobState.SUCCEEDED, null, null);
    public static final JobExecutionResultImpl CANCELLED = new JobExecutionResultImpl(InternalJobState.CANCELLED, null, null);
    public static final JobExecutionResultImpl FAILED = new JobExecutionResultImpl(InternalJobState.FAILED, null, null);

    private final InternalJobState state;

    private final String message;

    private final Long retryDelayInMs;

    public JobExecutionResultImpl(final InternalJobState state,
            final String message,
            final Long retryDelayInMs) {
        this.state = state;
        this.message = message;
        this.retryDelayInMs = retryDelayInMs;
    }

    public InternalJobState getState() {
        return this.state;
    }

    @Override
    public boolean succeeded() {
        return this.state == InternalJobState.SUCCEEDED;
    }

    @Override
    public boolean cancelled() {
        return this.state == InternalJobState.CANCELLED;
    }

    @Override
    public boolean failed() {
        return this.state == InternalJobState.FAILED;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public Long getRetryDelayInMs() {
        return this.retryDelayInMs;
    }

    @Override
    public String toString() {
        return "JobExecutionResultImpl [state=" + state + ", message="
                + message + ", retryDelayInMs=" + retryDelayInMs + "]";
    }
}
