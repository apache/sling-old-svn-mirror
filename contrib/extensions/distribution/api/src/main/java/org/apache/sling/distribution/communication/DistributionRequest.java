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
import java.util.Arrays;

/**
 * A {@link org.apache.sling.distribution.communication.DistributionRequest} represents the need from the caller to have
 * some content being distributed from a source instance to a target instance.
 */
public final class DistributionRequest {

    private final long time;

    private final DistributionRequestType actionType;

    private final String[] paths;

    public DistributionRequest(@Nonnull DistributionRequestType actionType, @Nonnull String... paths) {
        this.time = System.currentTimeMillis();
        this.actionType = actionType;
        this.paths = paths;
    }

    /**
     * get the time this distribution request was created as a {@code long} returned by {@code System#currentTimeMillis}.
     *
     * @return the distribution request creation time as returned by {@code System#currentTimeMillis}
     */
    public long getTime() {
        return time;
    }

    /**
     * get the {@link DistributionRequestType} associated with this request
     *
     * @return the type of actionType for request as a {@link DistributionRequestType}
     */
    public DistributionRequestType getRequestType() {
        return actionType;
    }

    /**
     * get the paths for this distribution request
     *
     * @return an array of paths
     */
    public String[] getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return "DistributionRequest{" +
                "time=" + time +
                ", actionType=" + actionType +
                ", paths=" + Arrays.toString(paths) +
                '}';
    }
}
