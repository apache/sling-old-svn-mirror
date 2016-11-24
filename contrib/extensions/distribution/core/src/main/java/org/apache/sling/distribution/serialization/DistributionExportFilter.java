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
package org.apache.sling.distribution.serialization;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.distribution.DistributionRequest;

/**
 * A filter is responsible for storing information about which resources / attributes should be serialized.
 */
public class DistributionExportFilter {

    private final Set<TreeFilter> nodeFilters = new HashSet<TreeFilter>();
    private TreeFilter propertyFilter;

    private DistributionExportFilter() {
        // can only be constructed by #createFilter
    }

    @Nonnull
    public Set<TreeFilter> getNodeFilters() {
        return nodeFilters;
    }

    @Nonnull
    public TreeFilter getPropertyFilter() {
        return propertyFilter;
    }

    /**
     * create a filter based on a request and global node and property filters
     * @param distributionRequest the request
     * @param nodeFilters the node level filters
     * @param propertyFilters the property level filters
     * @return a filter
     */
    public static DistributionExportFilter createFilter(DistributionRequest distributionRequest,
                                                        NavigableMap<String, List<String>> nodeFilters,
                                                        NavigableMap<String, List<String>> propertyFilters) {
        DistributionExportFilter exportFilter = new DistributionExportFilter();

        for (String path : distributionRequest.getPaths()) {

            boolean deep = distributionRequest.isDeep(path);
            TreeFilter treeFilter = new TreeFilter(path);

            if (deep) {
                treeFilter.addDeepInclude(path);
            } else {
                treeFilter.addInclude(path);
            }

            List<String> patterns = new LinkedList<String>();
            patterns.addAll(Arrays.asList(distributionRequest.getFilters(path)));
            initFilter(nodeFilters, treeFilter, patterns);

            exportFilter.addNodeFilter(treeFilter);

        }
        // Set property path filters
        TreeFilter propertyFilterSet = new TreeFilter("/");
        initFilter(propertyFilters, propertyFilterSet, new ArrayList<String>());
        exportFilter.setPropertyFilter(propertyFilterSet);

        return exportFilter;
    }

    private void addNodeFilter(TreeFilter filter) {
        nodeFilters.add(filter);
    }

    private static void initFilter(NavigableMap<String, List<String>> globalFilters, TreeFilter treeFilter, List<String> patterns) {
        // add the most specific filter rules
        for (String key : globalFilters.descendingKeySet()) {
            if (treeFilter.getPath().startsWith(key)) {
                patterns.addAll(globalFilters.get(key));
                break;
            }
        }

        for (String pattern : patterns) {
            TreeFilter.Entry entry = extractPathPattern(pattern);

            if (entry.isInclude()) {
                treeFilter.addInclude(entry.getPath());
            } else {
                treeFilter.addExclude(entry.getPath());
            }
        }
    }

    private static TreeFilter.Entry extractPathPattern(String pattern) {
        TreeFilter.Entry result;
        if (pattern.startsWith("+")) {
            result = new TreeFilter.Entry(pattern.substring(1), true);
        } else if (pattern.startsWith("-")) {
            result = new TreeFilter.Entry(pattern.substring(1), false);
        } else {
            result = new TreeFilter.Entry(pattern, true);
        }

        return result;
    }

    private void setPropertyFilter(TreeFilter propertyFilter) {
        this.propertyFilter = propertyFilter;
    }

    /**
     * a filter is responsible for finding the resources that should be serialized unders a certain path
     */
    public static class TreeFilter {
        private final String path;
        private final Collection<String> includes;
        private final Collection<String> excludes;
        private final Collection<String> deepIncludes;

        public TreeFilter(String path) {
            this.path = path;
            this.includes = new TreeSet<String>();
            this.deepIncludes = new TreeSet<String>();
            this.excludes = new TreeSet<String>();
        }

        public void addInclude(String path) {
            includes.add(path);
        }

        public void addDeepInclude(String path) {
            deepIncludes.add(path);
        }

        public void addExclude(String path) {
            excludes.add(path);
        }

        @Nonnull
        public String getPath() {
            return path;
        }

        /**
         * check whether a resource with a certain path should be included in the serialized output
         * @param path a path
         * @return {@code true} if the path mathces the filter, {@code false} otherwise
         */
        public boolean matches(String path) {
            boolean match = (includes.isEmpty() && excludes.isEmpty()) || includes.contains(path);
            if (!match) {
                for (String di : deepIncludes) {
                    match = path.startsWith(di);
                    if (match) {
                        break;
                    }
                }
            }
            match &= !excludes.contains(path);
            return match;
        }

        @Override
        public String toString() {
            return "TreeFilter{" +
                    "path='" + path + '\'' +
                    ", includes=" + includes +
                    ", excludes=" + excludes +
                    ", deepIncludes=" + deepIncludes +
                    '}';
        }

        private static class Entry {
            private final String path;
            private final boolean include;

            public Entry(String path, boolean include) {
                this.path = path;
                this.include = include;
            }

            public boolean isInclude() {
                return include;
            }

            public String getPath() {
                return path;
            }
        }
    }

    @Override
    public String toString() {
        return "DistributionExportFilter{" +
                "nodeFilters=" + nodeFilters +
                ", propertyFilter=" + propertyFilter +
                '}';
    }
}
