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

import javax.annotation.Nonnull;

/**
 * Components that implement JobConsumers are registered with the Job Sub System by an OSGi Whiteboard pattern. Once
 * registered the component will be offered jobs matching the job types set as the OSGi property JobConsumer.JOB_TYPES.
 * If the consumer accepts the job, by not throwing and exception on the execute method, it agrees to ensure that the job
 * is executed. It may queue the job, but if queued it must ensure the job is executed. A JobConsumer should check that he
 * dependencies required to execute the job are satisfied before accepting the job for execution. Queueing jobs in the job consumer
 * to ensure that the execution threads of the JobConsumer have work available is ok, but queuing jobs to remove them from
 * the job queue is to be discouraged, as it may require too much from the JobConsumer implementation to guarantee the contract
 * in this interface.
 */
public interface JobConsumer {

    String JOB_TYPES = "job.types";

    /**
     * Execute the job, given an initial state and supply a JobStateListener to allow the job to send updates of state change
     * @param initialState the initial state of the Job.
     * @param listener a listener to propagate JobUpdates.
     * @param callback called when the job is completed.
     * @throws RuntimeException or any subclass when the Job offered in initialState cant be accepted for execution.
     */
    @Nonnull
    void execute(@Nonnull Job initialState, @Nonnull JobUpdateListener listener, @Nonnull JobCallback callback);
}
