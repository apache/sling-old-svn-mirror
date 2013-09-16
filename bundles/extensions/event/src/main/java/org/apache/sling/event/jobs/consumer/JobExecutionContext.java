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
public interface JobExecutionContext {

    /**
     * Report an async result.
     * @throws IllegalStateException If the job is not processed asynchronously
     *                               or if this method has already been called.
     */
    void asyncProcessingFinished(final JobStatus status);

    /**
     * Indicate that the job executor is able to report the progress
     * by providing a step count.
     * This method should only be called once, consecutive calls
     * or a call to {@link #startProgress(long)} have no effect.
     * @param steps Number of total steps or -1 if the number of
     *              steps is unknown.
     */
    void startProgress(final int steps);

    /**
     * Indicate that the job executor is able to report the progress
     * by providing an ETA.
     * This method should only be called once, consecutive calls
     * or a call to {@link #startProgress(int)} have no effect.
     * @param eta Number of seconds the process should take or
     *        -1 of it's not known now.
     */
    void startProgress(final long eta);

    /**
     * Update the progress to the current finished step.
     * This method has only effect if {@link #startProgress(int)}
     * has been called first.
     * @param step The current step.
     */
    void setProgress(final int step);

    /**
     * Update the progress to the new ETA.
     * This method has only effect if {@link #startProgress(long)}
     * has been called first.
     * @param eta The new ETA
     */
    void update(final long eta);

    /**
     * Log a message.
     * The message might contain place holders for additional arguments.
     * @param message A message
     * @param args Additional arguments
     */
    void log(final String message, final Object...args);
}
