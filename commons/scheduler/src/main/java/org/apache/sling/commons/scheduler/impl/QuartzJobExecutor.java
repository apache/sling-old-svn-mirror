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
import java.util.Map;

import org.apache.sling.commons.scheduler.JobContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * This component is resposible to launch a {@link org.apache.sling.commons.scheduler.Job}
 * or {@link Runnable} in a Quartz Scheduler.
 *
 */
public class QuartzJobExecutor implements Job {

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(final JobExecutionContext context) throws JobExecutionException {

        final JobDataMap data = context.getJobDetail().getJobDataMap();

        final Boolean canRunConcurrentlyB = ((Boolean) data.get(QuartzScheduler.DATA_MAP_RUN_CONCURRENT));
        final boolean canRunConcurrently = ((canRunConcurrentlyB == null) ? true : canRunConcurrentlyB.booleanValue());

        if (!canRunConcurrently) {
            Boolean isRunning = (Boolean) data.get(QuartzScheduler.DATA_MAP_KEY_ISRUNNING);
            if (Boolean.TRUE.equals(isRunning)) {
                return;
            }
        }

        this.setup(data);

        final Object job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
        final Logger logger = (Logger)data.get(QuartzScheduler.DATA_MAP_LOGGER);

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
        } finally {

            this.release(data);
        }
    }

    protected void setup(JobDataMap data) throws JobExecutionException {
        data.put(QuartzScheduler.DATA_MAP_KEY_ISRUNNING, Boolean.TRUE);
    }

    protected void release(JobDataMap data) {
        data.put(QuartzScheduler.DATA_MAP_KEY_ISRUNNING, Boolean.FALSE);
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
