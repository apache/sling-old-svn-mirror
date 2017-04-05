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

import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobExecutor;

/**
 * The state of the job after it has been processed by a {@link JobExecutor}.
 */
public enum InternalJobState {

    SUCCEEDED(NotificationConstants.TOPIC_JOB_FINISHED),    // processing finished successfully
    FAILED(NotificationConstants.TOPIC_JOB_FAILED),         // processing failed, can be retried
    CANCELLED(NotificationConstants.TOPIC_JOB_CANCELLED);   // processing failed permanently

    private final String topic;

    InternalJobState(final String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }
}
