/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

/**
 * HealthCheck that checks if the Sling Scheduler has any long-running (by
 * default more than 60 seconds) Quartz-Jobs. If there are any long running
 * Quartz-Jobs they occupy threads from the (limited) thread-pool and might lead
 * to preventing other Quartz-Jobs from executing properly. The threshold
 * (60sec) is configurable.
 */
@Component(service = HealthCheck.class, property = { Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        HealthCheck.NAME + "=Scheduler Health Check",
        HealthCheck.MBEAN_NAME + "=slingCommonsSchedulerHealthCheck" }, immediate = true)
@Designate(ocd = SchedulerHealthCheck.Config.class)
public class SchedulerHealthCheck implements HealthCheck {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private MetricRegistry metricRegistry;

    @ObjectClassDefinition(name = "Apache Sling Scheduler Health Check Config", description = "Apache Sling Scheduler Health Check Config")
    public @interface Config {
        @AttributeDefinition(name = "Acceptable Duration Millis", description = "Maximum a job should take (in millis) for it to be acceptable. "
                + "Best to set this equal or higher to org.apache.sling.commons.scheduler.impl.QuartzScheduler.slowThresholdMillis")
        long max_quartzJob_duration_acceptable() default DEFAULT_MAX_QUARTZJOB_DURATION_ACCCEPTABLE;
    }

    private static final long DEFAULT_MAX_QUARTZJOB_DURATION_ACCCEPTABLE = 60000;
    private long maxQuartzJobDurationAcceptable;

    @Activate
    protected void activate(Config config) {
        configure(config);
    }

    @Modified
    protected void modified(Config config) {
        configure(config);
    }

    protected void configure(Config config) {
        maxQuartzJobDurationAcceptable = config.max_quartzJob_duration_acceptable();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        try {
            long runningCount = 0;
            final SortedMap<String, Counter> runningCntSet = metricRegistry.getCounters(new MetricFilter() {

                @Override
                public boolean matches(String name, Metric metric) {
                    return name.equals(QuartzScheduler.METRICS_NAME_RUNNING_JOBS);
                }
            });
            if (runningCntSet != null) {
                Iterator<Counter> it = runningCntSet.values().iterator();
                if (it.hasNext()) {
                    runningCount = it.next().getCount();
                }
                runningCount = Math.max(0, runningCount);
            }
            final SortedMap<String, Gauge> oldestGaugeSet = metricRegistry.getGauges(new MetricFilter() {

                @Override
                public boolean matches(String name, Metric metric) {
                    return name.equals(QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS);
                }
            });
            if (oldestGaugeSet.isEmpty()) {
                resultLog.warn("Sling Scheduler cannot find any metrics gauge starting with {}",
                        QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS);
            } else {
                final long oldestRunningJobInMillis = (Long) oldestGaugeSet.values().iterator().next().getValue();
                if (oldestRunningJobInMillis <= -1) {
                    resultLog.info("Sling Scheduler has no long-running Quartz-Jobs at this moment.");
                } else if (oldestRunningJobInMillis > maxQuartzJobDurationAcceptable) {
                    final String slowPrefix = QuartzScheduler.METRICS_NAME_OLDEST_RUNNING_JOB_MILLIS + ".slow.";
                    final MetricFilter filter = new MetricFilter() {

                        @Override
                        public boolean matches(String name, Metric metric) {
                            return name.startsWith(slowPrefix);
                        }
                    };
                    final SortedMap<String, Gauge> allGaugeSet = metricRegistry.getGauges(filter);
                    if (allGaugeSet.isEmpty()) {
                        resultLog.critical(
                                "Sling Scheduler has at least one long-running Quartz-Job with the oldest running for {}ms.",
                                oldestRunningJobInMillis);
                    } else {
                        final StringBuffer slowNames = new StringBuffer();
                        final Iterator<Entry<String, Gauge>> it = allGaugeSet.entrySet().iterator();
                        int numSlow = 0;
                        while (it.hasNext()) {
                            final Entry<String, Gauge> e = it.next();
                            final Gauge slowGauge = e.getValue();
                            final long millis = (Long) slowGauge.getValue();
                            if (millis < 0) {
                                // skip - this job is no longer running
                                continue;
                            }
                            if (numSlow++ > 0) {
                                slowNames.append(", ");
                            }
                            slowNames.append(e.getKey().substring(slowPrefix.length()));
                            slowNames.append("=").append(millis).append("ms");
                        }
                        if (numSlow == 1) {
                            resultLog.critical(
                                    "Sling Scheduler has 1 long-running Quartz-Job which is already running for {}ms: {}.",
                                    oldestRunningJobInMillis, slowNames);
                        } else {
                            resultLog.critical(
                                    "Sling Scheduler has {} long-running Quartz-Jobs with the oldest running for {}ms: {}.",
                                    numSlow, oldestRunningJobInMillis, slowNames);
                        }
                    }
                    resultLog.info("More details are exposed in metrics including gauges for slow Quartz-Jobs containing shortened job names.");
                    resultLog.info("Furthermore, thread-dumps can also help narrow down slow Quartz-Jobs.");
                } else {
                    resultLog.info(
                            "Sling Scheduler has no long-running Quartz-Jobs at this moment, the oldest current Quartz-Job is {}ms.",
                            oldestRunningJobInMillis);
                }
                resultLog.info("The total number of currently runnning Quartz-Jobs is {}.", runningCount);
                resultLog.info("The maximum acceptable duration a Quartz-Job should run for is configured to {}ms. "
                        + "[This duration can be changed in the QuartzScheduler via the configuration manager]({})", 
                        maxQuartzJobDurationAcceptable,
                        "/system/console/configMgr/org.apache.sling.commons.scheduler.impl.QuartzScheduler");
            }
        } catch (final Exception e) {
            logger.warn("execute: metrics invocation failed with exception: {}", e);
            resultLog.healthCheckError("execute: metrics invocation failed with exception: {}", e);
        }

        return new Result(resultLog);
    }

}
