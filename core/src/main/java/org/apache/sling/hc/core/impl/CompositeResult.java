package org.apache.sling.hc.core.impl;

import java.util.List;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.ResultLog.Entry;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.core.impl.executor.HealthCheckExecutorImpl;
import org.apache.sling.hc.util.HealthCheckMetadata;

public class CompositeResult extends Result {

    public CompositeResult(ResultLog log, List<HealthCheckExecutionResult> executionResults) {
        super(log);

        for (HealthCheckExecutionResult executionResult : executionResults) {
            HealthCheckMetadata healthCheckMetadata = executionResult.getHealthCheckMetadata();
            Result healthCheckResult = executionResult.getHealthCheckResult();
            for (Entry entry : healthCheckResult) {
                resultLog.add(new ResultLog.Entry(entry.getStatus(), healthCheckMetadata.getName() + ": " + entry.getMessage(), entry.getException()));
            }
            resultLog.add(new ResultLog.Entry(Result.Status.DEBUG, healthCheckMetadata.getName() + " finished after "
                    + HealthCheckExecutorImpl.msHumanReadable(executionResult.getElapsedTimeInMs())
                    + (executionResult.hasTimedOut() ? " (timed out)" : "")));
        }
    }

}
