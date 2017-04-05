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

package org.apache.sling.distribution.impl;

import javax.annotation.Nonnull;

import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;

/**
 * Simple implementation of {@link DistributionResponse} where success is given by not being in FAILED state.
 *
 */
public class SimpleDistributionResponse implements DistributionResponse {

    private final DistributionRequestState state;
    private final String message;

    public SimpleDistributionResponse(DistributionRequestState state, String message) {

        this.state = state;
        this.message = message;
    }

    public boolean isSuccessful() {
        return DistributionRequestState.ACCEPTED.equals(state) || DistributionRequestState.DISTRIBUTED.equals(state);
    }

    @Nonnull
    public DistributionRequestState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CompositeDistributionResponse{" +
                "isSuccesful=" + isSuccessful() +
                ", state=" + state +
                ", message=" + message +
                '}';
    }

}
