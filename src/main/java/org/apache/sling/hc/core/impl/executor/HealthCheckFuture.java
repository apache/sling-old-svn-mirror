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

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.HealthCheckResult;
import org.apache.sling.hc.api.Result;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Future to be able to schedule a health check for parallel execution.
 * 
 */
class HealthCheckFuture extends FutureTask<ExecutionResult> {
    private final static Logger LOG = LoggerFactory.getLogger(HealthCheckFuture.class);

    private final HealthCheckDescriptor healthCheckDescriptor;
    private final Date createdTime;

    HealthCheckFuture(final HealthCheckDescriptor healthCheckDescriptor, final BundleContext bundleContext) {
        super(new Callable<ExecutionResult>() {
            @Override
            public ExecutionResult call() throws Exception {
                Thread.currentThread().setName(
                        "Health-Check-" + StringUtils.substringAfterLast(healthCheckDescriptor.getClassName(), "."));
                LOG.debug("Starting check {}", healthCheckDescriptor);

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                HealthCheckResult resultFromHealthCheck = null;
                ExecutionResult executionResult = null;

                final HealthCheck healthCheck = (HealthCheck) bundleContext.getService(healthCheckDescriptor.getServiceReference());

                try {
                    if (healthCheck != null) {
                        resultFromHealthCheck = healthCheck.execute();
                    } else {
                        throw new IllegalStateException("Service for " + healthCheckDescriptor + " is gone");
                    }

                } catch (Exception e) {
                    resultFromHealthCheck = new Result(Result.Status.CRITICAL, "Exception during execution of " + this + ": " + e);
                    // TODO ResultLog should be improved to be able to store exceptions
                } finally {
                    // unget service ref
                    bundleContext.ungetService(healthCheckDescriptor.getServiceReference());

                    // update result with information about this run
                    stopWatch.stop();
                    long elapsedTime = stopWatch.getTime();
                    if (resultFromHealthCheck != null) {
                        // wrap the result in an execution result
                        executionResult = new ExecutionResult(healthCheckDescriptor, resultFromHealthCheck, elapsedTime);
                    }
                    LOG.debug("Time consumed for {}: {}", healthCheckDescriptor, HealthCheckExecutorImpl.msHumanReadable(elapsedTime));
                }

                Thread.currentThread().setName("Health-Check-idle");
                return executionResult;
            }
        });
        this.createdTime = new Date();
        this.healthCheckDescriptor = healthCheckDescriptor;

    }

    Date getCreatedTime() {
        return this.createdTime;
    }

    public HealthCheckDescriptor getHealthCheckDescriptor() {
        return healthCheckDescriptor;
    }

    @Override
    public String toString() {
        return "[Future for " + this.healthCheckDescriptor + ", createdTime=" + this.createdTime + "]";
    }

}