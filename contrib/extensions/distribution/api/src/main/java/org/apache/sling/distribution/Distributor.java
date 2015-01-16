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

package org.apache.sling.distribution;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;

import javax.annotation.Nonnull;

/**
 * A distributor is responsible for dispatching {@link org.apache.sling.distribution.DistributionRequest}s to distribution agents.
 * <p/>
 * The distribution agents are executing the requests by creating packages from a source Sling instance containing content for the specified paths
 * and then pushing and installing these on a target instance.
 */
@ProviderType
public interface Distributor {
    /**
     * Perform a {@link org.apache.sling.distribution.DistributionRequest} to distribute content from a source
     * instance to a target instance.
     * The content to be sent will be assembled according to the information contained in the request.
     * A {@link org.apache.sling.distribution.DistributionResponse} holding the {@link org.apache.sling.distribution.DistributionRequestState}
     * of the provided request will be returned.
     * Synchronous distribution agents will usually block until the execution has finished
     * while asynchronous agents will usually return the response as soon as the content to be distributed has been assembled
     * and scheduled for distribution.
     *
     * @param agentName the name of the agent used to distribute the request
     * @param distributionRequest the distribution request
     * @param resourceResolver    the resource resolver used for authorizing the request,
     * @return a {@link org.apache.sling.distribution.DistributionResponse}
     */
    @Nonnull
    DistributionResponse distribute(@Nonnull String agentName, @Nonnull ResourceResolver resourceResolver,
                                    @Nonnull DistributionRequest distributionRequest);


}
