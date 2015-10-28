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

import static java.util.Arrays.asList;
import static org.apache.sling.api.resource.observation.ResourceChangeListener.CHANGES;
import static org.apache.sling.api.resource.observation.ResourceChangeListener.PATHS;
import static org.apache.sling.commons.osgi.PropertiesUtil.toStringArray;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.spi.resource.provider.ObserverConfiguration;

public class BasicObserverConfiguration implements ObserverConfiguration {

    private final boolean includeExternal;

    private final Set<String> paths;

    private final Set<String> excludedPaths;

    private final Set<ChangeType> changeTypes;

    private BasicObserverConfiguration(Builder builder) {
        this.includeExternal = builder.isIncludeExternal();
        this.paths = builder.getPaths();
        this.excludedPaths = builder.getExludedPaths();
        this.changeTypes = builder.getChangeTypes();
    }

    @Override
    public boolean includeExternal() {
        return includeExternal;
    }

    @Override
    public Set<String> getPaths() {
        return paths;
    }

    @Override
    public Set<String> getExcludedPaths() {
        return excludedPaths;
    }

    @Override
    public Set<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    public static class Builder {
        private boolean includeExternal;

        private Set<String> paths = Collections.emptySet();

        private Set<String> excludedPaths = Collections.emptySet();

        private Set<ChangeType> changeTypes = Collections.emptySet();

        private String[] searchPaths = new String[0];

        public boolean isIncludeExternal() {
            return includeExternal;
        }

        public Builder setIncludeExternal(boolean includeExternal) {
            this.includeExternal = includeExternal;
            return this;
        }

        public Set<String> getPaths() {
            return normalizePaths(paths);
        }

        public Builder setPaths(Set<String> paths) {
            this.paths = paths;
            return this;
        }

        public Set<String> getExludedPaths() {
            return normalizePaths(excludedPaths);
        }

        public Builder setExcludedPaths(Set<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
            return this;
        }

        public Set<ChangeType> getChangeTypes() {
            return changeTypes;
        }

        public Builder setChangeTypes(Set<ChangeType> changeTypes) {
            this.changeTypes = changeTypes;
            return this;
        }

        public Builder setFromProperties(Map<String, Object> properties) {
            if (properties.containsKey(PATHS)) {
                this.paths = new HashSet<String>(asList(toStringArray(properties.get(PATHS))));
            } else {
                this.paths = Collections.emptySet();
            }
            if (properties.containsKey(CHANGES)) {
                this.changeTypes = EnumSet.noneOf(ChangeType.class);
                for (String changeName : toStringArray(properties.get(CHANGES))) {
                    this.changeTypes.add(ChangeType.valueOf(changeName));
                }
            } else {
                this.changeTypes = EnumSet.allOf(ChangeType.class);
            }
            return this;
        }

        public Builder setSearchPaths(String[] searchPaths) {
            this.searchPaths = searchPaths;
            return this;
        }

        public ObserverConfiguration build() {
            return new BasicObserverConfiguration(this);
        }

        private Set<String> normalizePaths(Set<String> relativePaths) {
            Set<String> absolutePaths = getAbsolutePaths(relativePaths);
            removeSubPaths(absolutePaths);
            return absolutePaths;
        }

        private static void removeSubPaths(Set<String> absolutePaths) {
            Iterator<String> it = absolutePaths.iterator();
            while (it.hasNext()) {
                String currentPath = it.next();
                for (String p : absolutePaths) {
                    if (!p.equals(currentPath) && currentPath.startsWith(p)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        private Set<String> getAbsolutePaths(Set<String> relativePaths) {
            Set<String> absolutePaths = new HashSet<String>();
            if (relativePaths == null) {
                return absolutePaths;
            }
            for (String path : relativePaths) {
                if (path.startsWith("/")) {
                    absolutePaths.add(path);
                } else if (".".equals(path)) {
                    absolutePaths.add("/");
                } else {
                    for (String searchPath : searchPaths) {
                        absolutePaths.add(searchPath + path);
                    }
                }
            }
            return absolutePaths;
        }
    }
}
