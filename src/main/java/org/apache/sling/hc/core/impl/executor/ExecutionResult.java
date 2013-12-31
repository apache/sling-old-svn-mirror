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

import java.text.Collator;
import java.util.Date;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.util.HealthCheckMetaData;

/** The result of executing a {@link HealthCheck} */
public class ExecutionResult implements Comparable<ExecutionResult>, HealthCheckExecutionResult {

    private final Result resultFromHC;

    private final HealthCheckMetaData metaData;

    private final Date finishedAt;
    private final long elapsedTimeInMs;

    /** Build a single-value Result
     *  @param s if lower than OK, our status is set to OK */
    ExecutionResult(final HealthCheckMetaData healthCheckDescriptor, Result simpleResult,
            long elapsedTimeInMs) {
        this.metaData = healthCheckDescriptor;
        this.resultFromHC = simpleResult;
        this.finishedAt = new Date();
        this.elapsedTimeInMs = elapsedTimeInMs;
    }

    /**
     * Shortcut constructor to created error result.
     *
     * @param healthCheckDescriptor
     * @param status
     * @param errorMessage
     */
    ExecutionResult(HealthCheckMetaData healthCheckDescriptor, Result.Status status, String errorMessage) {
        this(healthCheckDescriptor, new Result(status, errorMessage), 0L);
    }

    /**
     * Shortcut constructor to created error result.
     *
     * @param healthCheckDescriptor
     * @param status
     * @param errorMessage
     */
    ExecutionResult(HealthCheckMetaData healthCheckDescriptor, Result.Status status, String errorMessage, long elapsedTime) {
        this(healthCheckDescriptor, new Result(status, errorMessage), elapsedTime);
    }


    @Override
    public Result getHealthCheckResult() {
        return this.resultFromHC;
    }

    @Override
    public String toString() {
        return "ExecutionResult [status=" + this.resultFromHC.getStatus() + ", finishedAt=" + finishedAt + ", elapsedTimeInMs=" + elapsedTimeInMs + "]";
    }

    @Override
    public long getElapsedTimeInMs() {
        return elapsedTimeInMs;
    }

    @Override
    public HealthCheckMetaData getHealthCheckMetaData() {
        return this.metaData;
    }

    @Override
    public Date getFinishedAt() {
        return finishedAt;
    }

    /**
     * Natural order of results (failed results are sorted before ok results).
     */
    @Override
    public int compareTo(ExecutionResult otherResult) {
        int retVal = otherResult.getHealthCheckResult().getStatus().compareTo(this.getHealthCheckResult().getStatus());
        if (retVal == 0) {
            retVal = Collator.getInstance().compare(this.getHealthCheckMetaData().getTitle(), otherResult.getHealthCheckMetaData().getTitle());
        }
        return retVal;
    }

    long getServiceId() {
        return this.metaData.getServiceId();
    }
}