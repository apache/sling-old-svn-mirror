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
import org.slf4j.LoggerFactory;

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

    /** Force leader for jobs with run on single */
    public static final AtomicBoolean FORCE_LEADER = new AtomicBoolean(true);

    /** Is this instance the leader? */
    public static final AtomicBoolean IS_LEADER = new AtomicBoolean(true);

    /** The available Sling IDs */
    public static final AtomicReference<String[]> SLING_IDS = new AtomicReference<>(null);

    public static class JobDesc {

        public final Object job;
        public final String providedName;
        public final String name;
        public final String[] runOn;

        public JobDesc(final JobDataMap data) {
            this.job = data.get(QuartzScheduler.DATA_MAP_OBJECT);
            this.name = (String) data.get(QuartzScheduler.DATA_MAP_NAME);
            this.providedName = (String)data.get(QuartzScheduler.DATA_MAP_PROVIDED_NAME);
            this.runOn = (String[])data.get(QuartzScheduler.DATA_MAP_RUN_ON);
        }

        public boolean isKnownJob() {
            return this.job != null && this.name != null;
        }

        public String getKey() {
            String key = job.getClass().getName();
            if ( providedName != null ) {
                key = key + "-" + providedName;
            }
            return key;
        }

        @Override
        public String toString() {
            final String runOnInfo;
            if ( this.runOn == null ) {
                runOnInfo = null;
            } else if ( isRunOnLeader() ) {
                runOnInfo = Scheduler.VALUE_RUN_ON_LEADER;
            } else if ( isRunOnSingle() ) {
                runOnInfo = Scheduler.VALUE_RUN_ON_SINGLE;
            } else {
                runOnInfo = Arrays.toString(runOn);
            }
            return "job '" + job + "' with name '" + name + "'" + (runOnInfo == null ? "" : " and config " + runOnInfo);
        }

        public boolean isRunOnLeader() {
           return runOn != null && runOn.length == 1 && Scheduler.VALUE_RUN_ON_LEADER.equals(runOn[0]);
        }

        public boolean isRunOnSingle() {
            return runOn != null && runOn.length == 1 && Scheduler.VALUE_RUN_ON_SINGLE.equals(runOn[0]);
        }

        public String shouldRunAsSingleOn() {
            if ( !isRunOnSingle() ) {
                return null;
            }
            final String[] ids = QuartzJobExecutor.SLING_IDS.get();
            boolean schedule = false;
            if ( ids != null ) {
                int index = 0;
                try {
                    final MessageDigest m = MessageDigest.getInstance("MD5");
                    m.reset();
                    m.update(getKey().getBytes("UTF-8"));
                    index = new BigInteger(1, m.digest()).mod(BigInteger.valueOf(ids.length)).intValue();
                } catch ( final IOException | NoSuchAlgorithmException ex ) {
                    // although this should never happen (MD5 and UTF-8 are always available) we consider
                    // this an error case
                    LoggerFactory.getLogger(getClass().getName()).error("Unable to distribute scheduled " + this, ex);
                    return "";
                }
                final String myId = SLING_ID;
                schedule = myId != null && myId.equals(ids[index]);
                return schedule ? null : ids[index];
            }
            return "";
        }
    }

    private boolean checkDiscoveryAvailable(final Logger logger,
            final JobDesc desc) {
        if ( DISCOVERY_AVAILABLE.get() ) {
            if ( DISCOVERY_INFO_AVAILABLE.get() ) {
                return true;
            } else {
                logger.debug("No discovery info available. Excluding {}.", desc);
                return false;
            }
        } else {
            logger.debug("No discovery available, therefore not executing {}.", desc);
            return false;
        }
    }

    private String checkSlingId(final Logger logger,
            final JobDesc desc) {
        final String myId = SLING_ID;
        if ( myId == null ) {
            logger.error("No Sling ID available, therefore not executing {}.", desc);
            return null;
        }
        return myId;
    }

    private boolean shouldRun(final Logger logger,
            final JobDesc desc) {
        if ( desc.runOn != null ) {
            if ( desc.isRunOnLeader() ) {
                // leader
                if ( !checkDiscoveryAvailable(logger, desc) ) {
                    return false;
                }
                if ( !IS_LEADER.get() ) {
                    logger.debug("Excluding {} - instance is not leader", desc);
                    return false;
                }
            } else if ( desc.isRunOnSingle() ) {
                // single instance
                if ( !checkDiscoveryAvailable(logger, desc) ) {
                    return false;
                }
                if ( FORCE_LEADER.get() ) {
                    if ( !IS_LEADER.get() ) {
                        logger.debug("Excluding {} - instance is not leader", desc);
                        return false;
                    }
                } else {
                    final String myId = checkSlingId(logger, desc);
                    if ( myId == null ) {
                        return false;
                    }
                    if ( desc.shouldRunAsSingleOn() != null ) {
                        logger.debug("Excluding {} - distributed to different Sling instance", desc);
                        return false;
                    }
                }
            } else { // sling IDs
                final String myId = checkSlingId(logger, desc);
                if ( myId == null ) {
                    return false;
                } else {
                    boolean schedule = false;
                    for(final String id : desc.runOn ) {
                        if ( myId.equals(id) ) {
                            schedule = true;
                            break;
                        }
                    }
                    if ( !schedule ) {
                        logger.debug("Excluding job {} - different Sling ID", desc);
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
        final JobDesc desc = new JobDesc(data);
        final Logger logger = (Logger)data.get(QuartzScheduler.DATA_MAP_LOGGER);

        // check run on information
        if ( !shouldRun(logger, desc) ) {
            return;
        }

        String origThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(origThreadName + "-" + desc.name);

            logger.debug("Executing job {}", desc);
            if (desc.job instanceof org.apache.sling.commons.scheduler.Job) {
                @SuppressWarnings("unchecked")
                final Map<String, Serializable> configuration = (Map<String, Serializable>) data.get(QuartzScheduler.DATA_MAP_CONFIGURATION);

                final JobContext jobCtx = new JobContextImpl(desc.name, configuration);
                ((org.apache.sling.commons.scheduler.Job) desc.job).execute(jobCtx);
            } else if (desc.job instanceof Runnable) {
                ((Runnable) desc.job).run();
            } else {
                logger.error("Scheduled job {} is neither a job nor a runnable: {}", desc);
            }
        } catch (final Throwable t) {
            // if this is a quartz exception, rethrow it
            if (t instanceof JobExecutionException) {
                throw (JobExecutionException) t;
            }
            // there is nothing we can do here, so we just log
            logger.error("Exception during job execution of " + desc + " : " + t.getMessage(), t);
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
