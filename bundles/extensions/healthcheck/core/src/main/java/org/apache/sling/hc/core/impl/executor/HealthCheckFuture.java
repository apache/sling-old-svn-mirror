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
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Future to be able to schedule a health check for parallel execution.
 *
 */
class HealthCheckFuture extends FutureTask<ExecutionResult> {

    public interface Callback {
        public void finished(final HealthCheckExecutionResult result);
    }

    private final static Logger LOG = LoggerFactory.getLogger(HealthCheckFuture.class);

    private final HealthCheckMetadata metadata;
    private final Date createdTime;

    HealthCheckFuture(final HealthCheckMetadata metadata, final BundleContext bundleContext, final Callback callback) {
        super(new Callable<ExecutionResult>() {
            @Override
            public ExecutionResult call() throws Exception {
                Thread.currentThread().setName(
                        "Health-Check-" + StringUtils.substringAfterLast(metadata.getTitle(), "."));
                LOG.debug("Starting check {}", metadata);

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Result resultFromHealthCheck = null;
                ExecutionResult executionResult = null;

                final HealthCheck healthCheck = (HealthCheck) bundleContext.getService(metadata.getServiceReference());

                try {
                    if (healthCheck != null) {
                        resultFromHealthCheck = healthCheck.execute();
                    } else {
                        throw new IllegalStateException("Service for " + metadata + " is gone");
                    }

                } catch (final Exception e) {
                    resultFromHealthCheck = new Result(Result.Status.CRITICAL, "Exception during execution of " + this + ": " + e, e);
                } finally {
                    // unget service ref
                    bundleContext.ungetService(metadata.getServiceReference());

                    // update result with information about this run
                    stopWatch.stop();
                    long elapsedTime = stopWatch.getTime();
                    if (resultFromHealthCheck != null) {
                        // wrap the result in an execution result
                        executionResult = new ExecutionResult(metadata, resultFromHealthCheck, elapsedTime);
                    }
                    LOG.debug("Time consumed for {}: {}", metadata, HealthCheckExecutorImpl.msHumanReadable(elapsedTime));
                }

                Thread.currentThread().setName("Health-Check-idle");
                callback.finished(executionResult);
                return executionResult;
            }
        });
        this.createdTime = new Date();
        this.metadata = metadata;

    }

    Date getCreatedTime() {
        return this.createdTime;
    }

    public HealthCheckMetadata getHealthCheckMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "[Future for " + this.metadata + ", createdTime=" + this.createdTime + "]";
    }

}
