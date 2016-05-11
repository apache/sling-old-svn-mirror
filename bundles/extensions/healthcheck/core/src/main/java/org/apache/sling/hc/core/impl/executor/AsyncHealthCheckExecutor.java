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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.core.impl.executor.HealthCheckFuture.Callback;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs health checks that are configured with a cron expression for asynchronous
 * execution. Used by HealthCheckExecutor.
 *
 */
@Service({ AsyncHealthCheckExecutor.class })
@Component(immediate = true)
public class AsyncHealthCheckExecutor implements ServiceListener {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncHealthCheckExecutor.class);

    @Reference
    private Scheduler scheduler;

    private Map<HealthCheckMetadata, ExecutionResult> asyncResultsByDescriptor = new ConcurrentHashMap<HealthCheckMetadata, ExecutionResult>();

    private Map<HealthCheckMetadata, HealthCheckAsyncJob> registeredJobs = new HashMap<HealthCheckMetadata, HealthCheckAsyncJob>();

    private BundleContext bundleContext;



    @Activate
    protected final void activate(final ComponentContext componentContext) {
        this.bundleContext = componentContext.getBundleContext();
        this.bundleContext.addServiceListener(this);

        int count = 0;
        HealthCheckFilter healthCheckFilter = new HealthCheckFilter(bundleContext);
        final ServiceReference[] healthCheckReferences = healthCheckFilter.getTaggedHealthCheckServiceReferences(new String[0]);
        for (ServiceReference serviceReference : healthCheckReferences) {
            HealthCheckMetadata healthCheckMetadata = new HealthCheckMetadata(serviceReference);
            if (isAsync(healthCheckMetadata)) {
                if (scheduleHealthCheck(healthCheckMetadata)) {
                    count++;
                }
            }
        }
        LOG.debug("Scheduled {} jobs for asynchronous health checks", count);
    }

    @Deactivate
    protected final void deactivate(final ComponentContext componentContext) {
        this.bundleContext.removeServiceListener(this);
        this.bundleContext = null;

        LOG.debug("Unscheduling {} jobs for asynchronous health checks", registeredJobs.size());
        for (HealthCheckMetadata healthCheckDescriptor : new LinkedList<HealthCheckMetadata>(registeredJobs.keySet())) {
            unscheduleHealthCheck(healthCheckDescriptor);
        }

    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        if(bundleContext == null) {
            // already deactivated?
            return;
        }
        ServiceReference serviceReference = event.getServiceReference();
        final boolean isHealthCheck = serviceReference.isAssignableTo(bundleContext.getBundle(), HealthCheck.class.getName());

        if (isHealthCheck) {
            HealthCheckMetadata healthCheckMetadata = new HealthCheckMetadata(serviceReference);
            int eventType = event.getType();
            LOG.debug("Received service event of type {} for health check {}", eventType, healthCheckMetadata);
            if (eventType == ServiceEvent.REGISTERED) {
                scheduleHealthCheck(healthCheckMetadata);
            } else if (eventType == ServiceEvent.UNREGISTERING) {
                unscheduleHealthCheck(healthCheckMetadata);
            } else if (eventType == ServiceEvent.MODIFIED) {
                unscheduleHealthCheck(healthCheckMetadata);
                scheduleHealthCheck(healthCheckMetadata);
            }

        }
    }

    private boolean scheduleHealthCheck(HealthCheckMetadata descriptor) {

        if(!isAsync(descriptor)) {
            return false;
        }

        try {
            HealthCheckAsyncJob healthCheckAsyncJob = new HealthCheckAsyncJob(descriptor);
            LOG.debug("Scheduling job {} with cron expression {}", healthCheckAsyncJob, descriptor.getAsyncCronExpression());
            final boolean concurrent = false;
            this.scheduler.addJob(healthCheckAsyncJob.getJobId(), healthCheckAsyncJob, null, descriptor.getAsyncCronExpression(), concurrent);
            registeredJobs.put(descriptor, healthCheckAsyncJob);
            return true;
        } catch (Exception e) {
            LOG.warn("Could not schedule job for " + descriptor + ". Exeception: " + e, e);
            return false;
        }

    }

    private boolean unscheduleHealthCheck(HealthCheckMetadata descriptor) {

        // here no check for isAsync must be used to ensure previously
        // scheduled async checks are correctly unscheduled if they have
        // changed from async to sync.

        HealthCheckAsyncJob job = registeredJobs.remove(descriptor);
        try {
            if (job != null) {
                LOG.debug("Unscheduling job {} with cron expression '{}'", job, descriptor.getAsyncCronExpression());
                this.scheduler.removeJob(job.getJobId());
                return true;
            }
        } catch (Exception e) {
            LOG.warn("Could not unschedule job " + job + ". Exeception: " + e, e);
        }
        return false;

    }

    void collectAsyncResults(List<HealthCheckMetadata> healthCheckDescriptors, Collection<HealthCheckExecutionResult> results) {
        Iterator<HealthCheckMetadata> checksIt = healthCheckDescriptors.iterator();

        Set<ExecutionResult> asyncResults = new TreeSet<ExecutionResult>();
        while (checksIt.hasNext()) {
            HealthCheckMetadata healthCheckMetadata = checksIt.next();
            if (isAsync(healthCheckMetadata)) {
                ExecutionResult result = asyncResultsByDescriptor.get(healthCheckMetadata);
                if (result == null) {

                    result = new ExecutionResult(healthCheckMetadata, new Result(Result.Status.INFO, "Async Health Check with cron expression '"
                            + healthCheckMetadata.getAsyncCronExpression() + "' has not yet been executed."), 0L);

                    asyncResults.add(result);
                }
                asyncResults.add(result);
                // remove from HC collection to not execute the check in HealthCheckExecutorImpl
                checksIt.remove();
            }
        }
        LOG.debug("Adding {} results from async results", asyncResults.size());
        results.addAll(asyncResults);

    }

    void updateWith(HealthCheckExecutionResult result) {
        if (isAsync(result.getHealthCheckMetadata())) {
            asyncResultsByDescriptor.put(result.getHealthCheckMetadata(), (ExecutionResult) result);
            LOG.debug("Updated result for async hc {} with {}", result.getHealthCheckMetadata(), result);
        }
    }

    private boolean isAsync(HealthCheckMetadata healthCheckMetadata) {
        return StringUtils.isNotBlank(healthCheckMetadata.getAsyncCronExpression());
    }

    private class HealthCheckAsyncJob implements Runnable {

        private final HealthCheckMetadata healthCheckDescriptor;

        public HealthCheckAsyncJob(HealthCheckMetadata healthCheckDescriptor) {
            super();
            this.healthCheckDescriptor = healthCheckDescriptor;
        }

        public String getJobId() {
            String jobId = "job-hc-" + healthCheckDescriptor.getServiceId();
            return jobId;
        }

        @Override
        public void run() {

            LOG.debug("Running job {}", this);
            HealthCheckFuture healthCheckFuture = new HealthCheckFuture(healthCheckDescriptor, bundleContext, new Callback() {

                @Override
                public void finished(HealthCheckExecutionResult result) {
                    updateWith(result);
                }});

            // run future in same thread (as we are already async via scheduler)
            healthCheckFuture.run();

        }

        @Override
        public String toString() {
            return "[Async job for " + this.healthCheckDescriptor + "]";
        }

    }

}
