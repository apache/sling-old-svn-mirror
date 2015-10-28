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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.spi.resource.provider.ObservationReporter;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

public class BasicObservationReporter implements ObservationReporter {

    private final Map<ResourceChangeListener, ObserverConfiguration> listeners;

    private final List<ObserverConfiguration> configs;

    public BasicObservationReporter(Map<ResourceChangeListener, ObserverConfiguration> listeners) {
        this.listeners = new HashMap<ResourceChangeListener, ObserverConfiguration>(listeners);
        this.configs = new ArrayList<ObserverConfiguration>(listeners.values());
    }

    @Override
    public List<ObserverConfiguration> getObserverConfigurations() {
        return configs;
    }

    @Override
    public void reportChanges(Iterable<ResourceChange> changes, boolean distribute) {
        for (Entry<ResourceChangeListener, ObserverConfiguration> e : listeners.entrySet()) {
            List<ResourceChange> filtered = filterChanges(changes, e.getValue());
            e.getKey().onChange(filtered);
        }
    }

    private List<ResourceChange> filterChanges(Iterable<ResourceChange> changes, ObserverConfiguration config) {
        List<ResourceChange> filtered = new ArrayList<ResourceChange>();
        for (ResourceChange c : changes) {
            if (matches(c, config)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    private boolean matches(ResourceChange change, ObserverConfiguration config) {
        if (!config.getChangeTypes().contains(change.getType())) {
            return false;
        }
        if (!config.includeExternal() && change.isExternal()) {
            return false;
        }
        for (String excludedPath : config.getExcludedPaths()) {
            if (change.getPath().startsWith(excludedPath)) {
                return false;
            }
        }
        boolean included = false;
        for (String includedPath : config.getPaths()) {
            if (change.getPath().startsWith(includedPath)) {
                included = true;
                break;
            }
        }
        if (!included) {
            return false;
        }
        return true;
    }
}
