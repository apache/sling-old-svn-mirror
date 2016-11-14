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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the observation reporter.
 * Each resource provider gets its on instance.
 */
public class BasicObservationReporter implements ObservationReporter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /** List of observer configurations for the provider. */
    private final List<ObserverConfiguration> configs;

    /** The search path. */
    private final String[] searchPath;

    /**
     * Create a reporter listening for resource provider changes
     *
     * @param searchPath The search path
     * @param infos The listeners map
     */
    public BasicObservationReporter(
            final String[] searchPath,
            final Collection<ResourceChangeListenerInfo> infos) {
        this.searchPath = searchPath;
        final Set<String> paths = new HashSet<String>();
        final List<ResourceChangeListenerInfo> result = new ArrayList<>();
        for(final ResourceChangeListenerInfo info : infos) {
            if ( !info.getProviderChangeTypes().isEmpty() ) {
                for(final Path p : info.getPaths()) {
                    paths.add(p.getPath());
                }
                result.add(info);
            }
        }
        final BasicObserverConfiguration cfg = new BasicObserverConfiguration(PathSet.fromStringCollection(paths));
        for(final ResourceChangeListenerInfo i : infos) {
            cfg.addListener(i);
        }
        this.configs = Collections.singletonList((ObserverConfiguration)cfg);
    }

    /**
     * Create a reporter listening for a provider
     *
     * @param searchPath The search paths
     * @param infos The listeners map
     * @param providerPath The mount point of the provider
     * @param excludePaths Excluded paths for that provider
     */
    public BasicObservationReporter(
            final String[] searchPath,
            final Collection<ResourceChangeListenerInfo> infos,
            final Path providerPath,
            final PathSet excludePaths) {
        this.searchPath = searchPath;

        final List<ObserverConfiguration> observerConfigs = new ArrayList<>();
        for(final ResourceChangeListenerInfo info : infos) {
            if ( !info.getResourceChangeTypes().isEmpty() ) {
                // find the set of paths that match the provider
                final Set<Path> paths = new HashSet<>();
                for(final Path p : info.getPaths()) {
                    boolean add = providerPath.matches(p.getPath());
                    if ( add ) {
                        if ( p.isPattern() ) {
                            for(final Path exclude : excludePaths) {
                                if ( p.getPath().startsWith(Path.GLOB_PREFIX + exclude.getPath() + "/")) {
                                    logger.debug("ResourceChangeListener {} is shadowed by {}", info, exclude);
                                    add = false;
                                    break;
                                }
                            }
                        } else {
                            final Path exclude = excludePaths.matches(p.getPath());
                            if ( exclude != null ) {
                                logger.debug("ResourceChangeListener {} is shadowed by {}", info, exclude);
                                add = false;
                            }
                        }
                    }
                    if ( add ) {
                        paths.add(p);
                    }
                }
                if ( !paths.isEmpty() ) {
                    final PathSet pathSet = PathSet.fromPathCollection(paths);
                    // search for an existing configuration with the same paths and hints
                    BasicObserverConfiguration found = null;
                    for(final ObserverConfiguration c : observerConfigs) {
                        if ( c.getPaths().equals(pathSet)
                            && (( c.getPropertyNamesHint() == null && info.getPropertyNamesHint() == null)
                               || c.getPropertyNamesHint() != null && c.getPropertyNamesHint().equals(info.getPropertyNamesHint()))) {
                            found = (BasicObserverConfiguration)c;
                            break;
                        }
                    }
                    final BasicObserverConfiguration config;
                    if ( found != null ) {
                        // check external and types
                        boolean createNew = false;
                        if ( !found.includeExternal() && info.isExternal() ) {
                            createNew = true;
                        }
                        if ( !found.getChangeTypes().equals(info.getResourceChangeTypes()) ) {
                            createNew = true;
                        }
                        if ( createNew ) {
                            // create new/updated config
                            observerConfigs.remove(found);
                            final Set<ResourceChange.ChangeType> types = new HashSet<>();
                            types.addAll(found.getChangeTypes());
                            types.addAll(info.getResourceChangeTypes());
                            config = new BasicObserverConfiguration(pathSet,
                                types,
                                info.isExternal() || found.includeExternal(),
                                found.getExcludedPaths(),
                                found.getPropertyNamesHint());
                            observerConfigs.add(config);
                            for(final ResourceChangeListenerInfo i : found.getListeners()) {
                                config.addListener(i);
                            }

                        } else {
                            config = found;
                        }
                    } else {
                        // create new config
                        config = new BasicObserverConfiguration(pathSet,
                            info.getResourceChangeTypes(),
                            info.isExternal(),
                            excludePaths.getSubset(pathSet),
                            info.getPropertyNamesHint());
                        observerConfigs.add(config);
                    }
                    config.addListener(info);
                }
            }
        }
        this.configs = Collections.unmodifiableList(observerConfigs);
    }

    @Override
    public List<ObserverConfiguration> getObserverConfigurations() {
        return configs;
    }

    @Override
    public void reportChanges(final Iterable<ResourceChange> changes, final boolean distribute) {
        for(final ObserverConfiguration cfg : this.configs) {
            final List<ResourceChange> filteredChanges = filterChanges(changes, cfg);
            if (!filteredChanges.isEmpty() ) {
                this.reportChanges(cfg, filteredChanges, distribute);
            }
        }
    }

    @Override
    public void reportChanges(final ObserverConfiguration config,
            final Iterable<ResourceChange> changes,
            final boolean distribute) {
        if ( config != null && config instanceof BasicObserverConfiguration ) {
            final BasicObserverConfiguration observerConfig = (BasicObserverConfiguration)config;

            ResourceChangeListenerInfo previousInfo = null;
            List<ResourceChange> filteredChanges = null;
            for(final ResourceChangeListenerInfo info : observerConfig.getListeners()) {
                if ( previousInfo == null || !equals(previousInfo, info) ) {
                    filteredChanges = filterChanges(changes, info);
                    previousInfo = info;
                }
                if ( !filteredChanges.isEmpty() ) {
                    final ResourceChangeListener listener = info.getListener();
                    if ( listener != null ) {
                        listener.onChange(filteredChanges);
                    }
                }
            }
            // TODO implement distribute
            if ( distribute ) {
                logger.error("Distrubte flag is send for observation events, however distribute is currently not implemented!");
            }
        }
    }

    /**
     * Test if two resource change listener infos are equal wrt external and change types
     * @param infoA First info
     * @param infoB Second info
     * @return {@code true} if external and change types are equally configured
     */
    private boolean equals(final ResourceChangeListenerInfo infoA, final ResourceChangeListenerInfo infoB) {
        if ( infoA.isExternal() && !infoB.isExternal() ) {
            return false;
        }
        if ( !infoA.isExternal() && infoB.isExternal() ) {
            return false;
        }
        return infoA.getResourceChangeTypes().equals(infoB.getResourceChangeTypes());
    }

    /**
     * Filter the change list based on the configuration
     * @param changes The list of changes
     * @param config The configuration
     * @return The filtered list.
     */
    private List<ResourceChange> filterChanges(final Iterable<ResourceChange> changes, final ObserverConfiguration config) {
        final ResourceChangeListImpl filtered = new ResourceChangeListImpl(this.searchPath);
        for (final ResourceChange c : changes) {
            if (matches(c, config)) {
                filtered.add(c);
            }
        }
        filtered.lock();
        return filtered;
    }

    /**
     * Filter the change list based on the resource change listener, only type and external needs to be checkd.
     * @param changes The list of changes
     * @param config The resource change listener info
     * @return The filtered list.
     */
    private List<ResourceChange> filterChanges(final Iterable<ResourceChange> changes, final ResourceChangeListenerInfo config) {
        final ResourceChangeListImpl filtered = new ResourceChangeListImpl(this.searchPath);
        for (final ResourceChange c : changes) {
            if (matches(c, config)) {
                filtered.add(c);
            }
        }
        filtered.lock();
        return filtered;
    }

    /**
     * Match a change against the configuration
     * @param change The change
     * @param config The configuration
     * @return {@code true} whether it matches
     */
    private boolean matches(final ResourceChange change, final ObserverConfiguration config) {
        if (!config.getChangeTypes().contains(change.getType())) {
            return false;
        }
        if (!config.includeExternal() && change.isExternal()) {
            return false;
        }
        if (config.getPaths().matches(change.getPath()) == null ) {
            return false;
        }
        if ( config.getExcludedPaths().matches(change.getPath()) != null ) {
            return false;
        }
        return true;
    }

    /**
     * Match a change against the configuration
     * @param change The change
     * @param config The configuration
     * @return {@code true} whether it matches
     */
    private boolean matches(final ResourceChange change, final ResourceChangeListenerInfo config) {
        if (!config.getResourceChangeTypes().contains(change.getType())) {
            return false;
        }
        if (!config.isExternal() && change.isExternal()) {
            return false;
        }
        return true;
    }
}
