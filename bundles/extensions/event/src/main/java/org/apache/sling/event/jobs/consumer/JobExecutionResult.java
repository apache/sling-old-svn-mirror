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
 * The status of a job after it has been processed by a {@link JobExecutor}.
 * The job executor uses the {@link JobExecutionContext} to create a result object.
 *
 * The result can have three states, succeeded, cancelled or failed whereas
 * failed means that the execution is potentially retried.
 *
 * @since 1.1
 */
@ProviderType
public interface JobExecutionResult {

    /**
     * If this returns true the job processing finished successfully.
     * In this case {@link #cancelled()} and {@link #failed()} return
     * <code>false</code>
     * @return <code>true</code> for a successful processing
     */
    boolean succeeded();

    /**
     * If this returns true the job processing failed permanently.
     * In this case {@link #succeeded()} and {@link #failed()} return
     * <code>false</code>
     * @return <code>true</code> for a permanently failed processing
     */
    boolean cancelled();

    /**
     * If this returns true the job processing failed but might be
     * retried..
     * In this case {@link #cancelled()} and {@link #succeeded()} return
     * <code>false</code>
     * @return <code>true</code> for a failedl processing
     */
    boolean failed();

    /**
     * Return the optional message.
     * @return The message or <code>null</code>
     */
    String getMessage();

    /**
     * Return the retry delay in ms
     * @return The new retry delay (>= 0) or <code>null</code>
     */
    Long getRetryDelayInMs();
}
