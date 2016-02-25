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

import static org.apache.sling.hc.util.FormattingResultLog.msHumanReadable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ModifiableThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs health checks for a given list of tags in parallel.
 *
 */
@Service(value = {HealthCheckExecutor.class, ExtendedHealthCheckExecutor.class})
@Component(label = "Apache Sling Health Check Executor",
        description = "Runs health checks for a given list of tags in parallel.",
        metatype = true, immediate = true) // immediate = true to keep the cache!
public class HealthCheckExecutorImpl implements ExtendedHealthCheckExecutor, ServiceListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long TIMEOUT_DEFAULT_MS = 2000;

    public static final String PROP_TIMEOUT_MS = "timeoutInMs";
    @Property(name = PROP_TIMEOUT_MS, label = "Timeout",
            description = "Timeout in ms until a check is marked as timed out",
            longValue = TIMEOUT_DEFAULT_MS)

    private static final long LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS = 1000 * 60 * 5;

    public static final String PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS = "longRunningFutureThresholdForCriticalMs";
    @Property(name = PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS, label = "Timeout threshold for CRITICAL",
            description = "Threshold in ms until a check is marked as 'exceedingly' timed out and will marked CRITICAL instead of WARN only",
            longValue = LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS)

    private static final long RESULT_CACHE_TTLL_DEFAULT_MS = 1000 * 2;
    public static final String PROP_RESULT_CACHE_TTL_MS = "resultCacheTtlInMs";
    @Property(name = PROP_RESULT_CACHE_TTL_MS, label = "Results Cache TTL in Ms",
            description = "Result Cache time to live - results will be cached for the given time",
            longValue = RESULT_CACHE_TTLL_DEFAULT_MS)


    private long timeoutInMs;

    private long longRunningFutureThresholdForRedMs;

    private long resultCacheTtlInMs;

    private final HealthCheckResultCache healthCheckResultCache = new HealthCheckResultCache();

    private final Map<HealthCheckMetadata, HealthCheckFuture> stillRunningFutures = new HashMap<HealthCheckMetadata, HealthCheckFuture>();

    @Reference
    private AsyncHealthCheckExecutor asyncHealthCheckExecutor;
    
    @Reference
    private ThreadPoolManager threadPoolManager;
    private ThreadPool hcThreadPool;

    private BundleContext bundleContext;

    @Activate
    protected final void activate(final Map<String, Object> properties, final BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        final ModifiableThreadPoolConfig hcThreadPoolConfig = new ModifiableThreadPoolConfig();
        hcThreadPoolConfig.setMaxPoolSize(25);
        hcThreadPool = threadPoolManager.create(hcThreadPoolConfig, "Health Check Thread Pool");

        this.modified(properties);

        try {
            this.bundleContext.addServiceListener(this, "("
                    + Constants.OBJECTCLASS + "=" + HealthCheck.class.getName() + ")");
        } catch (final InvalidSyntaxException ise) {
            // this should really never happen as the expression above is constant
            throw new RuntimeException("Unexpected exception occured.", ise);
        }
    }

    @Modified
    protected final void modified(final Map<String, Object> properties) {
        this.timeoutInMs = PropertiesUtil.toLong(properties.get(PROP_TIMEOUT_MS), TIMEOUT_DEFAULT_MS);

        if ( this.timeoutInMs <= 0L) {
            this.timeoutInMs = TIMEOUT_DEFAULT_MS;
        }

        this.longRunningFutureThresholdForRedMs = PropertiesUtil.toLong(properties.get(PROP_LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_MS),
                LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS);
        if (this.longRunningFutureThresholdForRedMs <= 0L) {
            this.longRunningFutureThresholdForRedMs = LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS;
        }

        this.resultCacheTtlInMs = PropertiesUtil.toLong(properties.get(PROP_RESULT_CACHE_TTL_MS), RESULT_CACHE_TTLL_DEFAULT_MS);
        if (this.resultCacheTtlInMs <= 0L) {
            this.resultCacheTtlInMs = RESULT_CACHE_TTLL_DEFAULT_MS;
        }
    }

    @Deactivate
    protected final void deactivate() {
        threadPoolManager.release(hcThreadPool);
        this.bundleContext.removeServiceListener(this);
        this.bundleContext = null;
        this.healthCheckResultCache.clear();
    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        if ( event.getType() == ServiceEvent.UNREGISTERING ) {
            final Long serviceId = (Long)event.getServiceReference().getProperty(Constants.SERVICE_ID);
            this.healthCheckResultCache.removeCachedResult(serviceId);
        }
    }

    /**
     * @see org.apache.sling.hc.api.execution.HealthCheckExecutor#execute(String[])
     */
    @Override
    public List<HealthCheckExecutionResult> execute(final String... tags) {
        return execute(/*default options*/new HealthCheckExecutionOptions(), tags);
    }

    /**
     * @see org.apache.sling.hc.api.execution.HealthCheckExecutor#execute(HealthCheckExecutionOptions, String...)
     */
    @Override
    public List<HealthCheckExecutionResult> execute(HealthCheckExecutionOptions options, final String... tags) {
        logger.debug("Starting executing checks for tags {} and execution options {}", tags == null ? "*" : tags, options);

        final HealthCheckFilter filter = new HealthCheckFilter(this.bundleContext);
        try {
            final ServiceReference[] healthCheckReferences = filter.getTaggedHealthCheckServiceReferences(options.isCombineTagsWithOr(), tags);

            return this.execute(healthCheckReferences, options);
        } finally {
            filter.dispose();
        }
    }

    /**
     * @see org.apache.sling.hc.core.impl.executor.ExtendedHealthCheckExecutor#execute(org.osgi.framework.ServiceReference)
     */
    @Override
    public HealthCheckExecutionResult execute(final ServiceReference ref) {
        final HealthCheckMetadata metadata = this.getHealthCheckMetadata(ref);
        return createResultsForDescriptor(metadata);
    }

    /**
     * Execute a set of health checks
     */
    private List<HealthCheckExecutionResult> execute(final ServiceReference[] healthCheckReferences, HealthCheckExecutionOptions options) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final List<HealthCheckExecutionResult> results = new ArrayList<HealthCheckExecutionResult>();
        final List<HealthCheckMetadata> healthCheckDescriptors = getHealthCheckMetadata(healthCheckReferences);

        
        createResultsForDescriptors(healthCheckDescriptors, results, options);

        stopWatch.stop();
        if ( logger.isDebugEnabled() ) {
            logger.debug("Time consumed for all checks: {}", msHumanReadable(stopWatch.getTime()));
        }
        // sort result
        Collections.sort(results, new Comparator<HealthCheckExecutionResult>() {

            @Override
            public int compare(final HealthCheckExecutionResult arg0,
                    final HealthCheckExecutionResult arg1) {
                return ((ExecutionResult)arg0).compareTo((ExecutionResult)arg1);
            }

        });
        return results;
    }

    private void createResultsForDescriptors(final List<HealthCheckMetadata> healthCheckDescriptors,
            final Collection<HealthCheckExecutionResult> results, HealthCheckExecutionOptions options) {
        // -- All methods below check if they can transform a healthCheckDescriptor into a result
        // -- if yes the descriptor is removed from the list and the result added

        // get async results
        if (!options.isForceInstantExecution()) {
            asyncHealthCheckExecutor.collectAsyncResults(healthCheckDescriptors, results);
        }
        
        // reuse cached results where possible
        if (!options.isForceInstantExecution()) {
            healthCheckResultCache.useValidCacheResults(healthCheckDescriptors, results, resultCacheTtlInMs);
        }

        // everything else is executed in parallel via futures
        List<HealthCheckFuture> futures = createOrReuseFutures(healthCheckDescriptors);

        // wait for futures at most until timeout (but will return earlier if all futures are finished)
        waitForFuturesRespectingTimeout(futures, options);
        collectResultsFromFutures(futures, results);
    }

    private HealthCheckExecutionResult createResultsForDescriptor(final HealthCheckMetadata metadata) {
        // create result for a single descriptor

        // reuse cached results where possible
        HealthCheckExecutionResult result;

        result = healthCheckResultCache.getValidCacheResult(metadata, resultCacheTtlInMs);

        if ( result == null ) {
            final HealthCheckFuture future;
            synchronized ( this.stillRunningFutures ) {
                future = createOrReuseFuture(metadata);
            }

            // wait for futures at most until timeout (but will return earlier if all futures are finished)
            waitForFuturesRespectingTimeout(Collections.singletonList(future), null);
            result = collectResultFromFuture(future);
        }

        return result;
    }

    /**
     * Create the health check meta data
     */
    private List<HealthCheckMetadata> getHealthCheckMetadata(final ServiceReference... healthCheckReferences) {
        final List<HealthCheckMetadata> descriptors = new LinkedList<HealthCheckMetadata>();
        for (final ServiceReference serviceReference : healthCheckReferences) {
            final HealthCheckMetadata descriptor = getHealthCheckMetadata(serviceReference);

            descriptors.add(descriptor);
        }

        return descriptors;
    }

    /**
     * Create the health check meta data
     */
    private HealthCheckMetadata getHealthCheckMetadata(final ServiceReference healthCheckReference) {
        final HealthCheckMetadata descriptor = new HealthCheckMetadata(healthCheckReference);
        return descriptor;
    }

    /**
     * Create or reuse future for the list of health checks
     */
    private List<HealthCheckFuture> createOrReuseFutures(final List<HealthCheckMetadata> healthCheckDescriptors) {
        final List<HealthCheckFuture> futuresForResultOfThisCall = new LinkedList<HealthCheckFuture>();

        synchronized ( this.stillRunningFutures ) {
            for (final HealthCheckMetadata md : healthCheckDescriptors) {

                futuresForResultOfThisCall.add(createOrReuseFuture(md));

            }
        }
        return futuresForResultOfThisCall;
    }

    /**
     * Create or reuse future for the health check
     * This method must be synchronized by the caller(!) on stillRunningFutures
     */
    private HealthCheckFuture createOrReuseFuture(final HealthCheckMetadata metadata) {
        HealthCheckFuture future = this.stillRunningFutures.get(metadata);
        if (future != null ) {
            logger.debug("Found a future that is still running for {}", metadata);
        } else {
            logger.debug("Creating future for {}", metadata);
            future = new HealthCheckFuture(metadata, bundleContext, new HealthCheckFuture.Callback() {

                @Override
                public void finished(final HealthCheckExecutionResult result) {
                    healthCheckResultCache.updateWith(result);
                    asyncHealthCheckExecutor.updateWith(result);
                    synchronized ( stillRunningFutures ) {
                        stillRunningFutures.remove(metadata);
                    }
                }
            });
            this.stillRunningFutures.put(metadata, future);

            this.hcThreadPool.execute(future);
        }

        return future;
    }

    /**
     * Wait for the futures until the timeout is reached
     */
    private void waitForFuturesRespectingTimeout(final List<HealthCheckFuture> futuresForResultOfThisCall, HealthCheckExecutionOptions options) {
        final StopWatch callExcutionTimeStopWatch = new StopWatch();
        callExcutionTimeStopWatch.start();
        boolean allFuturesDone;

        long effectiveTimeout = this.timeoutInMs;
        if (options != null && options.getOverrideGlobalTimeout() > 0) {
            effectiveTimeout = options.getOverrideGlobalTimeout();
        }

        do {
            try {
                Thread.sleep(50);
            } catch (final InterruptedException ie) {
                logger.warn("Unexpected InterruptedException while waiting for healthCheckContributors", ie);
            }

            allFuturesDone = true;
            for (final HealthCheckFuture healthCheckFuture : futuresForResultOfThisCall) {
                allFuturesDone &= healthCheckFuture.isDone();
            }
        } while (!allFuturesDone && callExcutionTimeStopWatch.getTime() < effectiveTimeout);
    }

    /**
     * Collect the results from all futures
     * @param futuresForResultOfThisCall The list of futures
     * @param results The result collection
     */
    void collectResultsFromFutures(final List<HealthCheckFuture> futuresForResultOfThisCall, final Collection<HealthCheckExecutionResult> results) {

        final Set<HealthCheckExecutionResult> resultsFromFutures = new HashSet<HealthCheckExecutionResult>();

        final Iterator<HealthCheckFuture> futuresIt = futuresForResultOfThisCall.iterator();
        while (futuresIt.hasNext()) {
            final HealthCheckFuture future = futuresIt.next();
            final HealthCheckExecutionResult result = this.collectResultFromFuture(future);

            resultsFromFutures.add(result);
            futuresIt.remove();
        }

        logger.debug("Adding {} results from futures", resultsFromFutures.size());
        results.addAll(resultsFromFutures);
    }

    /**
     * Collect the result from a single future
     * @param future The future
     * @return The execution result or a result for a reached timeout
     */
    HealthCheckExecutionResult collectResultFromFuture(final HealthCheckFuture future) {

        HealthCheckExecutionResult result;
        HealthCheckMetadata hcMetadata = future.getHealthCheckMetadata();
        if (future.isDone()) {
            logger.debug("Health Check is done: {}", hcMetadata);

            try {
                result = future.get();
            } catch (final Exception e) {
                logger.warn("Unexpected Exception during future.get(): " + e, e);
                long futureElapsedTimeMs = new Date().getTime() - future.getCreatedTime().getTime();
                result = new ExecutionResult(hcMetadata, Result.Status.HEALTH_CHECK_ERROR,
                        "Unexpected Exception during future.get(): " + e, futureElapsedTimeMs, false);
            }

        } else {
            logger.debug("Health Check timed out: {}", hcMetadata);
            // Futures must not be cancelled as interrupting a health check might leave the system in invalid state 
            // (worst case could be a corrupted repository index if using write operations)

            // normally we turn the check into WARN (normal timeout), but if the threshold time for CRITICAL is reached for a certain
            // future we turn the result CRITICAL
            long futureElapsedTimeMs = new Date().getTime() - future.getCreatedTime().getTime();
            FormattingResultLog resultLog = new FormattingResultLog();
            if (futureElapsedTimeMs < this.longRunningFutureThresholdForRedMs) {
                resultLog.warn("Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs));
            } else {
                resultLog.critical("Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs)
                        + " (exceeding the configured threshold for CRITICAL: "
                        + msHumanReadable(this.longRunningFutureThresholdForRedMs) + ")");
            }

            // add logs from previous, cached result if exists (using a 1 year TTL)
            HealthCheckExecutionResult lastCachedResult = healthCheckResultCache.getValidCacheResult(hcMetadata, 1000 * 60 * 60 * 24 * 365);
            if (lastCachedResult != null) {
                DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
                resultLog.info("*** Result log of last execution finished at {} after {} ***",
                        df.format(lastCachedResult.getFinishedAt()), FormattingResultLog.msHumanReadable(lastCachedResult.getElapsedTimeInMs()));
                for (ResultLog.Entry entry : lastCachedResult.getHealthCheckResult()) {
                    resultLog.add(entry);
                }
            }

            result = new ExecutionResult(hcMetadata, new Result(resultLog), futureElapsedTimeMs, true);

        }

        return result;
    }


    public void setTimeoutInMs(final long timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }

    public void setLongRunningFutureThresholdForRedMs(
            final long longRunningFutureThresholdForRedMs) {
        this.longRunningFutureThresholdForRedMs = longRunningFutureThresholdForRedMs;
    }
}
