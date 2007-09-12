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
package org.apache.sling.scheduler;

import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A scheduler to schedule time/cron based jobs.
 * The fired jobs can either implement the Runnable interface or the Quartz job interface.
 */
public interface Scheduler {

    /**
     * Schedule a time based job.  Note that if a job with the same name has already beed added it is overwritten.
     *
     * @param name the name of the job
     * @param job The job object itself. It must implement either Runnable or might also be an implementation
     *        specific class (i.e. org.quartz.Job)
     * @param schedulingExpression the time specification using a scheduling expression
     * @param canRunConcurrently whether this job can run even previous scheduled runs are still running
     */
    void addJob(String name, Object job, Map<Object, Object> config, String schedulingExpression, boolean canRunConcurrently)
    throws Exception;

    /**
     * Schedule a periodic job. The job is started the first time when the period has passed.  Note that if a job with
     * the same name has already beed added it is overwritten.
     *
     * @param name the name of the job
     * @param job The job object itself. It must implement either Runnable or might also be an implementation
     *        specific class (i.e. org.quartz.Job)
     * @param period Every period seconds this job is started
     * @param canRunConcurrently whether this job can run even previous scheduled runs are still running
     */
    void addPeriodicJob(String name, Object job, Map<Object, Object> config, long period, boolean canRunConcurrently)
    throws Exception;

    /**
     * Fire a job once immediately
     *
     * @param job The job object itself. It must implement either Runnable or might also be an implementation
     *        specific class (i.e. org.quartz.Job)
     *
     */
    void fireJob(Object job, Map<Object, Object> config)
    throws Exception;

    /**
     * Fire a job once at a specific date Note that if a job with the same name has already beed added it is
     * overwritten.
     *
     * @param date The date this job should be scheduled
     * @param name the name of the job
     * @param job The job object itself. It must implement either Runnable or might also be an implementation
     *        specific class (i.e. org.quartz.Job)
     */
    void fireJobAt(String name, Object job, Map<Object, Object> config, Date date)
    throws Exception;

    /**
     * Remove a scheduled job by name.
     *
     * @param name the name of the job
     */
    void removeJob(String name)
    throws NoSuchElementException;
}
