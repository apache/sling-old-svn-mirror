package org.apache.sling.hc.api.execution;

import java.util.Date;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.HealthCheckMetadata;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for health check executions via the {@link HealthCheckExecutor}.
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
     * Get the date, the health check finished.
     */
    Date getFinishedAt();

    /**
     * Get the meta data about the health check service
     */
    HealthCheckMetadata getHealthCheckMetadata();
}