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
package org.apache.sling.distribution.queue;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.distribution.common.DistributionException;

/**
 * A provider for {@link DistributionQueue}s
 */
@ProviderType
public interface DistributionQueueProvider {

    /**
     * provide an {@link DistributionQueueType#ORDERED} queue with the given name
     *
     * @param queueName the name of the queue to retrieve
     * @return a {@link DistributionQueue}
     */
    @Nonnull
    DistributionQueue getQueue(@Nonnull String queueName) throws DistributionException;

    /**
     * provde a queue of the given type with the given name
     * @param queueName the name of the queue
     * @param type the type of the queue
     * @return a {@link DistributionQueue}
     */
    DistributionQueue getQueue(@Nonnull String queueName, @Nonnull DistributionQueueType type);

    /**
     * enables queue processing
     *
     * @param queueProcessor the queue processor to be used
     */
    void enableQueueProcessing(@Nonnull DistributionQueueProcessor queueProcessor, String... queueNames) throws DistributionException;

    /**
     * disables queue processing
     *
     */
    void disableQueueProcessing() throws DistributionException;
}
