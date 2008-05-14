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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quartz based implementation of the scheduler.
 *
 * @scr.component
 * @scr.service interface="org.apache.sling.commons.scheduler.Scheduler"
 * @scr.reference name="job" interface="org.apache.sling.commons.scheduler.Job" cardinality="0..n" policy="dynamic"
 * @scr.reference name="task" interface="java.lang.Runnable" cardinality="0..n" policy="dynamic"
 */
public class QuartzScheduler implements Scheduler {

    /** Default log. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String DEFAULT_QUARTZ_JOB_GROUP = "Sling";

    protected static final String THREAD_POOL_NAME = "SLING_SCHEDULER";

    /** Map key for the job object */
    static final String DATA_MAP_OBJECT = "QuartzJobScheduler.Object";

    /** Map key for the job name */
    static final String DATA_MAP_NAME = "QuartzJobScheduler.JobName";

    /** Map key for the concurrent run property */
    static final String DATA_MAP_RUN_CONCURRENT = "QuartzJobScheduler.RunConcurrently";

    /** Map key for the run status */
    static final String DATA_MAP_KEY_ISRUNNING = "QuartzJobExecutor.isRunning";

    /** Map key for the configuration. */
    static final String DATA_MAP_CONFIGURATION = "QuartzJobScheduler.Configuration";

    /** Map key for the logger. */
    static final String DATA_MAP_LOGGER = "QuartzJobScheduler.Logger";

    protected org.quartz.Scheduler scheduler;

    protected final List<Object[]> registeredJobs = new ArrayList<Object[]>();

    protected ComponentContext context;

    /** @scr.reference */
    protected ThreadPoolManager threadPoolManager;

    /**
     * Activate this component.
     * Start the scheduler.
     * @param ctx The component context.
     * @throws Exception
     */
    protected void activate(ComponentContext ctx) throws Exception {
        this.context = ctx;
        synchronized ( this.registeredJobs ) {
            this.init();
            for( Object[] arr : this.registeredJobs ) {
                try {
                    this.register((String)arr[0], (ServiceReference)arr[1]);
                } catch (Exception e) {
                    // we don't want that one malicious service brings down the scheduler, so we just log
                    // the exception and continue
                    this.logger.error("Exception during registering job service {}.", arr[1], e);
                }
            }
            this.registeredJobs.clear();
        }
    }

    /**
     * Deactivate this component.
     * Stop the scheduler.
     * @param ctx The component context.
     */
    protected void deactivate(ComponentContext ctx) {
        synchronized (this.registeredJobs ) {
            this.dispose();
        }
        this.context = null;
    }

    protected void init() throws SchedulerException {
        // if we don't have a thread pool manager, we use the default thread pool
        final ThreadPoolManager tpm = this.threadPoolManager;
        if ( tpm == null ) {
            final SchedulerFactory factory = new StdSchedulerFactory();
            this.scheduler = factory.getScheduler();
        } else {
            final ThreadPool pool = tpm.get(THREAD_POOL_NAME);
            final QuartzThreadPool quartzPool = new QuartzThreadPool(pool);
            final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
            factory.createScheduler(quartzPool, new RAMJobStore());
            this.scheduler = factory.getScheduler();
        }
        this.scheduler.start();
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Scheduler started.");
        }
    }

    protected void dispose() {
        if ( this.scheduler != null ) {
            try {
                this.scheduler.shutdown();
            } catch (SchedulerException e) {
                this.logger.debug("Exception during shutdown of scheduler.", e);
            }
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("Scheduler stopped.");
            }
            this.scheduler = null;
        }
    }

    /**
     * Add a job to the scheduler
     *
     * @param name The name of the job to add (or null)
     * @param Tje jopb
     * @param trigger a Trigger
     * @param canRunConcurrently whether this job can be run concurrently
     *
     * @throws Exception thrown in case of errors
     */
    protected void scheduleJob(String name,
                               final Object job,
                               final Map<String, Serializable>    config,
                               final Trigger trigger,
                               final boolean canRunConcurrently)
    throws Exception {
        // check if the supplied object is valid
        this.checkJob(job);

        // if there is already a job with the name, remove it first
        if ( name != null ) {
            try {
                final JobDetail jobdetail = this.scheduler.getJobDetail(name, DEFAULT_QUARTZ_JOB_GROUP);
                if (jobdetail != null) {
                    this.removeJob(name);
                }
            } catch (final SchedulerException ignored) {
            }
        } else {
            name = "Sling Quartz Scheduler " + UUID.randomUUID().toString();
        }

        // create the data map
        final JobDataMap jobDataMap = this.initDataMap(name, job, config, canRunConcurrently);

        final JobDetail detail = this.createJobDetail(name, jobDataMap);

        this.logger.debug("Scheduling job {} with name {} and trigger {}", new Object[] {job, name, trigger});
        this.scheduler.scheduleJob(detail, trigger);
    }

    /**
     * Initialize the data map for the job executor.
     * @param jobName
     * @param job
     * @param config
     * @param concurent
     * @return
     */
    protected JobDataMap initDataMap(String  jobName,
                                     Object  job,
                                     Map<String, Serializable> config,
                                     boolean concurent) {
        final JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(DATA_MAP_OBJECT, job);

        jobDataMap.put(DATA_MAP_NAME, jobName);
        jobDataMap.put(DATA_MAP_RUN_CONCURRENT, (concurent? Boolean.TRUE: Boolean.FALSE));
        jobDataMap.put(DATA_MAP_LOGGER, this.logger);
        if ( config != null ) {
            jobDataMap.put(DATA_MAP_CONFIGURATION, config);
        }

        return jobDataMap;
    }

    /**
     * Create the job detail.
     * @param name
     * @param jobDataMap
     * @return
     */
    protected JobDetail createJobDetail(String name, JobDataMap jobDataMap) {
        final JobDetail detail = new JobDetail(name, DEFAULT_QUARTZ_JOB_GROUP, QuartzJobExecutor.class);
        detail.setJobDataMap(jobDataMap);
        return detail;
    }

    /**
     * Check the job object, either runnable or job is allowed
     */
    protected void checkJob(Object job)
    throws Exception {
        if (!(job instanceof Runnable) && !(job instanceof Job)) {
            throw new Exception("Job object is neither an instance of " + Runnable.class.getName() + " nor " + Job.class.getName());
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addJob(java.lang.String, java.lang.Object, java.util.Map, java.lang.String, boolean)
     */
    public void addJob(String name,
                       Object job,
                       Map<String, Serializable>    config,
                       String schedulingExpression,
                       boolean canRunConcurrently)
    throws Exception {
        final CronTrigger cronJobEntry = new CronTrigger(name, DEFAULT_QUARTZ_JOB_GROUP);

        try {
            cronJobEntry.setCronExpression(schedulingExpression);
        } catch (final ParseException pe) {
            throw new Exception(pe.getMessage(), pe);
        }
        this.scheduleJob(name, job, config, cronJobEntry, canRunConcurrently);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean)
     */
    public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently)
    throws Exception {
        final long ms = period * 1000;
        if ( name == null ) {
            name = "Sling Quartz Scheduler " + UUID.randomUUID().toString();
        }
        final SimpleTrigger timeEntry =
            new SimpleTrigger(name, DEFAULT_QUARTZ_JOB_GROUP, new Date(System.currentTimeMillis() + ms), null,
                              SimpleTrigger.REPEAT_INDEFINITELY, ms);

        this.scheduleJob(name, job, config, timeEntry, canRunConcurrently);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map)
     */
    public void fireJob(Object job, Map<String, Serializable> config)
    throws Exception {
        this.checkJob(job);
        final String name = job.getClass().getName();
        final JobDataMap dataMap = this.initDataMap(name, job, config, true);

        final JobDetail detail = this.createJobDetail(name, dataMap);

        final Trigger trigger = new SimpleTrigger(name, DEFAULT_QUARTZ_JOB_GROUP);
        this.scheduler.scheduleJob(detail, trigger);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date)
     */
    public void fireJobAt(String name, Object job, Map<String, Serializable> config, Date date) throws Exception {
        if ( name == null ) {
            name = "Sling Quartz Scheduler " + UUID.randomUUID().toString();
        }
        final SimpleTrigger trigger = new SimpleTrigger(name, DEFAULT_QUARTZ_JOB_GROUP, date);
        this.scheduleJob(name, job, config, trigger, true);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#removeJob(java.lang.String)
     */
    public void removeJob(String name) throws NoSuchElementException {
        try {
            this.scheduler.deleteJob(name, DEFAULT_QUARTZ_JOB_GROUP);
        } catch (final SchedulerException se) {
            throw new NoSuchElementException(se.getMessage());
        }
    }

    protected void register(String type, ServiceReference ref)
    throws Exception {
        final Object job = this.context.locateService(type, ref);
        if ( ref != null ) {
            this.checkJob(job);
            String name = (String)ref.getProperty("scheduler.name");
            if ( name == null ) {
                name = (String)ref.getProperty(Constants.SERVICE_PID);
            }
            if ( name != null ) {
                final Boolean concurrent = (Boolean)ref.getProperty("scheduler.concurrent");
                final String expression = (String)ref.getProperty("scheduler.expression");
                if ( expression != null ) {
                    this.addJob(name, job, null, expression, (concurrent != null ? concurrent : true));
                } else {
                    final Long period = (Long)ref.getProperty("scheduler.period");
                    if ( period != null ) {
                        this.addPeriodicJob(name, job, null, period, (concurrent != null ? concurrent : true));
                    }
                }
            } else {
                throw new Exception("Job service must either have a PID or a configured property 'scheduler.name'.");
            }
        }
    }

    protected void unregister(ServiceReference ref) {
        String name = (String)ref.getProperty("scheduler.name");
        if ( name == null ) {
            name = (String)ref.getProperty(Constants.SERVICE_PID);
        }
        if ( name != null ) {
            this.removeJob(name);
        }
    }

    /**
     * Bind a new job.
     * @param ref
     * @throws Exception
     */
    protected void bindJob(ServiceReference ref)
    throws Exception {
        synchronized ( this.registeredJobs ) {
            if ( this.scheduler != null ) {
                this.register("job", ref);
            } else {
                this.registeredJobs.add(new Object[] {"job", ref});
            }
        }
    }

    /**
     * Unbind a job.
     * @param ref
     */
    protected void unbindJob(ServiceReference ref) {
        synchronized ( this.registeredJobs ) {
            if ( this.scheduler != null ) {
                this.unregister(ref);
            }
        }
    }

    /**
     * Bind a new job.
     * @param ref
     * @throws Exception
     */
    protected void bindTask(ServiceReference ref)
    throws Exception {
        synchronized ( this.registeredJobs ) {
            if ( this.scheduler != null ) {
                this.register("task", ref);
            } else {
                this.registeredJobs.add(new Object[] {"task", ref});
            }
        }
    }

    /**
     * Unbind a job.
     * @param ref
     */
    protected void unbindTask(ServiceReference ref) {
        synchronized ( this.registeredJobs ) {
            if ( this.scheduler != null ) {
                this.unregister(ref);
            }
        }
    }

    private static final class QuartzThreadPool implements org.quartz.spi.ThreadPool {

        /** Default log. */
        protected final Logger logger = LoggerFactory.getLogger(this.getClass());

        /** Our executor thread pool */
        private ThreadPool executor;

        /**
         * Create a new wrapper implementation for Quartz.
         */
        public QuartzThreadPool(final ThreadPool executor) {
            this.executor = executor;
        }

        /* (non-Javadoc)
         * @see org.quartz.spi.QuartzThreadPool#getPoolSize()
         */
        public int getPoolSize() {
            return this.executor.getConfiguration().getMaxPoolSize();
        }

        /* (non-Javadoc)
         * @see org.quartz.spi.QuartzThreadPool#initialize()
         */
        public void initialize() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.quartz.spi.QuartzThreadPool#runInThread(java.lang.Runnable)
         */
        public boolean runInThread(final Runnable job) {
            this.executor.execute(job);

            return true;
        }

        /**
         * @see org.quartz.spi.ThreadPool#blockForAvailableThreads()
         */
        public int blockForAvailableThreads() {
            return this.executor.getConfiguration().getMaxPoolSize() - this.executor.getConfiguration().getQueueSize();
        }

        /* (non-Javadoc)
         * @see org.quartz.spi.QuartzThreadPool#shutdown(boolean)
         */
        public void shutdown(final boolean waitForJobsToComplete) {
            // the pool is managed by the thread pool manager,
            // so we can just return
            this.executor = null;
        }
    }

}
