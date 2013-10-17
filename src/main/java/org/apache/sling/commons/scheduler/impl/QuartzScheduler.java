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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quartz based implementation of the scheduler.
 *
 */
@Component(metatype=true,label="%scheduler.name",description="%scheduler.description")
@Service(value=Scheduler.class)
public class QuartzScheduler implements Scheduler {

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PREFIX = "Apache Sling Quartz Scheduler ";

    private static final String QUARTZ_SCHEDULER_NAME = "ApacheSling";

    /** Map key for the job object */
    static final String DATA_MAP_OBJECT = "QuartzJobScheduler.Object";

    /** Map key for the job name */
    static final String DATA_MAP_NAME = "QuartzJobScheduler.JobName";

    /** Map key for the configuration. */
    static final String DATA_MAP_CONFIGURATION = "QuartzJobScheduler.Configuration";

    /** Map key for the logger. */
    static final String DATA_MAP_LOGGER = "QuartzJobScheduler.Logger";

    /** Map key for the runOn information (String[]). */
    static final String DATA_MAP_RUN_ON = "QuartzJobScheduler.runOn";

    /** The quartz scheduler. */
    private volatile org.quartz.Scheduler scheduler;

    @Reference
    private ThreadPoolManager threadPoolManager;

    private ThreadPool threadPool;

    /** Service registration for the plugin. */
    private ServiceRegistration plugin;

    @Property
    private static final String PROPERTY_POOL_NAME = "poolName";

    /**
     * Activate this component.
     * Start the scheduler.
     * @throws Exception
     */
    @Activate
    protected void activate(final BundleContext ctx, final Map<String, Object> props) throws Exception {
        final Object poolNameObj = props.get(PROPERTY_POOL_NAME);
        final String poolName;
        if ( poolNameObj != null && poolNameObj.toString().trim().length() > 0 ) {
            poolName = poolNameObj.toString().trim();
        } else {
            poolName = null;
        }

        // start scheduler
        this.scheduler = this.init(poolName);
        this.plugin = WebConsolePrinter.initPlugin(ctx, this);
    }

    /**
     * Deactivate this component.
     * Stop the scheduler.
     */
    @Deactivate
    protected void deactivate() {
        WebConsolePrinter.destroyPlugin(this.plugin);
        this.plugin = null;
        final org.quartz.Scheduler s = this.scheduler;
        this.scheduler = null;
        this.dispose(s);
    }

    /**
     * Initialize the quartz scheduler
     * @return Return the new scheduler instance.
     * @throws SchedulerException
     */
    private org.quartz.Scheduler init(final String poolName) throws SchedulerException {

        // SLING-2261 Prevent Quartz from checking for updates
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", Boolean.TRUE.toString());

        final ThreadPoolManager tpm = this.threadPoolManager;
        // sanity null check
        if ( tpm == null ) {
            throw new SchedulerException("Thread pool manager missing");
        }

        // create the pool
        this.threadPool = tpm.get(poolName);
        final QuartzThreadPool quartzPool = new QuartzThreadPool(this.threadPool);

        final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
        // unique run id
        final String runID = new Date().toString().replace(' ', '_');
        factory.createScheduler(QUARTZ_SCHEDULER_NAME, runID, quartzPool, new RAMJobStore());
        // quartz does not provide a way to get the scheduler by name AND runID, so we have to iterate!
        final Iterator<org.quartz.Scheduler> allSchedulersIter = factory.getAllSchedulers().iterator();
        org.quartz.Scheduler s = null;
        while ( s == null && allSchedulersIter.hasNext() ) {
            final org.quartz.Scheduler current = allSchedulersIter.next();
            if ( QUARTZ_SCHEDULER_NAME.equals(current.getSchedulerName())
                 && runID.equals(current.getSchedulerInstanceId()) ) {
                s = current;
            }
        }
        if ( s == null ) {
            throw new SchedulerException("Unable to find new scheduler with name " + QUARTZ_SCHEDULER_NAME + " and run ID " + runID);
        }

        s.start();
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug(PREFIX + "started.");
        }
        return s;
    }

    /**
     * Dispose the quartz scheduler
     * @param s The scheduler.
     */
    private void dispose(final org.quartz.Scheduler s) {
        if ( s != null ) {
            try {
                s.shutdown();
            } catch (SchedulerException e) {
                this.logger.debug("Exception during shutdown of scheduler.", e);
            }
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug(PREFIX + "stopped.");
            }
        }
        final ThreadPoolManager tpm = this.threadPoolManager;
        if ( tpm != null && this.threadPool != null ) {
            tpm.release(this.threadPool);
        }
        this.threadPool = null;
    }


    /**
     * Initialize the data map for the job executor.
     * @param jobName
     * @param job
     * @param config
     * @param concurent
     * @return
     */
    private JobDataMap initDataMap(final String  jobName,
                                   final Object  job,
                                   final InternalScheduleOptions options) {
        final JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(DATA_MAP_OBJECT, job);

        jobDataMap.put(DATA_MAP_NAME, jobName);
        jobDataMap.put(DATA_MAP_LOGGER, this.logger);
        if ( options.configuration != null ) {
            jobDataMap.put(DATA_MAP_CONFIGURATION, options.configuration);
        }
        if ( options.runOn != null) {
            jobDataMap.put(DATA_MAP_RUN_ON, options.runOn);
        }

        return jobDataMap;
    }

    /**
     * Create the job detail.
     * @param name
     * @param jobDataMap
     * @return
     */
    private JobDetail createJobDetail(final String name,
                                        final JobDataMap jobDataMap,
                                        final boolean concurrent) {
        final JobDetail detail = JobBuilder.newJob((concurrent ? QuartzJobExecutor.class : NonParallelQuartzJobExecutor.class))
                .withIdentity(name)
                .usingJobData(jobDataMap)
                .build();
        return detail;
    }

    /**
     * Check the job object, either runnable or job is allowed
     */
    private void checkJob(final Object job)
    throws IllegalArgumentException {
        if (!(job instanceof Runnable) && !(job instanceof Job)) {
            throw new IllegalArgumentException("Job object is neither an instance of " + Runnable.class.getName() + " nor " + Job.class.getName());
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addJob(java.lang.String, java.lang.Object, java.util.Map, java.lang.String, boolean)
     */
    public void addJob(final String name,
                       final Object job,
                       final Map<String, Serializable>    config,
                       final String schedulingExpression,
                       final boolean canRunConcurrently)
    throws SchedulerException {
        this.scheduleJob(job,
                EXPR(schedulingExpression).name(name).config(config).canRunConcurrently(canRunConcurrently));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean)
     */
    public void addPeriodicJob(final String name,
                               final Object job,
                               final Map<String, Serializable> config,
                               final long period,
                               final boolean canRunConcurrently)
    throws SchedulerException {
        this.addPeriodicJob(name, job, config, period, canRunConcurrently, false);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean, boolean)
     */
    public void addPeriodicJob(final String name,
            final Object job,
            final Map<String, Serializable> config,
            final long period,
            final boolean canRunConcurrently,
            final boolean startImmediate)
    throws SchedulerException {
        this.scheduleJob(job,
                PERIODIC(period, startImmediate).name(name).config(config).canRunConcurrently(canRunConcurrently));
    }

    private ScheduleOptions PERIODIC(final long period, final boolean startImmediate) {
        if ( period < 1 ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Period argument must be higher than 0"));
        }
        final long ms = period * 1000;

        final TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
                .startAt(new Date(System.currentTimeMillis() + ms))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().repeatForever().withIntervalInMilliseconds(ms));
        if ( startImmediate ) {
            return new InternalScheduleOptions( builder.startNow());
        } else {
            return new InternalScheduleOptions( builder.startAt(new Date(System.currentTimeMillis() + ms)) );
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map)
     */
    public void fireJob(final Object job, final Map<String, Serializable> config)
    throws SchedulerException {
        this.scheduleJob(job,
                NOW().config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date)
     */
    public void fireJobAt(final String name, final Object job, final Map<String, Serializable> config, final Date date)
    throws SchedulerException {
        this.scheduleJob(job,
                AT(date).name(name).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map, int, long)
     */
    public boolean fireJob(final Object job,
                           final Map<String, Serializable> config,
                           final int times,
                           final long period) {
        return this.schedule(job,
                NOW(times, period).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date, int, long)
     */
    public boolean fireJobAt(final String name,
                             final Object job,
                             final Map<String, Serializable> config,
                             final Date date,
                             final int times,
                             final long period) {
        return this.schedule(job,
                AT(date, times, period).name(name).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#removeJob(java.lang.String)
     */
    public void removeJob(final String name) throws NoSuchElementException {
        // as this method might be called from unbind and during
        // unbind a deactivate could happen, we check the scheduler first
        final org.quartz.Scheduler s = this.scheduler;
        if ( s != null ) {
            try {
                s.deleteJob(JobKey.jobKey(name));
                this.logger.debug("Unscheduling job with name {}", name);
            } catch (final SchedulerException se) {
                throw new NoSuchElementException(se.getMessage());
            }
        }
    }

    /** Used by the web console plugin. */
    org.quartz.Scheduler getScheduler() {
        return this.scheduler;
    }

    public static final class QuartzThreadPool implements org.quartz.spi.ThreadPool {

        /** Our executor thread pool */
        private ThreadPool executor;

        /**
         * Create a new wrapper implementation for Quartz.
         */
        public QuartzThreadPool(final ThreadPool executor) {
            this.executor = executor;
        }

        /**
         * @see org.quartz.spi.QuartzThreadPool#getPoolSize()
         */
        public int getPoolSize() {
            return this.executor.getConfiguration().getMaxPoolSize();
        }

        /**
         * @see org.quartz.spi.QuartzThreadPool#initialize()
         */
        public void initialize() {
            // nothing to do
        }

        /**
         * @see org.quartz.spi.ThreadPool#setInstanceId(java.lang.String)
         */
        public void setInstanceId(final String id) {
            // we ignore this
        }

        /**
         * @see org.quartz.spi.ThreadPool#setInstanceName(java.lang.String)
         */
        public void setInstanceName(final String name) {
            // we ignore this
        }

        /**
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

        /**
         * @see org.quartz.spi.QuartzThreadPool#shutdown(boolean)
         */
        public void shutdown(final boolean waitForJobsToComplete) {
            // the pool is managed by the thread pool manager,
            // so we can just return
            this.executor = null;
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#NOW()
     */
    public ScheduleOptions NOW() {
        return new InternalScheduleOptions( TriggerBuilder.newTrigger()
                .startNow());
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#NOW(int, long)
     */
    public ScheduleOptions NOW(final int times, final long period) {
        if ( times < 2 && times != -1 ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Times argument must be higher than 1 or -1"));
        }
        if ( period < 1 ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Period argument must be higher than 0"));
        }
        final SimpleScheduleBuilder sb;
        if ( times == -1 ) {
            sb = SimpleScheduleBuilder.simpleSchedule().repeatForever();
        } else {
            sb = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(times - 1);
        }
        return new InternalScheduleOptions( TriggerBuilder.newTrigger()
            .startNow()
            .withSchedule(sb.withIntervalInMilliseconds(period * 1000)));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#AT(java.util.Date)
     */
    public ScheduleOptions AT(final Date date) {
        if ( date == null ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Date can't be null"));
        }
        return new InternalScheduleOptions( TriggerBuilder.newTrigger()
            .startAt(date));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#AT(java.util.Date, int, long)
     */
    public ScheduleOptions AT(final Date date, final int times, final long period) {
        if ( date == null ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Date can't be null"));
        }
        if ( times < 2 && times != -1 ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Times argument must be higher than 1 or -1"));
        }
        if ( period < 1 ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Period argument must be higher than 0"));
        }

        final SimpleScheduleBuilder sb;
        if ( times == -1 ) {
            sb = SimpleScheduleBuilder.simpleSchedule().repeatForever();
        } else {
            sb = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(times - 1);
        }
        return new InternalScheduleOptions( TriggerBuilder.newTrigger()
            .startAt(date)
            .withSchedule(sb.withIntervalInMilliseconds(period * 1000)));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#EXPR(java.lang.String)
     */
    public ScheduleOptions EXPR(final String expression) {
        if ( expression == null ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Expression can't be null"));
        }
        if ( !CronExpression.isValidExpression(expression) ) {
            return new InternalScheduleOptions(new IllegalArgumentException("Expressionis invalid : " + expression));
        }
        return new InternalScheduleOptions( TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(expression)));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#schedule(java.lang.Object, org.apache.sling.commons.scheduler.ScheduleOptions)
     */
    public boolean schedule(final Object job, final ScheduleOptions options) {
        try {
            this.scheduleJob(job, options);
            return true;
        } catch (final IllegalArgumentException iae) {
            // ignore this and return false
            return false;
        } catch (final SchedulerException se) {
            // ignore this and return false
            return false;
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#unschedule(java.lang.String)
     */
    public boolean unschedule(final String jobName) {
        final org.quartz.Scheduler s = this.scheduler;
        if ( jobName != null && s != null ) {
            try {
                final JobKey key = JobKey.jobKey(jobName);
                final JobDetail jobdetail = s.getJobDetail(key);
                if (jobdetail != null) {
                    s.deleteJob(key);
                    this.logger.debug("Unscheduling job with name {}", jobName);
                    return true;
                }
            } catch (final SchedulerException ignored) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Internal method to schedule a job
     * @throws SchedulerException if the job can't be scheduled
     * @throws IllegalArgumentException If the preconditions are not met
     */
    private void scheduleJob(final Object job, final ScheduleOptions options)
    throws SchedulerException {
        this.checkJob(job);

        if ( !(options instanceof InternalScheduleOptions)) {
            throw new IllegalArgumentException("Options has not been created via schedule or is null.");
        }
        final InternalScheduleOptions opts = (InternalScheduleOptions)options;

        if ( opts.argumentException != null ) {
            throw opts.argumentException;
        }

        // as this method might be called from unbind and during
        // unbind a deactivate could happen, we check the scheduler first
        final org.quartz.Scheduler s = this.scheduler;
        if ( s == null ) {
            throw new IllegalStateException("Scheduler is not available anymore.");
        }

        final String name;
        if ( opts.name != null ) {
            // if there is already a job with the name, remove it first
            try {
                final JobKey key = JobKey.jobKey(opts.name);
                final JobDetail jobdetail = s.getJobDetail(key);
                if (jobdetail != null) {
                    s.deleteJob(key);
                    this.logger.debug("Unscheduling job with name {}", opts.name);
                }
            } catch (final SchedulerException ignored) {
                // ignore
            }
            name = opts.name;
        } else {
            name = job.getClass().getName() + ':' + UUID.randomUUID();
        }

        final Trigger trigger = opts.trigger.withIdentity(name).build();

        // create the data map
        final JobDataMap jobDataMap = this.initDataMap(name, job, opts);

        final JobDetail detail = this.createJobDetail(name, jobDataMap, opts.canRunConcurrently);

        this.logger.debug("Scheduling job {} with name {} and trigger {}", new Object[] {job, name, trigger});
        s.scheduleJob(detail, trigger);
    }
}
