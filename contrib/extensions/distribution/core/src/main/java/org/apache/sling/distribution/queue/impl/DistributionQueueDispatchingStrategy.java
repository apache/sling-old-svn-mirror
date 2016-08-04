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
package org.apache.sling.distribution.queue.impl;

import javax.annotation.Nonnull;
import java.util.List;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;

/**
 * a {@link DistributionQueueDispatchingStrategy} implements an algorithm for dispatching
 * {@link DistributionPackage}s among the available queues.
 * <p/>
 * Usually a {@link DistributionPackage} will be dispatched to a single {@link org.apache.sling.distribution.queue.DistributionQueue}
 * but it would also be possible to dispatch the same package to multiple queues, resulting in obtaining multiple states
 * (one for each queue) for a certain package.
 */
@ConsumerType
public interface DistributionQueueDispatchingStrategy {
    String DEFAULT_QUEUE_NAME = "default";

    /**
     * synchronously distribute a {@link DistributionPackage}
     * to one or more {@link org.apache.sling.distribution.queue.DistributionQueue}s provided by the given {@link org.apache.sling.distribution.queue.DistributionQueueProvider}
     *
     * @param distributionPackage a {@link DistributionPackage} to distribute
     * @param queueProvider       the {@link org.apache.sling.distribution.queue.DistributionQueueProvider} used to provide the queues to be used for the given package
     * @return an {@link java.lang.Iterable} of {@link org.apache.sling.distribution.queue.DistributionQueueItemStatus}s representing
     * the states of the {@link org.apache.sling.distribution.queue.DistributionQueueItem}s added to one or more {@link org.apache.sling.distribution.queue.DistributionQueue}s
     * @throws DistributionException if any internal error happens during distribution
     */
    Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionException;

    /**
     * Returns the queue names available for this strategy.
     *
     * @return a list of queue names
     */
    @Nonnull
    List<String> getQueueNames();

}
