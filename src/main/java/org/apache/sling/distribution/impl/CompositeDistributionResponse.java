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
import java.util.List;

import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;

/**
 * A composite {@link SimpleDistributionResponse}.
 */
public class CompositeDistributionResponse extends SimpleDistributionResponse {

    private final int packagesCount;
    private final int queuesCount;
    private final long exportTime;

    private DistributionRequestState state;

    private String message;

    public CompositeDistributionResponse(List<DistributionResponse> distributionResponses, int packagesCount, long exportTime) {
        super(DistributionRequestState.DISTRIBUTED, null);
        this.packagesCount = packagesCount;
        this.queuesCount = distributionResponses.size();
        this.exportTime = exportTime;
        if (distributionResponses.isEmpty()) {
            state = DistributionRequestState.DROPPED;
        } else {
            state = DistributionRequestState.DISTRIBUTED;
            StringBuilder messageBuilder = new StringBuilder("[");
            for (DistributionResponse response : distributionResponses) {
                state = aggregatedState(state, response.getState());
                messageBuilder.append(response.getMessage()).append(", ");
            }
            int lof = messageBuilder.lastIndexOf(", ");
            messageBuilder.replace(lof, lof + 2, "]");
            message = messageBuilder.toString();
        }
    }

    @Override
    public boolean isSuccessful() {
        return DistributionRequestState.ACCEPTED.equals(state) || DistributionRequestState.DISTRIBUTED.equals(state);
    }

    @Nonnull
    @Override
    public DistributionRequestState getState() {
        return state;
    }

    @Override
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


    /* Provide the aggregated state of two {@link org.apache.sling.distribution.DistributionRequestState}s */
    private DistributionRequestState aggregatedState(DistributionRequestState first, DistributionRequestState second) {
        DistributionRequestState aggregatedState;
        switch (second) {
            case DISTRIBUTED:
                aggregatedState = first;
                break;
            case ACCEPTED:
                if (first.equals(DistributionRequestState.DISTRIBUTED)) {
                    aggregatedState = DistributionRequestState.ACCEPTED;
                } else {
                    aggregatedState = first;
                }
                break;
            default:
                aggregatedState = DistributionRequestState.DROPPED;
        }
        return aggregatedState;
    }

    public int getPackagesCount() {
        return packagesCount;
    }

    public long getExportTime() {
        return exportTime;
    }

    public int getQueuesCount() {
        return queuesCount;
    }
}
