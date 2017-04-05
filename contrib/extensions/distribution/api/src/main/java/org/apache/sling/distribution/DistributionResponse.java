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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * A {@link org.apache.sling.distribution.DistributionResponse} represents the outcome of a
 * {@link org.apache.sling.distribution.DistributionRequest} as handled by a certain distribution agent.
 * Such a response will include the {@link org.apache.sling.distribution.DistributionRequestState state} of
 * the {@link org.apache.sling.distribution.DistributionRequest request} and optionally a message for more
 * verbose information about the outcome of the request.
 */
@ProviderType
public interface DistributionResponse {

    /**
     * returns the status of the request, whether it is successful or not.
     * A successful request it is not necessarily distributed, it is just successfully received by the agent.
     * To check the exact state of the request one can retrieve it with <code>getState</code>
     *
     * @return <code>true</code> if request has been accepted by the agent.
     */
    boolean isSuccessful();

    /**
     * returns the state of the associated {@link DistributionRequest}
     *
     * @return the state of the associated request
     */
    @Nonnull
    DistributionRequestState getState();

    /**
     * returns a verbose message of the response
     * @return a message associated with this response holding information about
     * e.g. why distribution execution failed, etc.
     */
    @CheckForNull
    String getMessage();
}
