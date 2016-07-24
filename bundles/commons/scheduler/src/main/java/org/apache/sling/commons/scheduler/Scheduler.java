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
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A scheduler to schedule time/cron based jobs.
 * A job is an object that is executed/fired by the scheduler. The object
 * should either implement the {@link Job} interface or the {@link Runnable}
 * interface.
 *
 * A job can be scheduled either by creating a {@link ScheduleOptions} instance
 * through one of the scheduler methods and then calling {@link #schedule(Object, ScheduleOptions)}
 * or
 * by using the whiteboard pattern and registering a Runnable service with either
 * the {@link #PROPERTY_SCHEDULER_EXPRESSION} or {@link #PROPERTY_SCHEDULER_PERIOD}
 * property. Services registered by the whiteboard pattern can by default run concurrently,
 * which usually is not wanted. Therefore it is advisable to also set the
 * {@link #PROPERTY_SCHEDULER_CONCURRENT} property with Boolean.FALSE.
 *
 * Jobs started through  the scheduler API are not persisted and are not
 * restarted after a bundle restart. If the client bundle is stopped, the scheduler
 * will stop all jobs started by this bundle as well. However, the client bundle does
 * not need to keep a reference to the scheduler service.
 */
@ProviderType
public interface Scheduler {

    /** Name of the configuration property to define the period for a job.
     * The period is expressed in seconds.
     * This property needs to be of type Long.
     */
    String PROPERTY_SCHEDULER_PERIOD = "scheduler.period";

    /** Name of the configuration property to define if a periodically job
     * should be scheduled immediate.
     * Default is to not startup immediate, the job is started the first time
     * after the period has expired.
     * This property needs to be of type Boolean.
     * @since 2.2.0 .*/
    String PROPERTY_SCHEDULER_IMMEDIATE = "scheduler.immediate";

    /** Name of the configuration property to define the cron expression for a job. */
    String PROPERTY_SCHEDULER_EXPRESSION = "scheduler.expression";

    /** Name of the configuration property to define if the job can be run concurrently. */
    String PROPERTY_SCHEDULER_CONCURRENT = "scheduler.concurrent";

    /** Name of the configuration property to define the job name. */
    String PROPERTY_SCHEDULER_NAME = "scheduler.name";

    /** Name of the optional configuration property to define the number of times the job
     * should be executed when {@link #PROPERTY_SCHEDULER_PERIOD} is defined.
     * This property is of type integer and must have a positive value.
     */
    String PROPERTY_SCHEDULER_TIMES = "scheduler.times";

    /**
     * Name of the configuration property to define the instances this job should run on.
     * By default a job is run on all instances. This property can be configured with:
     * - a list of Sling IDs : in that case the job is only run on instances in this set.
     * - constant {@link #VALUE_RUN_ON_LEADER} : the job is only run on the leader
     * - constant {@link #VALUE_RUN_ON_SINGLE} : the job is only run on a single instance in a cluster. This is
     *                     basically the same as {@link #VALUE_RUN_ON_LEADER} but it's not further specified which
     *                     single instance is used.
     * Default is to start the job on all instances. This property needs to be of type String
     * or String[].
     * If no topology information is available (= no Apache Sling Discovery Implementation active)
     * this option is ignored, and the job is run on all instances.
     * @since 2.3.0
     */
    String PROPERTY_SCHEDULER_RUN_ON = "scheduler.runOn";

    /** Value for {@link #PROPERTY_SCHEDULER_RUN_ON} to run the job on the leader only.
     * @since 2.3.0
     */
    String VALUE_RUN_ON_LEADER = "LEADER";

    /** Value for {@link #PROPERTY_SCHEDULER_RUN_ON} to run the job on a single instance only.
     * @since 2.3.0
     */
    String VALUE_RUN_ON_SINGLE = "SINGLE";

    /**
     * Name of the configuration property to define the thread pool to be used.
     * Scheduled jobs can run using different thread pools. By default, the default
     * thread pool of the scheduler is used.
     * If a thread pool name is specified, it is up to the scheduler to put the job
     * in the defined thread pool or any other thread pool.
     * This option must be used with special care as it might create new thread pools.
     * It should only be used if there is a good reason to not use the default thread
     * pool.
     * @since 2.5.0
     */
    String PROPERTY_SCHEDULER_THREAD_POOL = "scheduler.threadPool";

    /**
     * Schedule a job based on the options.
     *
     * Note that if a job with the same name has already been added, the old job is
     * cancelled and this new job replaces
     * the old job.
     *
     * The job object needs either to be a {@link Job} or a {@link Runnable}. The
     * options have to be created
     * by one of the provided methods from this scheduler.
     *
     * The job is only started on this instance - if it is started at all. The
     * options for running on a single instance, on the leader etc. (see
     * {@link ScheduleOptions#onInstancesOnly(String[])},
     * {@link ScheduleOptions#onLeaderOnly(boolean)},
     * and {@link ScheduleOptions#onSingleInstanceOnly(boolean)}) are only useful,
     * if the same job is scheduled on all instances in a cluster. In this case this
     * extra configuration controls on which instances the job is really started.
     * Using the above options might not start the job on the current instance, for
     * example if the current instance is not the leader.
     *
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param options Required options defining how to schedule the job
     * @return true if the job could be added, false otherwise.
     * @see #NOW()
     * @see #NOW(int, long)
     * @see #AT(Date)
     * @see #AT(Date, int, long)
     * @see #EXPR(String)
     * @since 2.3
     */
    boolean schedule(Object job, ScheduleOptions options);

    /**
     * Remove a scheduled job by name.
     *
     * @param jobName The name of the job.
     * @return <code>true</code> if the job existed and could be stopped, <code>false</code> otherwise.
     * @since 2.3
     */
    boolean unschedule(String jobName);

    /**
     * Create a schedule options to fire a job immediately and only once.
     * @return The schedule options.
     * @since 2.3
     */
    ScheduleOptions NOW();

    /**
     * Create a schedule options to fire a job immediately more than once.
     * @param times The number of times this job should be started (must be higher than 1 or
     *              -1 for endless)
     * @param period Every period seconds this job is started (must be at higher than 0).
     * @return The schedule options.
     * @since 2.3
     */
    ScheduleOptions NOW(int times, long period);

    /**
     * Create a schedule options to fire a job once at a specific date
     * @param date The date this job should be run.
     * @return The schedule options.
     * @since 2.3
     */
    ScheduleOptions AT(Date date);

    /**
     * Create a schedule options to fire a job period starting at a specific date
     * @param date The date this job should be run.
     * @param times The number of times this job should be started (must be higher than 1 or
     *              -1 for endless)
     * @param period Every period seconds this job is started (must be at higher than 0).
     * @return The schedule options.
     * @since 2.3
     */
    ScheduleOptions AT(Date date, int times, long period);

    /**
     * Create a schedule options to schedule the job based on the expression
     * @param expression The cron exception
     * @return The schedule options.
     * @since 2.3
     */
    ScheduleOptions EXPR(String expression);

    /**
    /**
     * Schedule a time based job.
     * Note that if a job with the same name has already been added, the old job is cancelled and this new job replaces
     * the old job.
     *
     * @param name The name of the job - or null. If no name is specified it can't be cancelled.
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param schedulingExpression The time specification using a scheduling expression.
     * @param canRunConcurrently Whether this job can run even if previous scheduled runs are still running.
     * @throws IllegalArgumentException If the scheduling expression can't be parsed or if the job has not the correct type.
     * @throws Exception If the job can't be scheduled.
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    void addJob(String name, Object job, Map<String, Serializable> config, String schedulingExpression, boolean canRunConcurrently)
    throws Exception;

    /**
     * Schedule a periodic job.
     * The job is started the first time when the period has passed.
     * Note that if a job with the same name has already been added, the old job is cancelled and this new job replaces
     * the old job.
     *
     * @param name The name of the job - or null. If no name is specified it can't be cancelled.
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param period Every period seconds this job is started.
     * @param canRunConcurrently Whether this job can run even if previous scheduled runs are still running.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @throws Exception If the job can't be scheduled.
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently)
    throws Exception;

    /**
     * Schedule a periodic job.
     * Note that if a job with the same name has already been added, the old job is cancelled and this new job replaces
     * the old job.
     *
     * @param name The name of the job - or null. If no name is specified it can't be cancelled.
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param period Every period seconds this job is started.
     * @param canRunConcurrently Whether this job can run even if previous scheduled runs are still running.
     * @param startImmediate Whether to start the job immediately for the first time or wait for the period to expire.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @throws Exception If the job can't be scheduled.
     * @since 2.2
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently,
            boolean startImmediate)
    throws Exception;

    /**
     * Fire a job immediately and only once.
     *
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @throws Exception If the job can't be scheduled.
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    void fireJob(Object job, Map<String, Serializable> config)
    throws Exception;

    /**
     * Fire a job immediately more than once.
     *
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param times The number of times this job should be started (must be higher than 1)
     * @param period Every period seconds this job is started.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @return true if the code could be added, false otherwise.
     * @since 2.1
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    boolean fireJob(Object job, Map<String, Serializable> config, int times, long period);

    /**
     * Fire a job once at a specific date
     * Note that if a job with the same name has already been added, the old job is cancelled and this new job replaces
     * the old job.
     *
     * @param name The name of the job - or null. If no name is specified it can't be cancelled.
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param date The date this job should be run.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @throws Exception If the job can't be scheduled.
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    void fireJobAt(String name, Object job, Map<String, Serializable> config, Date date)
    throws Exception;

    /**
     * Fire a job once at a specific date, several times with a given interval.
     * Note that if a job with the same name has already been added, the old job is cancelled and this new job replaces
     * the old job.
     *
     * @param name The name of the job - or null. If no name is specified it can't be cancelled.
     * @param job The job to execute (either {@link Job} or {@link Runnable}).
     * @param config An optional configuration object - this configuration is only passed to the job the job implements {@link Job}.
     * @param date The date this job should be run.
     * @param times The number of times this job should be started (must be higher than 1)
     * @param period Every period seconds this job is started.
     * @throws IllegalArgumentException If the job has not the correct type.
     * @return true if the job could be added, false otherwise.
     * @since 2.1
     * @deprecated Use {@link #schedule(Object, ScheduleOptions)} instead.
     */
    @Deprecated
    boolean fireJobAt(String name, Object job, Map<String, Serializable> config, Date date, int times, long period);

    /**
     * Remove a scheduled job by name.
     *
     * @param name The name of the job.
     * @throws NoSuchElementException If the job is not scheduled.
     * @deprecated Use {@link #unschedule(String)} instead.
     */
    @Deprecated
    void removeJob(String name)
    throws NoSuchElementException;
}
