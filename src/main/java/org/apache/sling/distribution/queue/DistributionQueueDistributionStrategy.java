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
import java.util.List;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.packaging.DistributionPackage;

/**
 * a {@link DistributionQueueDistributionStrategy} implements an algorithm for the distribution of
 * {@link org.apache.sling.distribution.packaging.DistributionPackage}s among the available queues
 */
@ConsumerType
public interface DistributionQueueDistributionStrategy extends DistributionComponent {
    String DEFAULT_QUEUE_NAME = "default";

    /**
     * synchronously distribute a {@link org.apache.sling.distribution.packaging.DistributionPackage}
     * to one or more {@link DistributionQueue}s provided by the given {@link DistributionQueueProvider}
     *
     * @param distributionPackage          a {@link org.apache.sling.distribution.packaging.DistributionPackage} to distribute
     * @param queueProvider the {@link DistributionQueueProvider} used to provide the queues to be used for the given package
     * @return <code>true</code> if addition was successful, <code>false</code> otherwise
     * @throws DistributionQueueException if any internal error happens during distribution
     */
    boolean add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionQueueException;


    /**
     * Returns the queue names available for this strategy.
     * @return a list of queue names
     */
    @Nonnull
    List<String> getQueueNames();

}
