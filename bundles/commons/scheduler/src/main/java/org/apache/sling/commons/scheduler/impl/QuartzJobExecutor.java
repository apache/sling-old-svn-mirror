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
package org.apache.sling.commons.scheduler.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * This component is responsible to launch a {@link org.apache.sling.commons.scheduler.Job}
 * or {@link Runnable} in a Quartz Scheduler.
 *
 */
public class QuartzJobExecutor implements Job {

    /** Is discovery information available? */
    public static final AtomicBoolean DISCOVERY_INFO_AVAILABLE = new AtomicBoolean(false);

    /** The id of the current instance. */
    public static String SLING_ID;

    /** Is this instance the leader? */
    public static final AtomicBoolean IS_LEADER = new AtomicBoolean(true);

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(final JobExecutionContext context) throws JobExecutionException {

        final JobDataMap data = context.getJobDetail().getJobDataMap();
        final Object job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
        final Logger logger = (Logger)data.get(QuartzScheduler.DATA_MAP_LOGGER);

        // check run on information
        final String[] runOn = (String[])data.get(QuartzScheduler.DATA_MAP_RUN_ON);
        if ( runOn != null ) {
            if ( runOn.length == 1 && Scheduler.VALUE_RUN_ON_LEADER.equals(runOn[0])
                 || runOn.length == 1 && Scheduler.VALUE_RUN_ON_SINGLE.equals(runOn[0]) ) {
                if ( DISCOVERY_INFO_AVAILABLE.get() ) {
                    if ( !IS_LEADER.get() ) {
                        logger.debug("Excluding job {} with name {} and config {}.",
                                new Object[] {job, data.get(QuartzScheduler.DATA_MAP_NAME), runOn[0]});
                        return;
                    }
                } else {
                    logger.warn("No discovery info available. Executing job {} with name {} and config {} anyway.",
                            new Object[] {job, data.get(QuartzScheduler.DATA_MAP_NAME), runOn[0]});
                }
            } else { // sling IDs
                final String myId = SLING_ID;
                boolean schedule = false;
                if ( myId == null ) {
                    logger.warn("No Sling ID available. Executing job {} with name {} and config {} anyway.",
                            new Object[] {job, data.get(QuartzScheduler.DATA_MAP_NAME), Arrays.toString(runOn)});
                    schedule = true;
                } else {
                    for(final String id : runOn ) {
                        if ( myId.equals(id) ) {
                            schedule = true;
                            break;
                        }
                    }
                }
                if ( !schedule ) {
                    logger.debug("Excluding job {} with name {} and config {}.",
                            new Object[] {job, data.get(QuartzScheduler.DATA_MAP_NAME), Arrays.toString(runOn)});
                    return;
                }
            }
        }

        try {
            logger.debug("Executing job {} with name {}", job, data.get(QuartzScheduler.DATA_MAP_NAME));
            if (job instanceof org.apache.sling.commons.scheduler.Job) {
                @SuppressWarnings("unchecked")
                final Map<String, Serializable> configuration = (Map<String, Serializable>) data.get(QuartzScheduler.DATA_MAP_CONFIGURATION);
                final String name = (String) data.get(QuartzScheduler.DATA_MAP_NAME);

                final JobContext jobCtx = new JobContextImpl(name, configuration);
                ((org.apache.sling.commons.scheduler.Job) job).execute(jobCtx);
            } else if (job instanceof Runnable) {
                ((Runnable) job).run();
            } else {
                logger.error("Scheduled job {} is neither a job nor a runnable.", job);
            }
        } catch (final Throwable t) {
            // if this is a quartz exception, rethrow it
            if (t instanceof JobExecutionException) {
                throw (JobExecutionException) t;
            }
            // there is nothing we can do here, so we just log
            logger.error("Exception during job execution of " + job + " : " + t.getMessage(), t);
        }
    }

    public static final class JobContextImpl implements JobContext {

        protected final Map<String, Serializable> configuration;
        protected final String name;

        public JobContextImpl(String name, Map<String, Serializable> config) {
            this.name = name;
            this.configuration = config;
        }

        /**
         * @see org.apache.sling.commons.scheduler.JobContext#getConfiguration()
         */
        public Map<String, Serializable> getConfiguration() {
            return this.configuration;
        }

        /**
         * @see org.apache.sling.commons.scheduler.JobContext#getName()
         */
        public String getName() {
            return this.name;
        }
    }
}
