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
package org.apache.sling.distribution.agent;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionResponse;
import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.queue.DistributionQueue;

/**
 * A distribution agent is responsible for handling {@link org.apache.sling.distribution.communication.DistributionRequest}s.
 * <p/>
 * This means executing actions of e.g.: a specific {@link org.apache.sling.distribution.communication.DistributionActionType}s on
 * specific path(s) which will resume pulling resources from a certain Sling instance and / or pushing resources to
 * other instances.
 */
@ProviderType
public interface DistributionAgent extends DistributionComponent {


    /**
     * retrieves the names of the queues for this agent.
     * @return the list of queue names
     */
    @Nonnull
    Iterable<String> getQueueNames();

    /**
     * get the agent queue with the given name
     *
     * @param name a queue name
     * @return a {@link org.apache.sling.distribution.queue.DistributionQueue} with the given name bound to this agent, if it exists, <code>null</code> otherwise
     * @throws DistributionAgentException if an error occurs in retrieving the queue
     */
    @CheckForNull
    DistributionQueue getQueue(@Nonnull String name) throws DistributionAgentException;

    /**
     * executes a {@link org.apache.sling.distribution.communication.DistributionRequest}
     *
     * @param distributionRequest the distribution request
     * @param resourceResolver   the resource resolver used for authenticating the request,
     * @return a {@link org.apache.sling.distribution.communication.DistributionResponse}
     * @throws DistributionAgentException if any error happens during the execution of the request or if the authentication fails
     */
    @Nonnull
    DistributionResponse execute(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionAgentException;

}
