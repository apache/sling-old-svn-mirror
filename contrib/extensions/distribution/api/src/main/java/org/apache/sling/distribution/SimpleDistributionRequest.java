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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link SimpleDistributionRequest} is a {@link DistributionRequest} where all paths are either "deep" or "shallow".
 */
@ProviderType
public final class SimpleDistributionRequest implements DistributionRequest {


    private final DistributionRequestType requestType;
    private final Set<String> deepPaths;
    private final Map<String, String[]> pathFilters;
    private final String[] paths;

    /**
     * Creates distribution request with "deep" or "shallow" paths.
     * @param requestType the request type
     * @param isDeep is <code>true</code> if all paths are "deep" and is <code>false</code> if all paths are "shallow"
     * @param paths the array of paths to be distributed
     */
    public SimpleDistributionRequest(@Nonnull DistributionRequestType requestType, boolean isDeep, @Nonnull String... paths) {
        this(requestType, paths, isDeep? new HashSet<String>(Arrays.asList(paths)) : new HashSet<String>());
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
     * Creates a distribution request with additional "deep" paths.
     * @param requestType the request type
     * @param paths the array of paths to be distributed
     * @param deepPaths the set of paths that are to be distributed in depth (with all their children)
     */
    public SimpleDistributionRequest(@Nonnull DistributionRequestType requestType, @Nonnull String[] paths, @Nonnull Set<String> deepPaths) {
        this(requestType, paths, deepPaths, new HashMap<String, String[]>());
    }


    /**
     * Creates a distribution request with "deep" paths and filters.
     * @param requestType the request type
     * @param paths the array of paths to be distributed
     * @param deepPaths the set of paths that are to be distributed in depth (with all their children)
     * @param pathFilters the filters applicable for each path
     */
    public SimpleDistributionRequest(@Nonnull DistributionRequestType requestType, @Nonnull String[] paths, @Nonnull Set<String> deepPaths, @Nonnull Map<String, String[]> pathFilters) {
        this.requestType = requestType;
        this.paths = paths;
        this.deepPaths = deepPaths;
        this.pathFilters = pathFilters;
    }

    /**
     * get the {@link DistributionRequestType} associated with this request
     *
     * @return the type of the request as a {@link DistributionRequestType}
     */
    @Nonnull
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
    public boolean isDeep(@Nonnull String path) {
        return deepPaths.contains(path);
    }

    @Nonnull
    public String[] getFilters(String path) {
        String[] filters = pathFilters.get(path);
        return filters != null ? filters : new String[0];
    }

    @Override
    public String toString() {
        return "SimpleDistributionRequest{" +
                "requestType=" + requestType +
                ", paths=" + Arrays.toString(paths) +
                '}';
    }


}
