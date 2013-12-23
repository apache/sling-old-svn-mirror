package org.apache.sling.hc.api;

import java.util.Date;
import java.util.List;

import org.apache.sling.hc.api.Result.Status;

import aQute.bnd.annotation.ProviderType;

/**
 * Interface for health check results.
 *
 * @since 1.1
 */
@ProviderType
public interface HealthCheckResult extends Iterable<ResultLog.Entry> {

    /** True if our status is OK - provides a convenient way of
     *  checking that.
     */
    boolean isOk();

    /** Return our Status */
    Status getStatus();

    long getElapsedTimeInMs();

    Date getFinishedAt();

    String getHealthCheckName();

    List<String> getHealthCheckTags();
}