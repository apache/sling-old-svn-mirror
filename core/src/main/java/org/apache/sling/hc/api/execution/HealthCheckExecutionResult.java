package org.apache.sling.hc.api.execution;

import java.util.Date;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.HealthCheckMetaData;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for health check results.
 *
 */
@ProviderType
public interface HealthCheckExecutionResult {

    /**
     * Get the result of the health check run.
     */
    Result getHealthCheckResult();

    long getElapsedTimeInMs();

    Date getFinishedAt();

    /**
     * Get the meta data about the health check service
     */
    HealthCheckMetaData getHealthCheckMetaData();
}