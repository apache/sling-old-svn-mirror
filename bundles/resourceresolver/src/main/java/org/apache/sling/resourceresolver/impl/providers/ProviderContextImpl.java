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
package org.apache.sling.resourceresolver.impl.providers;

import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ProviderContext;

/**
 * Provider context implementation
 */
public class ProviderContextImpl implements ProviderContext {

    private volatile ObservationReporter observationReporter;

    private volatile PathSet excludedPaths;

    public void update(final ObservationReporter observationReporter, PathSet excludedPaths) {
        this.observationReporter = observationReporter;
        this.excludedPaths = excludedPaths;
    }

    @Override
    public ObservationReporter getObservationReporter() {
        return observationReporter;
    }

    @Override
    public PathSet getExcludedPaths() {
        return excludedPaths;
    }
}
