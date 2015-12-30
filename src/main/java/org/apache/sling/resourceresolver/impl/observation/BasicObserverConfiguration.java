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
package org.apache.sling.resourceresolver.impl.observation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.util.PathSet;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

public class BasicObserverConfiguration implements ObserverConfiguration {

    private final boolean includeExternal;

    private final PathSet paths;

    private final PathSet excludedPaths;

    private final Set<ChangeType> changeTypes;

    public BasicObserverConfiguration(final String path, final Set<ChangeType> types,
            final boolean isExternal, final PathSet excludePaths) {
        this.includeExternal = isExternal;
        this.paths = PathSet.fromStrings(path);
        this.changeTypes = Collections.unmodifiableSet(types);
        this.excludedPaths = excludePaths.getSubset(path);
    }

    public BasicObserverConfiguration(final PathSet set) {
        this.includeExternal = false;
        this.paths = set;
        final Set<ChangeType> types = new HashSet<ChangeType>();
        types.add(ChangeType.PROVIDER_ADDED);
        types.add(ChangeType.PROVIDER_REMOVED);
        this.changeTypes = Collections.unmodifiableSet(types);
        this.excludedPaths = PathSet.EMPTY_SET;
    }

    @Override
    public boolean includeExternal() {
        return includeExternal;
    }

    @Override
    public PathSet getPaths() {
        return paths;
    }

    @Override
    public PathSet getExcludedPaths() {
        return excludedPaths;
    }

    @Override
    public Set<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    @Override
    public boolean matches(final String path) {
        if ( this.paths.matches(path) != null && this.excludedPaths.matches(path) == null ) {
            return true;
        }
        return false;
    }
}
