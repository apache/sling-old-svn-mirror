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

import org.apache.sling.hc.api.HealthCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches health check results.
 *
 */
public class HealthCheckResultCache {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<HealthCheckDescriptor, HealthCheckResult> cache = new ConcurrentHashMap<HealthCheckDescriptor, HealthCheckResult>();

    @Override
    public String toString() {
        return "[HealthCheckResultCache size=" + cache.size() + "]";
    }

    public void updateWith(final Collection<HealthCheckResult> results) {
        for (final HealthCheckResult result : results) {
            ExecutionResult executionResult = (ExecutionResult) result;
            cache.put(executionResult.getHealthCheckDescriptor(), executionResult);
        }
    }

    public void useValidCacheResults(List<HealthCheckDescriptor> healthCheckDescriptors,
            final Collection<HealthCheckResult> results,
            long resultCacheTtlInMs) {

        Iterator<HealthCheckDescriptor> checksIt = healthCheckDescriptors.iterator();

        Set<HealthCheckResult> cachedResults = new TreeSet<HealthCheckResult>();
        while (checksIt.hasNext()) {
            HealthCheckDescriptor descriptor = checksIt.next();
            HealthCheckResult result = get(descriptor, resultCacheTtlInMs);
            if (result != null) {
                cachedResults.add(result);
                checksIt.remove();
            }
        }
        logger.debug("Adding {} results from cache", cachedResults.size());
        results.addAll(cachedResults);
    }

    private HealthCheckResult get(HealthCheckDescriptor healthCheckDescriptor, long resultCacheTtlInMs) {
        HealthCheckResult cachedResult = cache.get(healthCheckDescriptor);
        if (cachedResult != null) {
            Date finishedAt = cachedResult.getFinishedAt();
            if (finishedAt == null) {
                // never cache without proper meta data
                return null;
            }

            // Option: add resultCacheTtlInMs as property to health check to make it configurable per check
            Date validUntil = new Date(finishedAt.getTime() + resultCacheTtlInMs);
            Date now = new Date();
            if (validUntil.after(now)) {
                logger.debug("Cache hit: validUntil={} cachedResult={}", validUntil, cachedResult);
                return cachedResult;
            } else {
                logger.trace("Outdated result: validUntil={} cachedResult={}", validUntil, cachedResult);
            }
        }

        // null => no cache hit
        return null;
    }

}