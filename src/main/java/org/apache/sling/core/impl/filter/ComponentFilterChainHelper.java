/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.filter;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.component.ComponentFilter;

/**
 * The <code>ComponentFilterChainHelper</code> class is used by Sling to support
 * building lists of <code>RenderFilter</code>s. To ensure filter ordering,
 * each filter is optionally registered with an ordering index. If none is
 * provided the default ordering index is Integer.MAX_VALUE to append the filter
 * to the end of the list.
 */
public class ComponentFilterChainHelper {

    SortedSet filterList;

    ComponentFilter[] filters;

    public ComponentFilterChainHelper() {
    }

    public ComponentFilter addFilter(ComponentFilter filter) {
        return addFilter(filter, filter, Integer.MAX_VALUE);
    }

    public synchronized ComponentFilter addFilter(ComponentFilter filter,
            Object filterId, int order) {
        filters = null;
        if (filterList == null) {
            filterList = new TreeSet();
        }
        filterList.add(new FilterListEntry(filter, filterId, order));
        return filter;
    }

    public synchronized ComponentFilter removeFilter(ComponentFilter filter) {
        if (filterList != null) {
            filters = null;
            for (Iterator fi=filterList.iterator(); fi.hasNext(); ) {
                FilterListEntry test = (FilterListEntry) fi.next();
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
        if (filterList != null) {
            filters = null;
            for (Iterator fi=filterList.iterator(); fi.hasNext(); ) {
                FilterListEntry test = (FilterListEntry) fi.next();
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
        if (filters == null) {
            if (filterList != null && !filterList.isEmpty()) {
                ComponentFilter[] tmp = new ComponentFilter[filterList.size()];
                int i = 0;
                for (Iterator fi = filterList.iterator(); fi.hasNext(); i++) {
                    tmp[i] = ((FilterListEntry) fi.next()).getFilter();
                }
                filters = tmp;
            }
        }
        return filters;
    }

    private static class FilterListEntry implements Comparable {

        private ComponentFilter filter;

        private Object filterId;

        private int order;

        FilterListEntry(ComponentFilter filter, Object filterId, int order) {
            this.filter = filter;
            this.filterId = filterId;
            this.order = order;
        }

        ComponentFilter getFilter() {
            return filter;
        }
        
        Object getFitlerId() {
            return filterId;
        }

        /**
         * Note: this class has a natural ordering that is inconsistent with
         * equals.
         */
        public int compareTo(Object obj) {
            if (this == obj || equals(obj)) {
                return 0;
            }

            FilterListEntry other = (FilterListEntry) obj;
            if (order < other.order) {
                return -1;
            } else if (order > other.order) {
                return 1;
            }

            // if the filterId is comparable and the other is of the same class
            if (filterId instanceof Comparable && other.filterId != null
                && filterId.getClass() == other.filterId.getClass()) {
                int comp = ((Comparable) filterId).compareTo(other.filterId);
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
                return getFilter().equals(other.getFilter());
            }

            return false;
        }
    }
}
