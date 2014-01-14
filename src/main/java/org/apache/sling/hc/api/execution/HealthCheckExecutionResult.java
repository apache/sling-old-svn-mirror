package org.apache.sling.hc.api.execution;

import java.util.Date;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.HealthCheckMetadata;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for health check executions via the {@link HealthCheckExecutor}.
 *
 * If the execution of the health check timed out, the method
 *
 */
@ProviderType
public interface HealthCheckExecutionResult {

    /**
     * Get the result of the health check run.
     */
    Result getHealthCheckResult();

    /**
     * Get the elapsed time in ms
     */
    long getElapsedTimeInMs();

    /**
     * Get the date, the health check finished or if the
     * execution timed out, the execution was aborted.
     * @return The finished date of the execution.
     */
    Date getFinishedAt();

    /**
     * Returns true if the execution has timed out. In this
     * case the result does not reflect the real result of the
     * underlying check, but a result indicating the timeout.
     * @return <code>true</code> if execution timed out.
     */
    boolean hasTimedOut();

    /**
     * Get the meta data about the health check service
     */
    HealthCheckMetadata getHealthCheckMetadata();
}