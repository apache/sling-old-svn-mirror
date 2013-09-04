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

import aQute.bnd.annotation.ProviderType;



/**
 *
 * @since 1.1
 */
@ProviderType
public final class JobStatus {

    enum JobState {
        OK,      // processing finished
        FAILED,  // processing failed, can be retried
        CANCEL,  // processing failed permanently
        ASYNC    // processing will be done async
    }

    private final JobState state;

    private final String message;

    private Long retryDelay;

    public JobStatus(final JobState result) {
        this(result, null);
    }

    public JobStatus(final JobState result, final String message) {
        this.state = result;
        this.message = message;
    }

    public JobState getState() {
        return this.state;
    }

    public String getMessage() {
        return this.message;
    }

    /**
     * Return the retry delay in ms
     */
    public Long getRetryDelayInMs() {
        return this.retryDelay;
    }

    public void setRetryDelayInMs(final Long value) {
        this.retryDelay = value;
    }
}
