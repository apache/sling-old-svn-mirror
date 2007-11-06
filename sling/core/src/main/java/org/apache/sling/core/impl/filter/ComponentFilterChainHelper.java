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
package org.apache.sling.core.impl.filter;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.component.ComponentFilter;

/**
 * The <code>ComponentFilterChainHelper</code> class is used by Sling to
 * support building lists of <code>RenderFilter</code>s. To ensure filter
 * ordering, each filter is optionally registered with an ordering index. If
 * none is provided the default ordering index is Integer.MAX_VALUE to append
 * the filter to the end of the list.
 */
public class ComponentFilterChainHelper {

    SortedSet<FilterListEntry> filterList;

    ComponentFilter[] filters;

    public ComponentFilterChainHelper() {
    }

    public synchronized ComponentFilter addFilter(ComponentFilter filter,
            Long filterId, int order) {
        this.filters = null;
        if (this.filterList == null) {
            this.filterList = new TreeSet<FilterListEntry>();
        }
        this.filterList.add(new FilterListEntry(filter, filterId, order));
        return filter;
    }

    public synchronized ComponentFilter[] removeAllFilters() {
        // will be returned after cleaning the lists
        ComponentFilter[] removedFilters = getFilters();

        filters = null;
        filterList = null;

        return removedFilters;
    }

    public synchronized ComponentFilter removeFilter(ComponentFilter filter) {
        if (this.filterList != null) {
            this.filters = null;
            for (Iterator<FilterListEntry> fi = filterList.iterator(); fi.hasNext();) {
                FilterListEntry test = fi.next();
                if (test.getFilter().equals(filter)) {
                    fi.remove();
                    return test.getFilter();
                }
            }
        }

        // no removed ComponentFilter
        return null;
    }

    public synchronized ComponentFilter removeFilterById(Object filterId) {
        if (this.filterList != null) {
            this.filters = null;
            for (Iterator<FilterListEntry> fi = filterList.iterator(); fi.hasNext();) {
                FilterListEntry test = fi.next();
                if (test.getFitlerId() == filterId
                    || (test.getFitlerId() != null && test.getFitlerId().equals(
                        filterId))) {
                    fi.remove();
                    return test.getFilter();
                }
            }
        }

        // no removed ComponentFilter
        return null;
    }

    /**
     * Returns the list of <code>RenderFilter</code>s added to this instance
     * or <code>null</code> if no filters have been added.
     */
    public synchronized ComponentFilter[] getFilters() {
        if (this.filters == null) {
            if (this.filterList != null && !this.filterList.isEmpty()) {
                ComponentFilter[] tmp = new ComponentFilter[this.filterList.size()];
                int i = 0;
                for (FilterListEntry entry : filterList) {
                    tmp[i] = entry.getFilter();
                    i++;
                }
                this.filters = tmp;
            }
        }
        return this.filters;
    }

    private static class FilterListEntry implements Comparable<FilterListEntry> {

        private ComponentFilter filter;

        private Long filterId;

        private int order;

        FilterListEntry(ComponentFilter filter, Long filterId, int order) {
            this.filter = filter;
            this.filterId = filterId;
            this.order = order;
        }

        ComponentFilter getFilter() {
            return this.filter;
        }

        Long getFitlerId() {
            return this.filterId;
        }

        /**
         * Note: this class has a natural ordering that is inconsistent with
         * equals.
         */
        public int compareTo(FilterListEntry other) {
            if (this == other || this.equals(other)) {
                return 0;
            }

            if (this.order < other.order) {
                return -1;
            } else if (this.order > other.order) {
                return 1;
            }

            // if the filterId is comparable and the other is of the same class
            if (filterId != null && other.filterId != null) {
                int comp = filterId.compareTo(other.filterId);
                if (comp != 0) {
                    return comp;
                }
            }

            // this is inserted, obj is existing key
            return 1; // insert after current key
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof FilterListEntry) {
                FilterListEntry other = (FilterListEntry) obj;
                return this.getFilter().equals(other.getFilter());
            }

            return false;
        }
    }
}
