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
package org.apache.sling.engine.impl.filter;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Filter;

/**
 * The <code>SlingFilterChainHelper</code> class is used by Sling to
 * support building lists of <code>Filter</code>s. To ensure filter
 * ordering, each filter is optionally registered with an ordering index. If
 * none is provided the default ordering index is Integer.MAX_VALUE to append
 * the filter to the end of the list.
 */
public class SlingFilterChainHelper {

    SortedSet<FilterListEntry> filterList;

    Filter[] filters;

    SlingFilterChainHelper() {
    }

    public synchronized Filter addFilter(Filter filter,
            Long filterId, int order) {
        filters = null;
        if (filterList == null) {
            filterList = new TreeSet<FilterListEntry>();
        }
        filterList.add(new FilterListEntry(filter, filterId, order));
        return filter;
    }

    public synchronized Filter[] removeAllFilters() {
        // will be returned after cleaning the lists
        Filter[] removedFilters = getFilters();

        filters = null;
        filterList = null;

        return removedFilters;
    }

    public synchronized Filter removeFilter(Filter filter) {
        if (filterList != null) {
            filters = null;
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

    public synchronized boolean removeFilterById(Object filterId) {
        if (filterList != null) {
            filters = null;
            for (Iterator<FilterListEntry> fi = filterList.iterator(); fi.hasNext();) {
                FilterListEntry test = fi.next();
                if (test.getFitlerId() == filterId
                    || (test.getFitlerId() != null && test.getFitlerId().equals(
                        filterId))) {
                    fi.remove();
                    return true;
                }
            }
        }

        // no removed ComponentFilter
        return false;
    }

    /**
     * Returns the list of <code>Filter</code>s added to this instance
     * or <code>null</code> if no filters have been added.
     */
    public synchronized Filter[] getFilters() {
        if (filters == null) {
            if (filterList != null && !filterList.isEmpty()) {
                Filter[] tmp = new Filter[filterList.size()];
                int i = 0;
                for (FilterListEntry entry : filterList) {
                    tmp[i] = entry.getFilter();
                    i++;
                }
                filters = tmp;
            }
        }
        return filters;
    }

    /**
     * Returns the list of <code>FilterListEntry</code>s added to this instance
     * or <code>null</code> if no filters have been added.
     */
    public synchronized FilterListEntry[] getFilterListEntries() {
        FilterListEntry[] result = null;
        if (filterList != null && !filterList.isEmpty()) {
            result = new FilterListEntry[filterList.size()];
            filterList.toArray(result);
        }
        return result;
    }

    public static class FilterListEntry implements Comparable<FilterListEntry> {

        private final Filter filter;

        private final Long filterId;

        private final int order;

        FilterListEntry(Filter filter, Long filterId, int order) {
            this.filter = filter;
            this.filterId = filterId;
            this.order = order;
        }

        public Filter getFilter() {
            return filter;
        }

        public Long getFitlerId() {
            return filterId;
        }

        public int getOrder() {
            return order;
        }

        /**
         * Note: this class has a natural ordering that is inconsistent with
         * equals.
         */
        public int compareTo(FilterListEntry other) {
            if (this == other || equals(other)) {
                return 0;
            }

            if (order < other.order) {
                return -1;
            } else if (order > other.order) {
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

        @Override
        public int hashCode() {
            if ( filter == null ) {
                return 0;
            }
            return filter.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof FilterListEntry) {
                FilterListEntry other = (FilterListEntry) obj;
                return getFilter().equals(other.getFilter());
            }

            return false;
        }
    }
}
