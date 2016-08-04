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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
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
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quartz based implementation of the scheduler.
 *
 */
@Component(immediate=true,
           metatype=true, label="Apache Sling Scheduler",
           description="The scheduler is able to run services and jobs at specific " +
                       "times or periodically based on cron expressions.")
@Service(value=QuartzScheduler.class)
public class QuartzScheduler implements BundleListener {

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

    /** Map key for the bundle information (Long). */
    static final String DATA_MAP_BUNDLE_ID = "QuartzJobScheduler.bundleId";

    /** Map key for the bundle information (Long). */
    static final String DATA_MAP_SERVICE_ID = "QuartzJobScheduler.serviceId";

    @Property(label="Thread Pool Name",
            description="The name of a configured thread pool - if no name is configured " +
                        "the default pool is used.")
    private static final String PROPERTY_POOL_NAME = "poolName";

    @Property(label="Allowed Thread Pools",
             description="The names of thread pools that are allowed to be used by jobs. " +
                        "If a job is using a pool not in this list, the default pool is used.",
             unbounded=PropertyUnbounded.ARRAY)
    private static final String PROPERTY_ALLOWED_POOLS = "allowedPoolNames";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ThreadPoolManager threadPoolManager;

    /** The quartz schedulers. */
    private final Map<String, SchedulerProxy> schedulers = new HashMap<String, SchedulerProxy>();

    private volatile String defaultPoolName;

    private volatile String[] allowedPoolNames;

    private volatile boolean active;

    /**
     * Activate this component.
     * Start the scheduler.
     * @throws Exception
     */
    @Activate
    protected void activate(final BundleContext ctx, final Map<String, Object> props) {
        // SLING-2261 Prevent Quartz from checking for updates
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", Boolean.TRUE.toString());

        final Object poolNameObj = props.get(PROPERTY_POOL_NAME);
        if ( poolNameObj != null && poolNameObj.toString().trim().length() > 0 ) {
            this.defaultPoolName = poolNameObj.toString().trim();
        } else {
            this.defaultPoolName = ThreadPoolManager.DEFAULT_THREADPOOL_NAME;
        }
        final Object value = props.get(PROPERTY_ALLOWED_POOLS);
        if ( value instanceof String[] ) {
            this.allowedPoolNames = (String[])value;
        } else if ( value != null ) {
            this.allowedPoolNames = new String[] {value.toString()};
        }
        if ( this.allowedPoolNames == null ) {
            this.allowedPoolNames = new String[0];
        } else {
            for(int i=0;i<this.allowedPoolNames.length;i++) {
                if ( this.allowedPoolNames[i] == null ) {
                    this.allowedPoolNames[i] = "";
                } else {
                    this.allowedPoolNames[i] = this.allowedPoolNames[i].trim();
                }
            }
        }
        ctx.addBundleListener(this);

        this.active = true;
    }

    /**
     * Deactivate this component.
     * Stop the scheduler.
     */
    @Deactivate
    protected void deactivate(final BundleContext ctx) {
        ctx.removeBundleListener(this);

        final Map<String, SchedulerProxy> proxies;
        synchronized ( this.schedulers ) {
            this.active = false;
            proxies = new HashMap<String, SchedulerProxy>(this.schedulers);
            this.schedulers.clear();
        }
        for(final SchedulerProxy proxy : proxies.values()) {
            proxy.dispose();
        }
    }

    /**
     * Get the thread pool name to use based on the provided/configured name
     * @param name The configured name
     * @return The name to use
     */
    private String getThreadPoolName(final String name) {
        // no name specified
        if ( name == null || name.trim().isEmpty() ) {
            return this.defaultPoolName;
        }
        // checked allowed list
        for(final String n : this.allowedPoolNames) {
            if ( name.trim().equals(n) ) {
                return n;
            }
        }
        logger.warn("Scheduler job requested thread pool with name " + name + " but this thread pool is not in the list of allowed pools.");
        return this.defaultPoolName;
    }

    private SchedulerProxy getScheduler(final String pName) throws SchedulerException {
        final String poolName = getThreadPoolName(pName);
        SchedulerProxy proxy = null;
        synchronized ( this.schedulers ) {
            if ( this.active ) {
                proxy = this.schedulers.get(poolName);
                if ( proxy == null ) {
                    proxy = new SchedulerProxy(this.threadPoolManager, poolName);
                    this.schedulers.put(poolName, proxy);
                }
            }
        }
        return proxy;
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    @Override
    public void bundleChanged(final BundleEvent event) {
        if ( event.getType() == BundleEvent.STOPPED ) {
            final Long bundleId = event.getBundle().getBundleId();

            final Map<String, SchedulerProxy> proxies;
            synchronized ( this.schedulers ) {
                if ( this.active ) {
                    proxies = new HashMap<String, SchedulerProxy>(this.schedulers);
                } else {
                    proxies = Collections.emptyMap();
                }
            }
            for(final SchedulerProxy proxy : proxies.values()) {
                synchronized ( proxy ) {
                    try {
                        final List<String> groups = proxy.getScheduler().getJobGroupNames();
                        for(final String group : groups) {
                            final Set<JobKey> keys = proxy.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(group));
                            for(final JobKey key : keys) {
                                final JobDetail detail = proxy.getScheduler().getJobDetail(key);
                                if ( detail != null ) {
                                    final String jobName = (String) detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_NAME);
                                    final Object job = detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_OBJECT);

                                    if ( jobName != null && job != null ) {
                                        final Long jobBundleId = (Long) detail.getJobDataMap().get(QuartzScheduler.DATA_MAP_BUNDLE_ID);
                                        if ( jobBundleId != null && jobBundleId.equals(bundleId) ) {
                                            proxy.getScheduler().deleteJob(key);
                                            this.logger.debug("Unscheduling job with name {}", jobName);
                                        }
                                    }
                                }
                            }
                        }
                    } catch ( final SchedulerException ignore) {
                        // we ignore this as there is nothing to do
                    }
                }
            }
        }

    }

    /**
     * Initialize the data map for the job executor.
     * @param jobName
     * @param job
     * @param config
     * @param concurent
     * @return
     */
    private JobDataMap initDataMap(final Long    bundleId,
                                   final Long    serviceId,
                                   final String  jobName,
                                   final Object  job,
                                   final InternalScheduleOptions options) {
        final JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(DATA_MAP_OBJECT, job);

        jobDataMap.put(DATA_MAP_NAME, jobName);
        jobDataMap.put(DATA_MAP_LOGGER, this.logger);
        if ( bundleId != null ) {
            jobDataMap.put(DATA_MAP_BUNDLE_ID, bundleId);
        }
        if ( serviceId != null ) {
            jobDataMap.put(DATA_MAP_SERVICE_ID, serviceId);
        }
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
    public void addJob(final Long bundleId,
                       final Long serviceId,
                       final String name,
                       final Object job,
                       final Map<String, Serializable>    config,
                       final String schedulingExpression,
                       final boolean canRunConcurrently)
    throws SchedulerException {
        this.scheduleJob(bundleId, serviceId, job,
                EXPR(schedulingExpression).name(name).config(config).canRunConcurrently(canRunConcurrently));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean)
     */
    public void addPeriodicJob(final Long bundleId,
                               final Long serviceId,
                               final String name,
                               final Object job,
                               final Map<String, Serializable> config,
                               final long period,
                               final boolean canRunConcurrently)
    throws SchedulerException {
        this.addPeriodicJob(bundleId, serviceId, name, job, config, period, canRunConcurrently, false);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean, boolean)
     */
    public void addPeriodicJob(final Long bundleId,
            final Long serviceId,
            final String name,
            final Object job,
            final Map<String, Serializable> config,
            final long period,
            final boolean canRunConcurrently,
            final boolean startImmediate)
    throws SchedulerException {
        this.scheduleJob(bundleId, serviceId, job,
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
    public void fireJob(final Long bundleId,
            final Long serviceId,
            final Object job, final Map<String, Serializable> config)
    throws SchedulerException {
        this.scheduleJob(bundleId, serviceId, job,
                NOW().config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date)
     */
    public void fireJobAt(final Long bundleId,
            final Long serviceId,
            final String name, final Object job, final Map<String, Serializable> config, final Date date)
    throws SchedulerException {
        this.scheduleJob(bundleId, serviceId, job,
                AT(date).name(name).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map, int, long)
     */
    public boolean fireJob(final Long bundleId,
                           final Long serviceId,
                           final Object job,
                           final Map<String, Serializable> config,
                           final int times,
                           final long period) {
        return this.schedule(bundleId, serviceId, job,
                NOW(times, period).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date, int, long)
     */
    public boolean fireJobAt(final Long bundleId,
                             final Long serviceId,
                             final String name,
                             final Object job,
                             final Map<String, Serializable> config,
                             final Date date,
                             final int times,
                             final long period) {
        return this.schedule(bundleId, serviceId, job,
                AT(date, times, period).name(name).config(config));
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#removeJob(java.lang.String)
     */
    public void removeJob(final Long bundleId, final String jobName) throws NoSuchElementException {
        // as this method might be called from unbind and during
        // unbind a deactivate could happen, we check the scheduler first
        final Map<String, SchedulerProxy> proxies;
        synchronized ( this.schedulers ) {
            if ( this.active ) {
                proxies = new HashMap<String, SchedulerProxy>(this.schedulers);
            } else {
                proxies = Collections.emptyMap();
            }
        }
        for(final SchedulerProxy proxy : proxies.values()) {
            synchronized ( proxy ) {
                try {
                    final JobKey key = JobKey.jobKey(jobName);
                    final JobDetail jobdetail = proxy.getScheduler().getJobDetail(key);
                    if (jobdetail != null) {
                        proxy.getScheduler().deleteJob(key);
                        this.logger.debug("Unscheduling job with name {}", jobName);
                        return;
                    }
                } catch (final SchedulerException ignored) {
                    // ignore
                }
            }
        }
        if ( this.active ) {
            throw new NoSuchElementException("No job found with name " + jobName);
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
    public boolean schedule(final Long bundleId, final Long serviceId, final Object job, final ScheduleOptions options) {
        try {
            this.scheduleJob(bundleId, serviceId, job, options);
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
    public boolean unschedule(final Long bundleId, final String jobName) {
        if ( jobName != null ) {
            final Map<String, SchedulerProxy> proxies;
            synchronized ( this.schedulers ) {
                proxies = new HashMap<String, SchedulerProxy>(this.schedulers);
            }
            for(final SchedulerProxy proxy : proxies.values()) {
                synchronized ( proxy ) {
                    try {
                        final JobKey key = JobKey.jobKey(jobName);
                        final JobDetail jobdetail = proxy.getScheduler().getJobDetail(key);
                        if (jobdetail != null) {
                            proxy.getScheduler().deleteJob(key);
                            this.logger.debug("Unscheduling job with name {}", jobName);
                            return true;
                        }
                    } catch (final SchedulerException ignored) {
                        // ignore
                    }
                }
            }
        }
        return false;
    }

    /**
     * Internal method to schedule a job
     * @throws SchedulerException if the job can't be scheduled
     * @throws IllegalArgumentException If the preconditions are not met
     */
    private void scheduleJob(final Long bundleId, final Long serviceId, final Object job, final ScheduleOptions options)
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
        final SchedulerProxy proxy = this.getScheduler(opts.threadPoolName);
        if ( proxy == null ) {
            throw new IllegalStateException("Scheduler is not available anymore.");
        }

        synchronized ( proxy ) {
            final String name;
            if ( opts.name != null ) {
                // if there is already a job with the name, remove it first
                this.unschedule(bundleId, opts.name);
                name = opts.name;
            } else {
                name = job.getClass().getName() + ':' + UUID.randomUUID();
            }

            final Trigger trigger = opts.trigger.withIdentity(name).build();

            // create the data map
            final JobDataMap jobDataMap = this.initDataMap(bundleId, serviceId, name, job, opts);

            final JobDetail detail = this.createJobDetail(name, jobDataMap, opts.canRunConcurrently);

            this.logger.debug("Scheduling job {} with name {} and trigger {}", new Object[] {job, name, trigger});
            proxy.getScheduler().scheduleJob(detail, trigger);
        }
    }

    /**
     * This is used by the web console plugin
     * @return All current schedulers
     */
    Map<String, SchedulerProxy> getSchedulers() {
        synchronized ( this.schedulers ) {
            return new HashMap<String, SchedulerProxy>(this.schedulers);
        }
    }
}
