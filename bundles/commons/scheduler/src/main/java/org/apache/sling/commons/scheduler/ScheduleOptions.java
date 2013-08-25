/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler;

import java.io.Serializable;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * Scheduler options provide an extensible way of defining how to schedule a job.
 * An option can be created via the scheduler.
 *
 * @since 2.3
 */
@ProviderType
public interface ScheduleOptions {

    /**
     * Add optional configuration for the job.
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     */
    ScheduleOptions config(final  Map<String, Serializable> config);

    /**
     * Sets the name of the job.
     * A job only needs a name if it is scheduled and should be cancelled later on. The name can then be used to cancel the job.
     * If a second job with the same name is started, the second one replaces the first one.
     * @param name The job name
     */
    ScheduleOptions name(final String name);

    /**
     * Flag indicating whether the job can be run concurrently.
     * This defaults to false.
     * @param flag Whether this job can run even if previous scheduled runs are still running.
     */
    ScheduleOptions canRunConcurrently(final boolean flag);

    /**
     * Flag indicating whether the job should only be run on the leader.
     * This defaults to false.
     * If no topology information is available (= no Apache Sling Discovery Implementation active)
     * this flag is ignored and the job is run on all instances!
     * If {@link #onSingleInstanceOnly(boolean)} or {@link #onInstancesOnly(String[])} has been called before,
     * that option is reset and overwritten by the value of this method.
     * @param flag Whether this job should only be run on the leader
     */
    ScheduleOptions onLeaderOnly(final boolean flag);

    /**
     * Flag indicating whether the job should only be run on a single instance in a cluster
     * This defaults to false.
     * If no topology information is available (= no Apache Sling Discovery Implementation active)
     * this flag is ignored and the job is run on all instances!
     * If {@link #onLeaderOnly(boolean)} or {@link #onInstancesOnly(String[])} has been called before,
     * that option is reset and overwritten by the value of this method.
     * @param flag Whether this job should only be run on a single instance.
     */
    ScheduleOptions onSingleInstanceOnly(final boolean flag);

    /**
     * List of Sling IDs this job should be run on.
     * If no topology information is available (= no Apache Sling Discovery Implementation active)
     * this flag is ignored and the job is run on all instances!
     * If {@link #onLeaderOnly(boolean)} or {@link #onSingleInstanceOnly(boolean)} has been called before,
     * that option is reset and overwritten by the value of this method.
     * @param slingIds Array of Sling IDs this job should run on
     */
    ScheduleOptions onInstancesOnly(final String[] slingIds);
}
