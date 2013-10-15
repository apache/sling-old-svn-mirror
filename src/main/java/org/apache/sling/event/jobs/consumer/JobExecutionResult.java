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
 *
 * A job executor can either use one of the constants {@link #SUCCEEDED}, {@link #FAILED}
 * or {@link #CANCELLED} to return a result or it can build an individual result
 * with optional parameters using the builder methods {@link #SUCCEEDED()}, {@link #FAILED()}
 * or {@link #CANCELLED()}.
 *
 * @since 1.1
 */
@ProviderType
public interface JobExecutionResult {

    boolean succeeded();

    boolean cancelled();

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
