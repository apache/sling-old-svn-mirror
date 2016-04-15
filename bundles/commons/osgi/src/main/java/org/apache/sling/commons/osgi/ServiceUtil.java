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
package org.apache.sling.commons.osgi;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * The <code>ServiceUtil</code> is a utility class providing some
 * useful utility methods for service handling.
 * @since 2.1
 */
public class ServiceUtil {

    /**
     * @deprecated Use {@link #getComparableForServiceRanking(Map, Order)} instead.
     * @param props The service properties.
     * @return the same comparable as returned by {@link #getComparableForServiceRanking(Map, Order.ASCENDING)}
     * @see #getComparableForServiceRanking(Map, Order)
     */
    @Deprecated
    public static Comparable<Object> getComparableForServiceRanking(final Map<String, Object> props) {
        return getComparableForServiceRanking(props, Order.ASCENDING);
    }

    /**
     * Create a comparable object out of the service properties. With the result
     * it is possible to compare service properties based on the service ranking
     * of a service. This object acts like {@link ServiceReference#compareTo(Object)}.
     * The comparator will return the services in the given order. In ascending order the 
     * service with the lowest ranking comes first, in descending order the service with the 
     * highest ranking comes first. The latter is useful if you want to have the service 
     * returned first which is also chosen by {@link BundleContext#getServiceReference(String)}.
     * @param props The service properties.
     * @param order The order (either ascending or descending).
     * @return A comparable for the ranking of the service
     * @since 2.4
     */
    public static Comparable<Object> getComparableForServiceRanking(final Map<String, Object> props, Order order) {
        return new ComparableImplementation(props, order);
    }

    private static final class ComparableImplementation implements Comparable<Object> {

        private final Map<String, Object> props;
        private final Order order;

        private ComparableImplementation(Map<String, Object> props, Order order) {
            this.props = props;
            this.order = order;
        }

        @SuppressWarnings("unchecked")
        public int compareTo(Object reference) {
            final Long otherId;
            Object otherRankObj;
            if ( reference instanceof ServiceReference ) {
                final ServiceReference other = (ServiceReference) reference;
                otherId = (Long) other.getProperty(Constants.SERVICE_ID);
                otherRankObj = other.getProperty(Constants.SERVICE_RANKING);
            } else if (reference instanceof Map){
                final Map<String, Object> otherProps = (Map<String, Object>)reference;
                otherId = (Long) otherProps.get(Constants.SERVICE_ID);
                otherRankObj = otherProps.get(Constants.SERVICE_RANKING);
            } else {
                final ComparableImplementation other = (ComparableImplementation)reference;
                otherId = (Long) other.props.get(Constants.SERVICE_ID);
                otherRankObj = other.props.get(Constants.SERVICE_RANKING);
            }
            final Long id = (Long) props.get(Constants.SERVICE_ID);
            if (id.equals(otherId)) {
                return 0; // same service
            }

            Object rankObj = props.get(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = (rankObj instanceof Integer)
                ? (Integer) rankObj : new Integer(0);
            Integer otherRank = (otherRankObj instanceof Integer)
                ? (Integer) otherRankObj : new Integer(0);

            // Sort by rank.
            if (rank.compareTo(otherRank) < 0) {
                return order.lessThan; // lower rank
            } else if (rank.compareTo(otherRank) > 0) {
                return order.greaterThan; // higher rank
            }

            // If ranks are equal, then sort by service id.
            return (id.compareTo(otherId) < 0) ? order.greaterThan : order.lessThan;
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof ComparableImplementation ) {
                return this.props.equals(((ComparableImplementation)obj).props);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.props.hashCode();
        }
    }
}
