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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

/**
 * Implementation of a {@code ObserverConfiguration}
 */
public class BasicObserverConfiguration implements ObserverConfiguration {

    private final boolean includeExternal;

    private final PathSet paths;

    private final PathSet excludedPaths;

    private final Set<String> propertyNamesHint;

    private final Set<ChangeType> changeTypes;

    private final List<ResourceChangeListenerInfo> listeners = new ArrayList<>();

    public BasicObserverConfiguration(final PathSet paths,
            final Set<ChangeType> types,
            final boolean isExternal,
            final PathSet excludePaths,
            final Set<String> propertyNamesHint) {
        this.includeExternal = isExternal;
        this.paths = paths;
        this.changeTypes = Collections.unmodifiableSet(types);
        this.excludedPaths = excludePaths;
        this.propertyNamesHint = propertyNamesHint;
    }

    public BasicObserverConfiguration(final PathSet set) {
        this.includeExternal = false;
        this.paths = set;
        final Set<ChangeType> types = new HashSet<ChangeType>();
        types.add(ChangeType.PROVIDER_ADDED);
        types.add(ChangeType.PROVIDER_REMOVED);
        this.changeTypes = Collections.unmodifiableSet(types);
        this.excludedPaths = PathSet.EMPTY_SET;
        this.propertyNamesHint = null;
    }

    /**
     * Add a listener
     * @param listener The listener
     */
    public void addListener(final ResourceChangeListenerInfo listener) {
        this.listeners.add(listener);
        Collections.sort(this.listeners);
    }

    /**
     * All listeners associated with this configuration
     * @return List of listeners, might be empty
     */
    public List<ResourceChangeListenerInfo> getListeners() {
        return this.listeners;
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

    @Override
    public Set<String> getPropertyNamesHint() {
        return propertyNamesHint;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changeTypes == null) ? 0 : changeTypes.hashCode());
        result = prime * result + ((excludedPaths == null) ? 0 : excludedPaths.hashCode());
        result = prime * result + (includeExternal ? 1231 : 1237);
        result = prime * result + ((paths == null) ? 0 : paths.hashCode());
        result = prime * result + ((propertyNamesHint == null) ? 0 : propertyNamesHint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicObserverConfiguration other = (BasicObserverConfiguration) obj;
        if (changeTypes == null) {
            if (other.changeTypes != null)
                return false;
        } else if (!changeTypes.equals(other.changeTypes))
            return false;
        if (excludedPaths == null) {
            if (other.excludedPaths != null)
                return false;
        } else if (!excludedPaths.equals(other.excludedPaths))
            return false;
        if (includeExternal != other.includeExternal)
            return false;
        if (paths == null) {
            if (other.paths != null)
                return false;
        } else if (!paths.equals(other.paths))
            return false;
        if (propertyNamesHint == null) {
            if (other.propertyNamesHint != null)
                return false;
        } else if (!propertyNamesHint.equals(other.propertyNamesHint))
            return false;
        return true;
    }

    @Override
    public String toString() {
        String excludedPathsToString = String.valueOf(excludedPaths);
        if (excludedPathsToString.length() > 100) {
            excludedPathsToString = excludedPathsToString.substring(0, 99) + "... (" + (excludedPathsToString.length() - 99) + " chars cut)";
        }
        return "BasicObserverConfiguration [includeExternal=" + includeExternal + ", paths=" + paths
                + ", excludedPaths=" + excludedPathsToString + ", propertyNamesHint=" + propertyNamesHint + ", changeTypes="
                + changeTypes + ", listeners=" + listeners + "]";
    }
}
