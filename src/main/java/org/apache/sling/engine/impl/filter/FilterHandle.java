/*************************************************************************
*
* ADOBE CONFIDENTIAL
* ___________________
*
*  Copyright 2014 Adobe Systems Incorporated
*  All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
**************************************************************************/
package org.apache.sling.engine.impl.filter;

import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Filter;

public class FilterHandle implements Comparable<FilterHandle> {

    private final Filter filter;

    private final Long filterId;

    private final int order;

    private final String orderSource;

    private AtomicLong calls;

    private AtomicLong time;

    FilterHandle(Filter filter, Long filterId, int order, final String orderSource) {
        this.filter = filter;
        this.filterId = filterId;
        this.order = order;
        this.orderSource = orderSource;
        this.calls = new AtomicLong();
        this.time = new AtomicLong();
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

    public long getCalls() {
        return calls.get();
    }

    public long getTime() {
        return time.get();
    }

    public long getTimePerCall() {
        return (getCalls() > 0) ? (1000L * getTime() / getCalls()) : -1;
    }

    void track() {
        calls.incrementAndGet();
    }

    void trackTime(long time) {
        this.time.addAndGet(time);
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with
     * equals.
     */
    public int compareTo(FilterHandle other) {
        if (this == other || equals(other)) {
            return 0;
        }

        if (order > other.order) {
            return -1;
        } else if (order < other.order) {
            return 1;
        }

        // if the filterId is comparable and the other is of the same class
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof FilterHandle) {
            FilterHandle other = (FilterHandle) obj;
            return getFilter().equals(other.getFilter());
        }

        return false;
    }
}