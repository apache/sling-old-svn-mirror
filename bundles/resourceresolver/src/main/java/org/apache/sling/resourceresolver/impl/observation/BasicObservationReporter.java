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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.util.Path;
import org.apache.sling.api.resource.util.PathSet;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

/**
 * Implementation of the observation reporter.
 * Each resource provider gets its on instance.
 */
public class BasicObservationReporter implements ObservationReporter {


    private final List<ObserverConfiguration> configs;

    private final Map<ListenerConfig, List<ResourceChangeListenerInfo>> listeners = new HashMap<BasicObservationReporter.ListenerConfig, List<ResourceChangeListenerInfo>>();;

    /**
     * Create a reporter listening for resource provider changes
     * @param infos The listeners map
     */
    public BasicObservationReporter(final Collection<ResourceChangeListenerInfo> infos) {
        final Set<String> paths = new HashSet<String>();
        for(final ResourceChangeListenerInfo info : infos) {
            if ( !info.getProviderChangeTypes().isEmpty() ) {
                for(final Path p : info.getPaths()) {
                    paths.add(p.getPath());
                }
                fillListeners(info, info.getResourceChangeTypes());
            }
        }
        final ObserverConfiguration cfg = new BasicObserverConfiguration(PathSet.fromStringCollection(paths));
        this.configs = Collections.singletonList(cfg);
    }

    /**
     * Create a reporter listening for a provider
     * @param infos The listeners map
     * @param providerPath The mount point of the provider
     * @param excludePaths Excluded paths for that provider
     */
    public BasicObservationReporter(final Collection<ResourceChangeListenerInfo> infos,
            final Path providerPath, final PathSet excludePaths) {
        final Map<String, ObserverConfig> configMap = new HashMap<String, ObserverConfig>();
        for(final ResourceChangeListenerInfo info : infos) {
            if ( !info.getResourceChangeTypes().isEmpty() ) {
                boolean add = false;
                for(final Path p : info.getPaths()) {
                    if ( providerPath.matches(p.getPath()) && excludePaths.matches(p.getPath()) == null ) {
                        ObserverConfig config = configMap.get(p);
                        if ( config == null ) {
                            config = new ObserverConfig();
                            configMap.put(p.getPath(), config);
                        }
                        config.types.addAll(info.getResourceChangeTypes());
                        if ( info.isExternal() ) {
                            config.isExternal = true;
                        }
                        add = true;
                    }
                }
                if ( add ) {
                    fillListeners(info, info.getResourceChangeTypes());
                }
            }
        }
        final List<ObserverConfiguration> result = new ArrayList<ObserverConfiguration>();
        for(final Map.Entry<String, ObserverConfig> entry : configMap.entrySet()) {
            final ObserverConfiguration cfg = new BasicObserverConfiguration(entry.getKey(), entry.getValue().types,
                    entry.getValue().isExternal, excludePaths);
            result.add(cfg);
        }
        this.configs = Collections.unmodifiableList(result);
    }

    private void fillListeners(final ResourceChangeListenerInfo info, final Set<ChangeType> types) {
        final ListenerConfig cfg = new ListenerConfig(info, types);
        List<ResourceChangeListenerInfo> list = this.listeners.get(cfg);
        if ( list == null ) {
            list = new ArrayList<ResourceChangeListenerInfo>();
            this.listeners.put(cfg, list);
        }
        list.add(info);
    }

    @Override
    public List<ObserverConfiguration> getObserverConfigurations() {
        return configs;
    }

    @Override
    public void reportChanges(final Iterable<ResourceChange> changes, final boolean distribute) {
        final List<ResourceChange> changeList = new ArrayList<ResourceChange>();
        for(final ResourceChange ch : changes) {
            changeList.add(ch);
        }
        for (final Map.Entry<ListenerConfig, List<ResourceChangeListenerInfo>> entry : this.listeners.entrySet()) {
            final List<ResourceChange> filtered = filterChanges(changeList, entry.getKey());
            if ( !filtered.isEmpty() ) {
                for(final ResourceChangeListenerInfo info : entry.getValue()) {
                    info.getListener().onChange(filtered);
                }
            }
        }
        // TODO implement distribute
    }

    /**
     * Filter the change list based on the configuration
     * @param changes The list of changes
     * @param config The configuration
     * @return The filtered list.
     */
    private List<ResourceChange> filterChanges(final List<ResourceChange> changes, final ListenerConfig config) {
        final List<ResourceChange> filtered = new ArrayList<ResourceChange>();
        for (final ResourceChange c : changes) {
            if (matches(c, config)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    /**
     * Match a change against the configuration
     * @param change The change
     * @param config The configuration
     * @return {@code true} whether it matches
     */
    private boolean matches(final ResourceChange change, final ListenerConfig config) {
        if (!config.types.contains(change.getType())) {
            return false;
        }
        if (!config.isExternal && change.isExternal()) {
            return false;
        }
        if (config.paths.matches(change.getPath()) == null ) {
            return false;
        }
        return true;
    }

    private static final class ObserverConfig {
        public final Set<ChangeType> types = new HashSet<ChangeType>();
        public boolean isExternal;
    }

    private static final class ListenerConfig {

        public final PathSet paths;

        public final boolean isExternal;

        public final Set<ChangeType> types;

        public ListenerConfig(final ResourceChangeListenerInfo info, Set<ChangeType> types) {
            this.paths = info.getPaths();
            this.isExternal = info.isExternal();
            this.types = types;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isExternal ? 1231 : 1237);
            result = prime * result + paths.hashCode();
            result = prime * result + types.hashCode();
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
            ListenerConfig other = (ListenerConfig) obj;
            if (isExternal != other.isExternal)
                return false;
            if (!paths.equals(other.paths))
                return false;
            if (!types.equals(other.types))
                return false;
            return true;
        }

    }
}
