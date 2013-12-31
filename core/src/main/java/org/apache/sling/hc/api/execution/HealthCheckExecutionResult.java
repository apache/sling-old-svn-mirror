package org.apache.sling.hc.api.execution;

import java.util.Date;
import java.util.List;

import org.apache.sling.hc.api.Result;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for health check results.
 *
 */
@ProviderType
public interface HealthCheckExecutionResult {

    Result getHealthCheckResult();

    long getElapsedTimeInMs();

    Date getFinishedAt();

    String getHealthCheckName();

    List<String> getHealthCheckTags();
}