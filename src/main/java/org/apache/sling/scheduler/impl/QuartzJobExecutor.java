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
package org.apache.sling.scheduler.impl;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This component is resposible to launch a {@link org.apache.sling.scheduler.Job}
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

        try {
            final Object job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
            final Map<Object, Object> configuration = (Map<Object, Object>) data.get(QuartzScheduler.DATA_MAP_CONFIGURATION);
            final String name = (String) data.get(QuartzScheduler.DATA_MAP_NAME);
            if (job instanceof org.apache.sling.scheduler.Job) {
                ((org.apache.sling.scheduler.Job) job).execute(name, configuration, null);
            } else if (job instanceof Runnable) {
                ((Runnable) job).run();
            }
        } catch (final Throwable t) {

            if (t instanceof JobExecutionException) {
                throw (JobExecutionException) t;
            }
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

}
