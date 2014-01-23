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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches health check results.
 */
public class HealthCheckResultCache {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The map holding the cached results.
     */
    private final Map<Long, HealthCheckExecutionResult> cache = new ConcurrentHashMap<Long, HealthCheckExecutionResult>();

    /**
     * Update the cache with the result
     */
    public void updateWith(HealthCheckExecutionResult result) {
        final ExecutionResult executionResult = (ExecutionResult) result;
        cache.put(executionResult.getServiceId(), result);
    }

    /**
     * Get the valid cache results
     */
    public void useValidCacheResults(final List<HealthCheckMetadata> metadatas,
            final Collection<HealthCheckExecutionResult> results,
            final long resultCacheTtlInMs) {
        final Set<HealthCheckExecutionResult> cachedResults = new TreeSet<HealthCheckExecutionResult>();
        final Iterator<HealthCheckMetadata> checksIt = metadatas.iterator();
        while (checksIt.hasNext()) {
            final HealthCheckMetadata md = checksIt.next();
            final HealthCheckExecutionResult result = useValidCacheResults(md, resultCacheTtlInMs);
            if (result != null) {
                cachedResults.add(result);
                checksIt.remove();
            }
        }
        logger.debug("Adding {} results from cache", cachedResults.size());
        results.addAll(cachedResults);
    }

    /**
     * Return the cached result if it's still valid.
     */
    public HealthCheckExecutionResult useValidCacheResults(final HealthCheckMetadata metadata,
            final long resultCacheTtlInMs) {
        return get(metadata, resultCacheTtlInMs);
    }

    private HealthCheckExecutionResult get(final HealthCheckMetadata metadata, final long resultCacheTtlInMs) {
        final Long key = metadata.getServiceId();
        final HealthCheckExecutionResult cachedResult = cache.get(key);
        if (cachedResult != null) {
            Date finishedAt = cachedResult.getFinishedAt();
            if (finishedAt == null) {
                // never cache without proper meta data -> remove it
                cache.remove(key);
                return null;
            }

            // Option: add resultCacheTtlInMs as property to health check to make it configurable per check
            Date validUntil = new Date(finishedAt.getTime() + resultCacheTtlInMs);
            Date now = new Date();
            if (validUntil.after(now)) {
                logger.debug("Cache hit: validUntil={} cachedResult={}", validUntil, cachedResult);
                return cachedResult;
            } else {
                logger.debug("Outdated result: validUntil={} cachedResult={}", validUntil, cachedResult);
                cache.remove(key);
            }
        }

        // null => no cache hit
        return null;
    }

    /**
     * Clear the whole cache
     */
    public void clear() {
        this.cache.clear();
    }

    /**
     * Remove entry from cache
     */
    public void removeCachedResult(final Long serviceId) {
        this.cache.remove(serviceId);
    }

    @Override
    public String toString() {
        return "[HealthCheckResultCache size=" + cache.size() + "]";
    }

}
