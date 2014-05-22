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

    private static final Filter[] EMPTY_FILTER_ARRAY = new Filter[0];

    private SortedSet<FilterListEntry> filterList;

    private Filter[] filters = EMPTY_FILTER_ARRAY;

    SlingFilterChainHelper() {
    }

    public synchronized Filter addFilter(final Filter filter,
            final Long filterId, final int order, final String orderSource) {
        if (filterList == null) {
            filterList = new TreeSet<FilterListEntry>();
        }
        filterList.add(new FilterListEntry(filter, filterId, order, orderSource));
        filters = this.getFilterArray();
        return filter;
    }

    public synchronized boolean removeFilterById(final Object filterId) {
        if (filterList != null) {
            for (Iterator<FilterListEntry> fi = filterList.iterator(); fi.hasNext();) {
                FilterListEntry test = fi.next();
                if (test.getFilterId() == filterId
                    || (test.getFilterId() != null && test.getFilterId().equals(
                        filterId))) {
                    fi.remove();
                    filters = this.getFilterArray();
                    return true;
                }
            }
        }

        // no removed filter
        return false;
    }

    /**
     * Returns the list of <code>Filter</code>s added to this instance
     * or <code>null</code> if no filters have been added.
     * This method doesn't need to be synced as it is called from synced methods.
     */
    private Filter[] getFilterArray() {
        if (filterList != null && !filterList.isEmpty()) {
            final Filter[] tmp = new Filter[filterList.size()];
            int i = 0;
            for (FilterListEntry entry : filterList) {
                tmp[i] = entry.getFilter();
                i++;
            }
            return tmp;
        }
        return EMPTY_FILTER_ARRAY;
    }

    /**
     * Returns the list of <code>Filter</code>s added to this instance
     * or <code>null</code> if no filters have been added.
     * This method doesn't need to be synced as it is only
     * returned the current cached filter array.
     */
    public Filter[] getFilters() {
        return this.filters;
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

        private final String orderSource;

        FilterListEntry(final Filter filter, final Long filterId, final int order, final String orderSource) {
            this.filter = filter;
            this.filterId = filterId;
            this.order = order;
            this.orderSource = orderSource;
        }

        public Filter getFilter() {
            return filter;
        }

        public Long getFilterId() {
            return filterId;
        }

        public int getOrder() {
            return order;
        }

        public String getOrderSource() {
            return orderSource;
        }

        /**
         * Note: this class has a natural ordering that is inconsistent with
         * equals.
         */
        public int compareTo(final FilterListEntry other) {
            if (this == other || equals(other)) {
                return 0;
            }

            // new service.ranking order (correct)
            if (order > other.order) {
                return -1;
            } else if (order < other.order) {
                return 1;
            }

            // compare filter id (service id)
            if (filterId != null && other.filterId != null) {
                int comp = filterId.compareTo(other.filterId);
                if (comp != 0) {
                    return comp;
                }
            }

            // consider equal ranking
            return 0;
        }

        @Override
        public int hashCode() {
            if ( filter == null ) {
                return 0;
            }
            return filter.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof FilterListEntry) {
                final FilterListEntry other = (FilterListEntry) obj;
                return getFilter().equals(other.getFilter());
            }

            return false;
        }
    }
}
