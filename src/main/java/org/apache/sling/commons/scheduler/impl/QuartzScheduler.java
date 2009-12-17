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
import java.util.Iterator;
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
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="org.apache.sling.commons.scheduler.Scheduler"
 * @scr.reference name="job" interface="org.apache.sling.commons.scheduler.Job" cardinality="0..n" policy="dynamic"
 * @scr.reference name="task" interface="java.lang.Runnable" cardinality="0..n" policy="dynamic"
 */
public class QuartzScheduler implements Scheduler {

    /** Default log. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String DEFAULT_QUARTZ_JOB_GROUP = "Sling";

    protected static final String PREFIX = "Apache Sling Quartz Scheduler ";

    protected static final String QUARTZ_SCHEDULER_NAME = "ApacheSling";

    /** Map key for the job object */
    static final String DATA_MAP_OBJECT = "QuartzJobScheduler.Object";

    /** Map key for the job name */
    static final String DATA_MAP_NAME = "QuartzJobScheduler.JobName";

    /** Map key for the configuration. */
    static final String DATA_MAP_CONFIGURATION = "QuartzJobScheduler.Configuration";

    /** Map key for the logger. */
    static final String DATA_MAP_LOGGER = "QuartzJobScheduler.Logger";

    /** Map key for the job handler */
    static final String DATA_MAP_JOB_HANDLER = "QuartzJobExecutor.JobHandler";

    /** Theq quartz scheduler. */
    protected volatile org.quartz.Scheduler scheduler;

    /** List of registrations while this service is not activated yet. */
    protected final List<Registration> registeredJobs = new ArrayList<Registration>();

    /** The component context. */
    protected volatile ComponentContext context;

    /** @scr.reference */
    protected ThreadPoolManager threadPoolManager;

    protected ThreadPool threadPool;

    /**
     * Activate this component.
     * Start the scheduler.
     * @param ctx The component context.
     * @throws Exception
     */
    protected void activate(final ComponentContext ctx) throws Exception {
        this.context = ctx;
        // start scheduler
        this.scheduler = this.init();

        final Registration[] regs;
        synchronized ( this.registeredJobs ) {
            regs = this.registeredJobs.toArray(new Registration[this.registeredJobs.size()]);
            this.registeredJobs.clear();
        }
        for( final Registration reg : regs ) {
            try {
                this.register(reg.componentName, reg.reference);
            } catch (Exception e) {
                // we don't want that one malicious service brings down the scheduler, so we just log
                // the exception and continue
                this.logger.error("Exception during registering " + reg.componentName + " service " + reg.reference, e);
            }
        }
    }

    /**
     * Deactivate this component.
     * Stop the scheduler.
     * @param ctx The component context.
     */
    protected void deactivate(final ComponentContext ctx) {
        final org.quartz.Scheduler s = this.scheduler;
        this.scheduler = null;
        this.dispose(s);
        this.context = null;
    }

    /**
     * Initialize the quartz scheduler
     * @return Return the new scheduler instance.
     * @throws SchedulerException
     */
    protected org.quartz.Scheduler init() throws SchedulerException {
        // if we don't have a thread pool manager, we use the default thread pool
        final ThreadPoolManager tpm = this.threadPoolManager;
        org.quartz.Scheduler s;
        if ( tpm == null ) {
            final SchedulerFactory factory = new StdSchedulerFactory();
            s = factory.getScheduler();
        } else {
            // create the pool
            this.threadPool = tpm.get(null);
            final QuartzThreadPool quartzPool = new QuartzThreadPool(this.threadPool);

            final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
            // unique run id
            final String runID = new Date().toString().replace(' ', '_');
            factory.createScheduler(QUARTZ_SCHEDULER_NAME, runID, quartzPool, new RAMJobStore());
            // quartz does not provide a way to get the scheduler by name AND runID, so we have to iterate!
            @SuppressWarnings("unchecked")
            final Iterator<org.quartz.Scheduler> allSchedulersIter = factory.getAllSchedulers().iterator();
            s = null;
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
    protected void dispose(final org.quartz.Scheduler s) {
        final ThreadPoolManager tpm = this.threadPoolManager;
        if ( tpm != null && this.threadPool != null ) {
            tpm.release(this.threadPool);
            this.threadPool = null;
        }
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
    }

    /**
     * Add a job to the scheduler
     *
     * @param name The name of the job to add (or null)
     * @param Tje jopb
     * @param trigger a Trigger
     * @param canRunConcurrently whether this job can be run concurrently
     *
     * @throws SchedulerException thrown in case of errors
     */
    protected void scheduleJob(final String name,
                               final Object job,
                               final Map<String, Serializable>    config,
                               final Trigger trigger,
                               final boolean canRunConcurrently)
    throws SchedulerException {
        // this method is also called from bind - as deactivate might have been
        // called in the meantime, we just check
        final org.quartz.Scheduler s = this.scheduler;
        if ( s == null ) {
            throw new IllegalStateException("Scheduler is not available anymore.");
        }

        // check if the supplied object is valid
        this.checkJob(job);

        // if there is already a job with the name, remove it first
        if ( name != null ) {
            try {
                final JobDetail jobdetail = s.getJobDetail(name, DEFAULT_QUARTZ_JOB_GROUP);
                if (jobdetail != null) {
                    this.removeJob(name);
                }
            } catch (final SchedulerException ignored) {
            }
        }

        final String jobName = this.getJobName(name);

        // create the data map
        final JobDataMap jobDataMap = this.initDataMap(jobName, job, config, canRunConcurrently);

        final JobDetail detail = this.createJobDetail(jobName, jobDataMap);

        this.logger.debug("Scheduling job {} with name {} and trigger {}", new Object[] {job, jobName, trigger});
        s.scheduleJob(detail, trigger);
    }

    /**
     * Initialize the data map for the job executor.
     * @param jobName
     * @param job
     * @param config
     * @param concurent
     * @return
     */
    protected JobDataMap initDataMap(final String  jobName,
                                     final Object  job,
                                     final Map<String, Serializable> config,
                                     final boolean concurrent) {
        final JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(DATA_MAP_OBJECT, job);

        jobDataMap.put(DATA_MAP_NAME, jobName);
        final JobHandler handler = new JobHandler(concurrent);
        jobDataMap.put(DATA_MAP_JOB_HANDLER, handler);
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
    protected JobDetail createJobDetail(final String name, final JobDataMap jobDataMap) {
        final JobDetail detail = new JobDetail(name, DEFAULT_QUARTZ_JOB_GROUP, QuartzJobExecutor.class);
        detail.setJobDataMap(jobDataMap);
        return detail;
    }

    /**
     * Check the job object, either runnable or job is allowed
     */
    protected void checkJob(Object job)
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
        final CronTrigger cronJobEntry = new CronTrigger(name, DEFAULT_QUARTZ_JOB_GROUP);

        try {
            cronJobEntry.setCronExpression(schedulingExpression);
        } catch (final ParseException pe) {
            throw new IllegalArgumentException("Error during parsing of cron '" + schedulingExpression + "' : " + pe.getMessage(), pe);
        }
        this.scheduleJob(name, job, config, cronJobEntry, canRunConcurrently);
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
        final long ms = period * 1000;
        final String jobName = this.getJobName(name);

        final SimpleTrigger timeEntry =
            new SimpleTrigger(jobName, DEFAULT_QUARTZ_JOB_GROUP, new Date(System.currentTimeMillis() + ms), null,
                              SimpleTrigger.REPEAT_INDEFINITELY, ms);

        this.scheduleJob(jobName, job, config, timeEntry, canRunConcurrently);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map)
     */
    public void fireJob(final Object job, final Map<String, Serializable> config)
    throws SchedulerException {
        this.checkJob(job);
        final String jobName = job.getClass().getName();
        final JobDataMap dataMap = this.initDataMap(jobName, job, config, true);

        final JobDetail detail = this.createJobDetail(jobName, dataMap);

        final Trigger trigger = new SimpleTrigger(jobName, DEFAULT_QUARTZ_JOB_GROUP);
        this.scheduler.scheduleJob(detail, trigger);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date)
     */
    public void fireJobAt(final String name, final Object job, final Map<String, Serializable> config, final Date date)
    throws SchedulerException {
        final String jobName = this.getJobName(name);
        final SimpleTrigger trigger = new SimpleTrigger(jobName, DEFAULT_QUARTZ_JOB_GROUP, date);
        this.scheduleJob(jobName, job, config, trigger, true);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map, int, long)
     */
    public boolean fireJob(final Object job,
                           final Map<String, Serializable> config,
                           final int times,
                           final long period) {
        this.checkJob(job);
        if ( times < 2 ) {
            throw new IllegalArgumentException("Times argument must be higher than 1");
        }
        final long ms = period * 1000;
        final String jobName = job.getClass().getName();
        final JobDataMap dataMap = this.initDataMap(jobName, job, config, true);

        final JobDetail detail = this.createJobDetail(jobName, dataMap);

        final Trigger trigger = new SimpleTrigger(jobName, DEFAULT_QUARTZ_JOB_GROUP, times, ms);
        try {
            this.scheduler.scheduleJob(detail, trigger);
        } catch (final SchedulerException se) {
            // ignore this and return false
            return false;
        }
        return true;
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
        if ( times < 2 ) {
            throw new IllegalArgumentException("Times argument must be higher than 1");
        }
        final String jobName = job.getClass().getName();
        final long ms = period * 1000;
        final SimpleTrigger trigger = new SimpleTrigger(jobName, DEFAULT_QUARTZ_JOB_GROUP, date, null, times, ms);
        try {
            this.scheduleJob(jobName, job, config, trigger, true);
        } catch (final SchedulerException se) {
            // ignore this and return false
            return false;
        }
        return true;
    }

    /**
     * Helper method which gets always a job name.
     * @param name The job name or null
     * @return The input job name or a unique job name.
     */
    private String getJobName(final String name) {
        return name != null ? name : PREFIX + UUID.randomUUID();
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#removeJob(java.lang.String)
     */
    public void removeJob(String name) throws NoSuchElementException {
        // as this method might be called from unbind and during
        // unbind a deactivate could happen, we check the scheduler first
        final org.quartz.Scheduler s = this.scheduler;
        if ( s != null ) {
            try {
                s.deleteJob(name, DEFAULT_QUARTZ_JOB_GROUP);
                this.logger.debug("Unscheduling job with name {}", name);
            } catch (final SchedulerException se) {
                throw new NoSuchElementException(se.getMessage());
            }
        }
    }

    /**
     * Create unique identifier
     * @param type
     * @param ref
     * @throws Exception
     */
    private String getServiceIdentifier(final ServiceReference ref) {
        String name = (String)ref.getProperty(Scheduler.PROPERTY_SCHEDULER_NAME);
        if ( name == null ) {
            name = (String)ref.getProperty(Constants.SERVICE_PID);
            if ( name == null ) {
                name = "Registered Service";
            }
        }
        // now append service id to create a unique identifer
        name = name + "." + ref.getProperty(Constants.SERVICE_ID);
        return name;
    }

    /**
     * Register a job or task
     * @param type The type (job or task)
     * @param ref The service reference
     */
    private void register(String type, ServiceReference ref) {
        // we called from bind, it might be that deactivate has been
        // called in the meantime
        final ComponentContext ctx = this.context;
        if ( ctx != null ) {
            try {
                final Object job = ctx.locateService(type, ref);
                if ( ref != null ) {
                    this.checkJob(job);
                    final String name = getServiceIdentifier(ref);
                    final Boolean concurrent = (Boolean)ref.getProperty(Scheduler.PROPERTY_SCHEDULER_CONCURRENT);
                    final String expression = (String)ref.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION);
                    if ( expression != null ) {
                        this.addJob(name, job, null, expression, (concurrent != null ? concurrent : true));
                    } else {
                        final Long period = (Long)ref.getProperty(Scheduler.PROPERTY_SCHEDULER_PERIOD);
                        if ( period != null ) {
                            this.addPeriodicJob(name, job, null, period, (concurrent != null ? concurrent : true));
                        }
                    }
                }
            } catch (IllegalStateException e) {
                // this can happen if deactivate has been called - therefore ignoring
            } catch (SchedulerException e) {
                // this can happen if deactivate has been called - therefore ignoring
            }
        }
    }

    /**
     * Unregister a service.
     * @param ref The service reference.
     */
    private void unregister(ServiceReference ref) {
        try {
            final String name = getServiceIdentifier(ref);
            this.removeJob(name);
        } catch (NoSuchElementException nsee) {
            // we ignore this
        }
    }

    /**
     * Bind a new job.
     * @param ref
     * @throws Exception
     */
    protected void bindJob(ServiceReference ref) {
        if ( this.scheduler != null ) {
            this.register(Registration.JOB, ref);
        } else {
            synchronized ( this.registeredJobs ) {
                this.registeredJobs.add(new Registration(ref, Registration.JOB));
            }
        }
    }

    /**
     * Unbind a job.
     * @param ref
     */
    protected void unbindJob(ServiceReference ref) {
        if ( this.scheduler != null ) {
            this.unregister(ref);
        } else {
            synchronized ( this.registeredJobs ) {
                this.registeredJobs.remove(new Registration(ref, Registration.JOB));
            }
        }
    }

    /**
     * Bind a new task.
     * @param ref
     * @throws Exception
     */
    protected void bindTask(ServiceReference ref) {
        if ( this.scheduler != null ) {
            this.register(Registration.TASK, ref);
        } else {
            synchronized ( this.registeredJobs ) {
                this.registeredJobs.add(new Registration(ref, Registration.TASK));
            }
        }
    }

    /**
     * Unbind a task.
     * @param ref
     */
    protected void unbindTask(ServiceReference ref) {
        if ( this.scheduler != null ) {
            this.unregister(ref);
        } else {
            synchronized ( this.registeredJobs ) {
                this.registeredJobs.remove(new Registration(ref, Registration.TASK));
            }
        }
    }

    /**
     * Helper class holding a registration if this service is not active yet.
     */
    private static final class Registration {
        public static final String JOB = "job";
        public static final String TASK = "task";

        public final ServiceReference reference;
        public final String componentName;

        public Registration(final ServiceReference r, final String name) {
            this.reference = r;
            this.componentName = name;
        }

        @Override
        public boolean equals(Object obj) {
            if ( !(obj instanceof Registration) ) {
                return false;
            }
            if ( obj == this ) {
                return true;
            }
            return this.reference.equals(((Registration)obj).reference);
        }

        @Override
        public int hashCode() {
            return this.reference.hashCode();
        }
    }


    private static final class QuartzThreadPool implements org.quartz.spi.ThreadPool {

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
