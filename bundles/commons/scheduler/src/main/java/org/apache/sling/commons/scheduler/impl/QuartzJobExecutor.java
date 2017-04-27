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

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    /** Is discovery available? */
    public static final AtomicBoolean DISCOVERY_AVAILABLE = new AtomicBoolean(false);

    /** Is stable discovery information available? */
    public static final AtomicBoolean DISCOVERY_INFO_AVAILABLE = new AtomicBoolean(false);

    /** The id of the current instance (if settings service is available. */
    public static volatile String SLING_ID;

    /** Is this instance the leader? */
    public static final AtomicBoolean IS_LEADER = new AtomicBoolean(true);

    /** The available Sling IDs */
    public static final AtomicReference<String[]> SLING_IDS = new AtomicReference<>(null);

    private boolean checkDiscoveryAvailable(final Logger logger,
            final Object job,
            final String name,
            final String[] runOn) {
        if ( DISCOVERY_AVAILABLE.get() ) {
            if ( DISCOVERY_INFO_AVAILABLE.get() ) {
                return true;
            } else {
                logger.debug("No discovery info available. Excluding job {} with name {} and config {}.",
                        new Object[] {job, name, runOn[0]});
                return false;
            }
        } else {
            logger.debug("No discovery available, therefore not executing job {} with name {} and config {}.",
                    new Object[] {job, name, runOn[0]});
            return false;
        }
    }

    private String checkSlingId(final Logger logger,
            final Object job,
            final String name,
            final String[] runOn) {
        final String myId = SLING_ID;
        if ( myId == null ) {
            logger.error("No Sling ID available, therefore not executing job {} with name {} and config {}.",
                    new Object[] {job, name, Arrays.toString(runOn)});
            return null;
        }
        return myId;
    }

    private boolean shouldRun(final Logger logger,
            final Object job,
            final String name,
            final String[] runOn) {
        if ( runOn != null ) {
            if ( runOn.length == 1 && Scheduler.VALUE_RUN_ON_LEADER.equals(runOn[0]) ) {
                // leader
                if ( !checkDiscoveryAvailable(logger, job, name, runOn) ) {
                    return false;
                }
                if ( !IS_LEADER.get() ) {
                    logger.debug("Excluding job {} with name {} and config {} - instance is not leader",
                            new Object[] {job, name, runOn[0]});
                    return false;
                }
            } else if ( runOn.length == 1 && Scheduler.VALUE_RUN_ON_SINGLE.equals(runOn[0]) ) {
                // single instance
                if ( !checkDiscoveryAvailable(logger, job, name, runOn) ) {
                    return false;
                }
                final String myId = checkSlingId(logger, job, name, runOn);
                if ( myId == null ) {
                    return false;
                }
                final String[] ids = QuartzJobExecutor.SLING_IDS.get();
                boolean schedule = false;
                if ( ids != null ) {
                    int index = 0;
                    try {
                        final MessageDigest m = MessageDigest.getInstance("MD5");
                        m.reset();
                        m.update(job.getClass().getName().getBytes("UTF-8"));
                        index = new BigInteger(1, m.digest()).mod(BigInteger.valueOf(ids.length)).intValue();
                    } catch ( final IOException | NoSuchAlgorithmException ex ) {
                        // although this should never happen (MD5 and UTF-8 are always available) we consider
                        // this an error case
                        logger.error("Unable to distribute scheduled job " + job + " with name " + name, ex);
                        return false;
                    }
                    schedule = myId.equals(ids[index]);
                }
                if ( !schedule ) {
                    logger.debug("Excluding job {} with name {} and config {} - distributed to different Sling instance",
                            new Object[] {job, name, runOn});
                    return false;
                }
            } else { // sling IDs
                final String myId = checkSlingId(logger, job, name, runOn);
                if ( myId == null ) {
                    return false;
                } else {
                    boolean schedule = false;
                    for(final String id : runOn ) {
                        if ( myId.equals(id) ) {
                            schedule = true;
                            break;
                        }
                    }
                    if ( !schedule ) {
                        logger.debug("Excluding job {} with name {} and config {} - different Sling ID",
                                new Object[] {job, name, Arrays.toString(runOn)});
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {

        final JobDataMap data = context.getJobDetail().getJobDataMap();
        final Object job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
        final Logger logger = (Logger)data.get(QuartzScheduler.DATA_MAP_LOGGER);

        // check run on information
        final String name = (String) data.get(QuartzScheduler.DATA_MAP_NAME);
        if ( !shouldRun(logger, job, name, (String[])data.get(QuartzScheduler.DATA_MAP_RUN_ON)) ) {
            return;
        }

        String origThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(origThreadName + "-" + name);

            logger.debug("Executing job {} with name {}", job, data.get(QuartzScheduler.DATA_MAP_NAME));
            if (job instanceof org.apache.sling.commons.scheduler.Job) {
                @SuppressWarnings("unchecked")
                final Map<String, Serializable> configuration = (Map<String, Serializable>) data.get(QuartzScheduler.DATA_MAP_CONFIGURATION);

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
            Thread.currentThread().setName(origThreadName);
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
        @Override
        public Map<String, Serializable> getConfiguration() {
            return this.configuration;
        }

        /**
         * @see org.apache.sling.commons.scheduler.JobContext#getName()
         */
        @Override
        public String getName() {
            return this.name;
        }
    }
}
