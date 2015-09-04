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
package org.apache.sling.event.impl.jobs.queues;

import org.apache.sling.event.impl.jobs.InternalJobState;
import org.apache.sling.event.jobs.consumer.JobExecutionContext.ResultBuilder;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;

public class ResultBuilderImpl implements ResultBuilder {

    private volatile String message;

    private volatile Long retryDelayInMs;

    @Override
    public JobExecutionResult failed(final long retryDelayInMs) {
        this.retryDelayInMs = retryDelayInMs;
        return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
    }

    @Override
    public ResultBuilder message(final String message) {
        this.message = message;
        return this;
    }

    @Override
    public JobExecutionResult succeeded() {
        return new JobExecutionResultImpl(InternalJobState.SUCCEEDED, message, retryDelayInMs);
    }

    @Override
    public JobExecutionResult failed() {
        return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
    }

    @Override
    public JobExecutionResult cancelled() {
        return new JobExecutionResultImpl(InternalJobState.CANCELLED, message, retryDelayInMs);
    }
}
