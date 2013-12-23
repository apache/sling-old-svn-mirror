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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.HealthCheckResult;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;

/** The result of executing a {@link HealthCheck} */
public class ExecutionResult implements Comparable<ExecutionResult>, HealthCheckResult {

    private final HealthCheckResult resultFromHC;

    private final HealthCheckDescriptor healthCheckDescriptor;
    private final Date finishedAt;
    private final long elapsedTimeInMs;

    /** Build a single-value Result
     *  @param s if lower than OK, our status is set to OK */
    ExecutionResult(final HealthCheckDescriptor healthCheckDescriptor, HealthCheckResult simpleResult,
            long elapsedTimeInMs) {
        this.healthCheckDescriptor = healthCheckDescriptor;
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
    ExecutionResult(HealthCheckDescriptor healthCheckDescriptor, Result.Status status, String errorMessage) {
        this(healthCheckDescriptor, new Result(status, errorMessage), 0L);
    }

    /**
     * Shortcut constructor to created error result.
     * 
     * @param healthCheckDescriptor
     * @param status
     * @param errorMessage
     */
    ExecutionResult(HealthCheckDescriptor healthCheckDescriptor, Result.Status status, String errorMessage, long elapsedTime) {
        this(healthCheckDescriptor, new Result(status, errorMessage), elapsedTime);
    }

    public boolean isOk() {
        return resultFromHC.isOk();
    }


    public Result.Status getStatus() {
        return resultFromHC.getStatus();
    }

    @Override
    public Iterator<ResultLog.Entry> iterator() {
        return resultFromHC.iterator();
    }

    @Override
    public String toString() {
        return "ExecutionResult [status=" + getStatus() + ", finishedAt=" + finishedAt + ", elapsedTimeInMs=" + elapsedTimeInMs + "]";
    }

    public long getElapsedTimeInMs() {
        return elapsedTimeInMs;
    }

    HealthCheckDescriptor getHealthCheckDescriptor() {
        return healthCheckDescriptor;
    }

    public String getHealthCheckName() {
        return healthCheckDescriptor != null ? healthCheckDescriptor.getName() : toString();
    }


    @Override
    public List<String> getHealthCheckTags() {
        return healthCheckDescriptor != null ? healthCheckDescriptor.getTags() : new LinkedList<String>();
    }

    public Date getFinishedAt() {
        return finishedAt;
    }


    /**
     * Natural order of results (failed results are sorted before ok results).
     */
    @Override
    public int compareTo(ExecutionResult otherResult) {
        int retVal = otherResult.getStatus().compareTo(this.getStatus());
        if (retVal == 0) {
            retVal = Collator.getInstance().compare(getHealthCheckName(), otherResult.getHealthCheckName());
        }
        return retVal;
    }


}