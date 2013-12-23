/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.core.impl.executor;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.hc.api.HealthCheckExecutor;
import org.apache.sling.hc.api.HealthCheckResult;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs health checks for a given list of tags in parallel.
 *
 */
@Service(value = HealthCheckExecutor.class)
@Component(label = "Apache Sling Health Check Executor",
        description = "Runs health checks for a given list of tags in parallel.",
        metatype = true, immediate = true)
public class HealthCheckExecutorImpl implements HealthCheckExecutor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long TIMEOUT_DEFAULT_MS = 2000;

    public static final String PROP_TIMEOUT_MS = "timeoutInMs";
    @Property(name = PROP_TIMEOUT_MS, label = "Timeout",
            description = "Timeout in ms until a check is marked as timed out",
            longValue = TIMEOUT_DEFAULT_MS)
    private Long timeoutInMs;

    private static final long LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS = 1000 * 60 * 5;

    public static final String PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS = "longRunningFutureThresholdForCriticalMs";
    @Property(name = PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS, label = "Timeout threshold for CRITICAL",
            description = "Threshold in ms until a check is marked as 'exceedingly' timed out and will marked CRITICAL instead of WARN only",
            longValue = LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS)
    private Long longRunningFutureThresholdForRedMs;

    static final String PROP_DISABLED_CHECKS = "disabledChecks";
    @Property(name = PROP_DISABLED_CHECKS, label = "Disabled Checks", cardinality = Integer.MAX_VALUE,
            description = "Empty by default, allows to disable checks if necessary (add fully qualified class names here)",
            value = {})
    private String[] disabledChecks;

    private static final long RESULT_CACHE_TTLL_DEFAULT_MS = 1000 * 2;
    public static final String PROP_RESULT_CACHE_TTL_MS = "resultCacheTtlInMs";
    @Property(name = PROP_RESULT_CACHE_TTL_MS, label = "Results Cache TTL in Ms",
            description = "Result Cache time to live - results will be cached for the given time",
            longValue = RESULT_CACHE_TTLL_DEFAULT_MS)
    private Long resultCacheTtlInMs;

    private HealthCheckResultCache healthCheckResultCache = new HealthCheckResultCache();

    private Map<HealthCheckDescriptor, HealthCheckFuture> stillRunningFutures = new ConcurrentHashMap<HealthCheckDescriptor, HealthCheckFuture>();

    @Reference
    private ThreadPoolManager threadPoolManager;
    private ThreadPool hcThreadPool;

    private BundleContext bundleContext;

    @Activate
    protected final void activate(final ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();

        ModifiableThreadPoolConfig hcThreadPoolConfig = new ModifiableThreadPoolConfig();
        hcThreadPoolConfig.setMaxPoolSize(25);
        hcThreadPool = threadPoolManager.create(hcThreadPoolConfig, "Health Check Thread Pool");

        final Dictionary<?, ?> properties = componentContext.getProperties();
        this.disabledChecks = (String[]) properties.get(PROP_DISABLED_CHECKS);
        this.timeoutInMs = (Long) properties.get(PROP_TIMEOUT_MS);

        if (this.timeoutInMs == null || this.timeoutInMs == 0L) {
            this.timeoutInMs = TIMEOUT_DEFAULT_MS;
        }

        this.longRunningFutureThresholdForRedMs = (Long) properties.get(PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS);
        if (this.longRunningFutureThresholdForRedMs == null || this.longRunningFutureThresholdForRedMs == 0L) {
            this.longRunningFutureThresholdForRedMs = LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS;
        }

        this.resultCacheTtlInMs = (Long) properties.get(PROP_RESULT_CACHE_TTL_MS);
        if (this.resultCacheTtlInMs == null || this.resultCacheTtlInMs == 0L) {
            this.resultCacheTtlInMs = RESULT_CACHE_TTLL_DEFAULT_MS;
        }
    }

    @Deactivate
    protected final void deactivate(final ComponentContext componentContext) {
        threadPoolManager.release(hcThreadPool);
        this.bundleContext = null;
    }

    /**
     * @see org.apache.sling.hc.api.HealthCheckExecutor#executeAll()
     */
    @Override
    public Collection<HealthCheckResult> executeAll() {
        return executeAllForTags(new String[0]);
    }

    /**
     * @see org.apache.sling.hc.api.HealthCheckExecutor#executeAllForTags(java.lang.String[])
     */
    @Override
    public Collection<HealthCheckResult> executeAllForTags(final String... tags) {

        logger.debug("Starting executing all checks... ");

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Collection<HealthCheckResult> results = new TreeSet<HealthCheckResult>();
        final List<HealthCheckDescriptor> healthCheckDescriptors = getHealthCheckDescriptors(tags);

        createResultsForDescriptors(healthCheckDescriptors, results);

        stopWatch.stop();
        logger.debug("Time consumed for all checks: " + msHumanReadable(stopWatch.getTime()));

        return results;
    }

    @Override
    public HealthCheckResult execute(ServiceReference healthCheckReference) {

        final List<HealthCheckDescriptor> healthCheckDescriptors = new LinkedList<HealthCheckDescriptor>();
        healthCheckDescriptors.add(new HealthCheckDescriptor(healthCheckReference));

        final Collection<HealthCheckResult> results = new TreeSet<HealthCheckResult>();

        createResultsForDescriptors(healthCheckDescriptors, results);

        return results.iterator().next();
    }


    private void createResultsForDescriptors(final List<HealthCheckDescriptor> healthCheckDescriptors,
            final Collection<HealthCheckResult> results) {
        // -- All methods below check if they can transform a healthCheckDescriptor into a result
        // -- if yes the descriptor is removed from the list and the result added

        // reuse cached results where possible
        healthCheckResultCache.useValidCacheResults(healthCheckDescriptors, results, resultCacheTtlInMs);

        // everything else is executed in parallel via futures
        List<HealthCheckFuture> futures = createOrReuseFutures(healthCheckDescriptors);

        // wait for futures at most until timeout (but will return earlier if all futures are finsihed)
        waitForFuturesRespectingTimeout(futures);
        collectResultsFromFutures(futures, results);

        healthCheckResultCache.updateWith(results);
    }


    private List<HealthCheckDescriptor> getHealthCheckDescriptors(final String... tags) {
        HealthCheckFilter healthCheckFilter = new HealthCheckFilter(bundleContext);
        final ServiceReference[] healthCheckReferences = healthCheckFilter.getTaggedHealthCheckServiceReferences(tags);
        List<HealthCheckDescriptor> descriptors = new LinkedList<HealthCheckDescriptor>();
        for (ServiceReference serviceReference : healthCheckReferences) {
            HealthCheckDescriptor descriptor = new HealthCheckDescriptor(serviceReference);

            if (ArrayUtils.contains(this.disabledChecks, descriptor.getClassName())) {
                logger.debug("Skipping check {} because it is disabled in OSGi configuration", descriptor);
                continue;
            }

            descriptors.add(descriptor);
        }

        logger.debug("Found {} health check descriptors for tags {}", descriptors.size(), StringUtils.join(tags));
        return descriptors;
    }

    private List<HealthCheckFuture> createOrReuseFutures(final List<HealthCheckDescriptor> healthCheckDescriptors) {
        List<HealthCheckFuture> futuresForResultOfThisCall = new LinkedList<HealthCheckFuture>();

        for (final HealthCheckDescriptor healthCheckDescriptor : healthCheckDescriptors) {

            HealthCheckFuture stillRunningFuture = this.stillRunningFutures.get(healthCheckDescriptor);
            HealthCheckFuture resultFuture;
            if (stillRunningFuture != null && !stillRunningFuture.isDone()) {
                logger.debug("Found a future that is still running for {}", healthCheckDescriptor);
                resultFuture = stillRunningFuture;
            } else {
                logger.debug("Creating future for {}", healthCheckDescriptor);
                resultFuture = new HealthCheckFuture(healthCheckDescriptor, bundleContext);
                this.hcThreadPool.execute(resultFuture);
            }

            futuresForResultOfThisCall.add(resultFuture);

        }
        return futuresForResultOfThisCall;
    }

    private void waitForFuturesRespectingTimeout(List<HealthCheckFuture> futuresForResultOfThisCall) {
        StopWatch callExcutionTimeStopWatch = new StopWatch();
        callExcutionTimeStopWatch.start();
        boolean allFuturesDone;
        do {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                logger.warn("Unexpected InterruptedException while waiting for healthCheckContributors", ie);
            }

            allFuturesDone = true;
            for (HealthCheckFuture healthCheckFuture : futuresForResultOfThisCall) {
                allFuturesDone &= healthCheckFuture.isDone();
            }
        } while (!allFuturesDone && callExcutionTimeStopWatch.getTime() < this.timeoutInMs);
    }

    void collectResultsFromFutures(List<HealthCheckFuture> futuresForResultOfThisCall, Collection<HealthCheckResult> results) {

        Set<ExecutionResult> resultsFromFutures = new HashSet<ExecutionResult>();

        Iterator<HealthCheckFuture> futuresIt = futuresForResultOfThisCall.iterator();
        while (futuresIt.hasNext()) {
            HealthCheckFuture future = futuresIt.next();
            ExecutionResult result;
            if (future.isDone()) {
                logger.debug("Health Check is done: {}", future.getHealthCheckDescriptor());

                try {
                    result = future.get();
                } catch (Exception e) {
                    logger.warn("Unexpected Exception during future.get(): " + e, e);
                    result = new ExecutionResult(future.getHealthCheckDescriptor(), Result.Status.HEALTH_CHECK_ERROR,
                            "Unexpected Exception during future.get(): " + e);
                }

                // if the future came from a previous call remove it from stillRunningFutures
                if (this.stillRunningFutures.containsKey(future.getHealthCheckDescriptor())) {
                    this.stillRunningFutures.remove(future.getHealthCheckDescriptor());
                }

            } else {
                logger.debug("Health Check timed out: {}", future.getHealthCheckDescriptor());
                // Futures must not be cancelled as interrupting a health check might could cause a corrupted repository index
                // (CrxRoundtripCheck) or ugly messages/stack traces in the log file

                this.stillRunningFutures.put(future.getHealthCheckDescriptor(), future);

                // normally we turn the check into WARN (normal timeout), but if the threshold time for CRITICAL is reached for a certain
                // future we turn the result CRITICAL
                long futureElapsedTimeMs = new Date().getTime() - future.getCreatedTime().getTime();
                if (futureElapsedTimeMs < this.longRunningFutureThresholdForRedMs) {
                    result = new ExecutionResult(future.getHealthCheckDescriptor(), Result.Status.WARN,
                            "Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs), futureElapsedTimeMs);

                } else {
                    result = new ExecutionResult(future.getHealthCheckDescriptor(), Result.Status.CRITICAL,
                            "Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs)
                                    + " (exceeding the configured threshold for CRITICAL: "
                                    + msHumanReadable(this.longRunningFutureThresholdForRedMs) + ")", futureElapsedTimeMs);
                }
            }
            resultsFromFutures.add(result);
            futuresIt.remove();
        }

        logger.debug("Adding {} results from futures", resultsFromFutures.size());
        results.addAll(resultsFromFutures);
    }

    static String msHumanReadable(final long millis) {

        double number = millis;
        final String[] units = new String[] { "ms", "sec", "min", "h", "days" };
        final double[] divisors = new double[] { 1000, 60, 60, 24 };

        int magnitude = 0;
        do {
            double currentDivisor = divisors[Math.min(magnitude, divisors.length - 1)];
            if (number < currentDivisor) {
                break;
            }
            number /= currentDivisor;
            magnitude++;
        } while (magnitude < units.length - 1);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.UK);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(1);
        String result = format.format(number) + units[magnitude];
        return result;
    }

    void setTimeoutInMs(final Long timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }

    public Long getTimeoutInMs() {
        return this.timeoutInMs;
    }

    public void setLongRunningFutureThresholdForRedMs(Long longRunningFutureThresholdForRedMs) {
        this.longRunningFutureThresholdForRedMs = longRunningFutureThresholdForRedMs;
    }

    void setExcludedChecks(final String[] excludedChecks) {
        this.disabledChecks = excludedChecks;
    }


}
