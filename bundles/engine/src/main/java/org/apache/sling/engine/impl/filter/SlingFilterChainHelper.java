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

    private static final FilterHandle[] EMPTY_FILTER_ARRAY = new FilterHandle[0];

    private SortedSet<FilterHandle> filterList;

    private FilterHandle[] filters = EMPTY_FILTER_ARRAY;

    SlingFilterChainHelper() {
    }

    public synchronized Filter addFilter(final Filter filter,  String pattern,
            final Long filterId, final int order, final String orderSource, FilterProcessorMBeanImpl mbean) {
        if (filterList == null) {
            filterList = new TreeSet<FilterHandle>();
        }
        filterList.add(new FilterHandle(filter, pattern, filterId, order, orderSource, mbean));
        filters = getFiltersInternal();
        return filter;
    }

    public synchronized boolean removeFilterById(final Object filterId) {
        if (filterList != null) {
            for (Iterator<FilterHandle> fi = filterList.iterator(); fi.hasNext();) {
                FilterHandle test = fi.next();
                if (test.getFilterId() == filterId
                    || (test.getFilterId() != null && test.getFilterId().equals(
                        filterId))) {
                    fi.remove();
                    filters = getFiltersInternal();
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
    public FilterHandle[] getFilters() {
        return filters;
    }

    private FilterHandle[] getFiltersInternal() {
        if (filterList == null || filterList.isEmpty()) {
            return EMPTY_FILTER_ARRAY;
        }
        return filterList.toArray(new FilterHandle[filterList.size()]);
    }
}