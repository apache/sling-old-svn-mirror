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

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.sling.commons.metrics.Gauge;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        property = {
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
        },
        immediate = true
    )
/**
 * This service creates gauges for getting how long the oldest running job is
 * already running.
 * <p>
 * If it hits slow jobs (which are configurable, default is at 1 second, it
 * creates a temporary gauge for that. These temporary gauges subsequently
 * become visible in JMX and allow to get insight information about jobs that eg
 * run forever or for a very long time. The other measurements that are done,
 * such as job timers (which also separate out slow jobs), only hit once a job
 * is finished. This temporary gauge mechanism now additionally allows to see
 * runtime information about currently running jobs (that might only become
 * visible once they finish, which they might not or just in a long time)
 */
public class GaugesSupport {

    private final static String CLEANUP_JOB_NAME = "org.apache.sling.commons.scheduler.impl.GaugesSupport.CleanupJob";

    @SuppressWarnings("rawtypes")
    private final class TemporaryGauge implements Gauge {
        private final ServiceRegistration registration;
        private final JobExecutionContext jobExecutionContext;
        private final String gaugeName;
        private volatile boolean unregistered = false;

        private TemporaryGauge(BundleContext ctx, JobExecutionContext jobExecutionContext, String gaugeName) {
            this.jobExecutionContext = jobExecutionContext;
            this.gaugeName = gaugeName;

            Dictionary<String, String> p = new Hashtable<String, String>();
            p.put(Gauge.NAME, gaugeName);
            registration = ctx.registerService(Gauge.class.getName(), TemporaryGauge.this, p);
        }

        private void unregister() {
            synchronized (this) {
                if (unregistered) {
                    return;
                }
                unregistered = true;
            }
            synchronized (temporaryGauges) {
                if (temporaryGauges.get(gaugeName) == TemporaryGauge.this) {
                    logger.debug("unregister: unregistering active temporary gauge for slow job : " + gaugeName);
                    temporaryGauges.remove(gaugeName);
                } else {
                    // else leaving it as is, there's already a new gauge with
                    // the same name
                    logger.debug("unregister: unregistering dangling temporary gauge for slow job : " + gaugeName);
                }
            }
            registration.unregister();
        }

        @Override
        public Long getValue() {
            if (unregistered) {
                return -1L;
            }
            if (!active) {
                unregister();
                return -1L; // quartzscheduler is no longer active, unregister
            }
            if (jobExecutionContext.getJobRunTime() != -1) {
                unregister();
                return -1L; // job is finished, unregister automatically
            }
            final Date oldestDate = jobExecutionContext.getFireTime();
            if (oldestDate == null) {
                // never fired? this should not happen - but unregister to be
                // safe
                unregister();
                return -1L;
            } else {
                return System.currentTimeMillis() - oldestDate.getTime();
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("rawtypes")
    private final Map<String, ServiceRegistration> gaugeRegistrations = new HashMap<String, ServiceRegistration>();
    private final Map<String, TemporaryGauge> temporaryGauges = new HashMap<String, TemporaryGauge>();

    private volatile boolean active = true;

    @Reference
    QuartzScheduler quartzScheduler;

    private ConfigHolder configHolder;

    private long bundleId;

    @Activate
    protected void activate(final BundleContext ctx) {
        logger.debug("activate: activating.");
        configHolder = quartzScheduler.configHolder;
        active = true;

        // register the gauges
        registerGauges(ctx);

        bundleId = ctx.getBundle().getBundleId();
        try {
            quartzScheduler.addPeriodicJob(bundleId, null, CLEANUP_JOB_NAME, new Runnable() {

                @Override
                public void run() {
                    if (active) {
                        cleanupTemporaryGauges();
                    } // if the GaugesSupport isn't active anymore it is so
                      // because
                      // the QuartzScheduler was deactivated - which means we
                      // don't
                      // have to unregister the periodic job here.
                    else {
                        logger.debug("run: late executed periodic cleanup job - ignoring");
                    }
                }

            }, null, 1800 /* 1800sec == 30min */, false);
        } catch (SchedulerException e) {
            throw new RuntimeException("Could not activate GaugesSupport due to " + e, e);
        }
    }

    private void cleanupTemporaryGauges() {
        logger.debug("cleanupTemporaryGauges: checking for unused temporary gauges.");
        final long start = System.currentTimeMillis();
        final Map<String, TemporaryGauge> localTemporaryGauges;
        synchronized (temporaryGauges) {
            localTemporaryGauges = new HashMap<String, TemporaryGauge>(temporaryGauges);
        }
        final Iterator<TemporaryGauge> it = localTemporaryGauges.values().iterator();
        while (it.hasNext()) {
            TemporaryGauge gauge = it.next();
            // calling getValue will trigger the unregistration if applicable
            gauge.getValue();
        }
        final int endCount;
        synchronized (temporaryGauges) {
            endCount = temporaryGauges.size();
        }
        final long diff = System.currentTimeMillis() - start;
        logger.debug("cleanupTemporaryGauges: done. (temporary gauges at start : " + localTemporaryGauges.size()
                + ", at end : " + endCount + ", cleanup took : " + diff + "ms)");
    }

    private void registerGauges(BundleContext ctx) {
        createGauge(ctx, configHolder, null, null, QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS);
        createGauge(ctx, configHolder, configHolder.poolName(), null,
                QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS + ".tp." + configHolder.poolName());
        if (configHolder.allowedPoolNames() != null) {
            for (String tpName : configHolder.allowedPoolNames()) {
                createGauge(ctx, configHolder, tpName, null,
                        QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS + ".tp." + tpName);
            }
        }
        for (Map.Entry<String, String> entry : configHolder.getFilterSuffixes().entrySet()) {
            final String name = entry.getKey();
            final String filterName = entry.getValue();
            createGauge(ctx, configHolder, null, filterName,
                    QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS + ".filter." + name);
        }
    }

    @SuppressWarnings("rawtypes")
    private void createGauge(final BundleContext ctx, final ConfigHolder configHolder, final String tpName,
            final String filterName, final String gaugeName) {
        Dictionary<String, String> p = new Hashtable<String, String>();
        p.put(Gauge.NAME, gaugeName);
        final Gauge gauge = new Gauge() {
            @Override
            public Long getValue() {
                if (!active) {
                    return -1L; // disabled case
                }
                return getOldestRunningJobMillis(configHolder, ctx, tpName, filterName);
            }
        };
        logger.debug("createGauge: registering gauge : " + gaugeName);
        ServiceRegistration reg = ctx.registerService(Gauge.class.getName(), gauge, p);
        synchronized (this.gaugeRegistrations) {
            gaugeRegistrations.put(gaugeName, reg);
        }
    }

    private Long getOldestRunningJobMillis(ConfigHolder configHolder, BundleContext ctx, String threadPoolNameOrNull,
            String filterNameOrNull) {
        final QuartzScheduler localQuartzScheduler = quartzScheduler;
        if (localQuartzScheduler == null) {
            // could happen during deactivation
            return -1L;
        }
        Map<String, SchedulerProxy> schedulers = localQuartzScheduler.getSchedulers();
        if (schedulers == null) {
            // guessing this is should never happen - so just for paranoia
            return -1L;
        }

        Date oldestDate = null;
        if (filterNameOrNull == null && threadPoolNameOrNull != null) {
            // if a threadPoolName is set and no filter then we go by
            // threadPoolName
            final SchedulerProxy schedulerProxy = schedulers.get(threadPoolNameOrNull);
            oldestDate = getOldestRunningJobDate(configHolder, ctx, schedulerProxy, null);
        } else {
            // if nothing is set we iterate over everything
            // if both threadPoolName and filter is set, filter has precedence
            // (hence we iterate over everything but using the filter)
            for (Map.Entry<String, SchedulerProxy> entry : schedulers.entrySet()) {
                SchedulerProxy schedulerProxy = entry.getValue();
                oldestDate = olderOf(oldestDate,
                        getOldestRunningJobDate(configHolder, ctx, schedulerProxy, filterNameOrNull));
            }
        }
        if (oldestDate == null) {
            return -1L;
        } else {
            return System.currentTimeMillis() - oldestDate.getTime();
        }
    }

    private Date getOldestRunningJobDate(final ConfigHolder configHolder, final BundleContext ctx,
            final SchedulerProxy schedulerProxy, final String filterNameOrNull) {
        if (schedulerProxy == null) {
            return null;
        }
        final org.quartz.Scheduler scheduler = schedulerProxy.getScheduler();
        if (scheduler == null) {
            return null;
        }
        List<JobExecutionContext> currentlyExecutingJobs = null;
        try {
            currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
        } catch (SchedulerException e) {
            logger.warn("getValue: could not get currently executing jobs due to Exception: " + e, e);
        }
        if (currentlyExecutingJobs == null)
            return null;
        Date oldestDate = null;
        for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
            if (jobExecutionContext == null) {
                continue;
            }
            if (filterNameOrNull != null) {
                // apply the filter
                JobDetail jobDetail = jobExecutionContext.getJobDetail();
                JobDataMap map = null;
                if (jobDetail != null) {
                    map = jobDetail.getJobDataMap();
                }
                String filterName = null;
                if (map != null) {
                    filterName = MetricsHelper.deriveFilterName(configHolder, map.get(QuartzScheduler.DATA_MAP_OBJECT));
                }
                if (filterName == null || !filterNameOrNull.equals(filterName)) {
                    // filter doens't match
                    continue;
                }
                // filter matches, go ahead and get the fire time
            }
            final Date fireTime = jobExecutionContext.getFireTime();
            final long elapsedMillis = System.currentTimeMillis() - fireTime.getTime();
            final long slowThresholdMillis = configHolder.slowThresholdMillis();
            if (slowThresholdMillis > 0 && elapsedMillis > slowThresholdMillis) {
                // then create a gauge for this slow job in case there isn't one
                // yet
                createTemporaryGauge(ctx, jobExecutionContext);
            }
            oldestDate = olderOf(oldestDate, fireTime);
        }
        return oldestDate;
    }

    private static Date olderOf(Date date1, final Date date2) {
        if (date1 == null)
            return date2;
        if (date2 == null)
            return date1;
        if (date2.before(date1)) {
            return date2;
        } else {
            return date1;
        }
    }

    private void createTemporaryGauge(final BundleContext ctx, final JobExecutionContext jobExecutionContext) {
        final JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        final String jobName = data.getString(QuartzScheduler.DATA_MAP_NAME);
        final String gaugeName = QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS + ".slow."
                + MetricsHelper.asMetricsSuffix(jobName);
        TemporaryGauge tempGauge;
        synchronized (temporaryGauges) {
            tempGauge = temporaryGauges.get(gaugeName);
            if (tempGauge != null) {
                // then there is already a gauge for this job execution
                // check if it has the same jobExecutionContext
                if (tempGauge.jobExecutionContext == jobExecutionContext) {
                    // then all is fine, skip
                    return;
                }
                // otherwise the current temporary gauge is an old one, that job
                // execution has already finished
                // so we should unregister that one and create a new one

                // the unregister we want to do outside of this sync block
                // though
            }
        }
        if (tempGauge != null) {
            logger.debug("createTemporaryGauge: unregistering temporary gauge for slow job : " + gaugeName);
            tempGauge.unregister();
        }
        logger.debug("createTemporaryGauge: creating temporary gauge for slow job : " + gaugeName);
        synchronized (this.temporaryGauges) {
            temporaryGauges.put(gaugeName, new TemporaryGauge(ctx, jobExecutionContext, gaugeName));
        }
    }

    @SuppressWarnings("rawtypes")
    private void unregisterGauges() {
        final Map<String, ServiceRegistration> localGaugeRegistrations;
        synchronized (gaugeRegistrations) {
            localGaugeRegistrations = new HashMap<String, ServiceRegistration>(gaugeRegistrations);
            gaugeRegistrations.clear();
        }
        final Map<String, TemporaryGauge> localTemporaryGauges;
        synchronized (temporaryGauges) {
            localTemporaryGauges = new HashMap<String, TemporaryGauge>(temporaryGauges);
        }

        final Iterator<Entry<String, ServiceRegistration>> it = localGaugeRegistrations.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, ServiceRegistration> e = it.next();
            logger.debug("unregisterGauges: unregistering gauge : " + e.getKey());
            e.getValue().unregister();
        }

        for (TemporaryGauge tempGauge : localTemporaryGauges.values()) {
            logger.debug("unregisterGauges: unregistering temporary gauge for slow job : " + tempGauge.gaugeName);
            tempGauge.unregister();
        }
    }

    @Deactivate
    void deactivate() {
        logger.debug("deactivate: deactivating.");
        active = false;

        try {
            // likely the job got removed by the QuartzScheduler itself,
            // if not, lets remove it explicitly too
            quartzScheduler.removeJob(bundleId, CLEANUP_JOB_NAME);
        } catch (NoSuchElementException e) {
            // this is fine
        }

        unregisterGauges();
    }

}
