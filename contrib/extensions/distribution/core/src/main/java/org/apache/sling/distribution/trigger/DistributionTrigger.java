/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.trigger;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.common.DistributionException;

/**
 * A {@link DistributionTrigger} is responsible to trigger
 * {@link org.apache.sling.distribution.DistributionRequest}s upon certain 'events' (e.g. Sling / Jcr events,
 * periodic pulling, etc.).
 * A {@link DistributionTrigger} is meant to be stateless so that more than one
 * {@link DistributionRequestHandler} can be registered into the same trigger.
 */
@ConsumerType
public interface DistributionTrigger {

    /**
     * register a request handler to be triggered and returns a corresponding registration id
     *
     * @param requestHandler handler
     * @throws DistributionException if registration fails
     */
    void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException;

    /**
     * unregister the given handler, if existing
     *
     * @param requestHandler handler to unregister
     * @throws DistributionException if any error happen
     */
    void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException;
}
