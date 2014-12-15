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
 * A {@link SimpleDistributionRequest} is a {@link DistributionRequest} where all paths are either "deep" or "shallow".
 */
public final class SimpleDistributionRequest implements DistributionRequest {


    private final DistributionRequestType requestType;

    private final boolean deep;
    private final String[] paths;

    /**
     * Creates distribution request with "deep" or "shallow" paths.
     * @param requestType the request type
     * @param isDeep is <code>true</code> if all paths are "deep" and is <code>false</code> if all paths are "shallow"
     * @param paths the array of paths to be distributed
     */
    public SimpleDistributionRequest(@Nonnull DistributionRequestType requestType, boolean isDeep, @Nonnull String... paths) {
        this.requestType = requestType;
        deep = isDeep;
        this.paths = paths;
    }

    /**
     * Creates a distribution request with "shallow" paths.
     * @param requestType the request type
     * @param paths the array of paths to be distributed
     */
    public SimpleDistributionRequest(@Nonnull DistributionRequestType requestType, @Nonnull String... paths) {
        this(requestType, false, paths);
    }

    /**
     * get the {@link DistributionRequestType} associated with this request
     *
     * @return the type of the request as a {@link DistributionRequestType}
     */
    public DistributionRequestType getRequestType() {
        return requestType;
    }

    /**
     * get the paths for this distribution request
     *
     * @return an array of paths
     */
    public String[] getPaths() {
        return paths;
    }


    /**
     * Returns whether the a path is covering the entire subtree (deep) or just the specified nodes (shallow)
     * @return <code>true</code> if the path is deep
     */
    public boolean isDeep(String path) {
        return deep;
    }

    @Override
    public String toString() {
        return "DistributionRequest{" +
                ", requestType=" + requestType +
                ", paths=" + Arrays.toString(paths) +
                '}';
    }


}
