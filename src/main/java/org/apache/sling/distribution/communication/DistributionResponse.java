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
package org.apache.sling.distribution.communication;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link org.apache.sling.distribution.communication.DistributionResponse} represents the outcome of a
 * {@link org.apache.sling.distribution.communication.DistributionRequest} as handled by a certain {@link org.apache.sling.distribution.agent.DistributionAgent}.
 * Such a response will include the {@link org.apache.sling.distribution.communication.DistributionRequestState state} of
 * the {@link org.apache.sling.distribution.communication.DistributionRequest request} and optionally a message for more
 * verbose information about the outcome of the request.
 */
public class DistributionResponse {

    private final DistributionRequestState state;
    private final String message;

    public DistributionResponse(@Nonnull DistributionRequestState state, @Nullable String message) {
        this.state = state;
        this.message = message;
    }

    public DistributionRequestState getState() {
        return state;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    @Override
    public String toString() {
        return "{\"state\":" + state + ", \"message\":\"" + message + "\"}";
    }

}
