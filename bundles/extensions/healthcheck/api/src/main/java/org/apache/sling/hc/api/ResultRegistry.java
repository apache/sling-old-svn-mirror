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
package org.apache.sling.hc.api;

import java.util.Calendar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ProviderType;

/**
 * ResultRegistry is a service that can be leveraged to provide health check results.
 * These results can be for a period of time through an expiration, until the JVM is 
 * restarted, or added and later removed.
 * 
 * This can be useful when code observes a specific (possibly bad) state, and wants to 
 * alert through the health check API that this state has taken place.
 * 
 * Some examples: 
 *  An event pool has filled, and some events will be thrown away.
 *   This is a failure case that requires a restart of the instance.
 *   It would be appropriate to trigger a permanent failure.
 *  
 *  A quota has been tripped. This quota may immediately recover, but it is sensible to 
 *   alert for 30 minutes that the quota has been tripped.
 * 
 * If you expect the failure will clear itself within a certain window, 
 * setting the expiration to that window can be ideal.
 *
 */
@ProviderType
public interface ResultRegistry extends HealthCheck {
    /**
     * Put result which will be reported in the health check list until the expiration
     *  has passed, the system is restarted, or the result has been removed by identifier.
     * @param identifier A unique identifier for this health check result. 
     *  This should be "package.class:method" to avoid collisions and be identifiable. 
     *  
     *  Putting a result with the same ID will replace the stored result and expiration 
     *   if the stored entry is of a lower or equal status level.
     *  
     *  If a stored entry is of a higher status level, and the status of the result being put
     *   is of warn or above, the later of the two expirations will be used for the stored entry
     * @param result The object to report until expiration.
     * @param expiration When the system time is after this time, the result is removed from the
     *  list of health checks. If the expiration is null, the result never expires.
     * @param tags a list of tags to apply to this result
     */
    void put(@Nonnull String identifier, @Nonnull Result result, @Nullable Calendar expiration, @Nullable String... tags);
    
    /**
     * removes the health check information
     * @param identifier
     */
    void remove(@Nonnull String identifier);
}
