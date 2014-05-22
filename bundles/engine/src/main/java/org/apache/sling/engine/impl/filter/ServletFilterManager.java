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

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.engine.impl.helper.SlingFilterConfig;
import org.apache.sling.engine.impl.helper.SlingServletContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletFilterManager extends ServiceTracker {

    public static enum FilterChainType {
        /**
         * Indicates request level filters.
         *
         * @see EngineConstants#FILTER_SCOPE_REQUEST
         */
        REQUEST("Request"),

        /**
         * Indicates error level filters.
         *
         * @see EngineConstants#FILTER_SCOPE_ERROR
         */
        ERROR("Error"),

        /**
         * Indicates include level filters.
         *
         * @see EngineConstants#FILTER_SCOPE_INCLUDE
         */
        INCLUDE("Include"),

        /**
         * Indicates forward level filters.
         *
         * @see EngineConstants#FILTER_SCOPE_FORWARD
         */
        FORWARD("Forward"),

        /**
         * Indicates component level filters.
         *
         * @see EngineConstants#FILTER_SCOPE_COMPONENT
         */
        COMPONENT("Component");

        private final String message;

        private FilterChainType(final String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    /**
     * The service property used by Felix's HttpService whiteboard
     * implementation.
     */
    private static String FELIX_WHITEBOARD_PATTERN_PROPERTY = "pattern";

    // TODO: use filter (&(objectclass=javax.servlet.Filter)(filter.scope=*))
    private static final String FILTER_SERVICE_NAME = Filter.class.getName();

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SlingServletContext servletContext;

    private final SlingFilterChainHelper[] filterChains;

    private final boolean compatMode;

    public ServletFilterManager(final BundleContext context,
            final SlingServletContext servletContext,
            final boolean compatMode) {
        super(context, FILTER_SERVICE_NAME, null);
        this.servletContext = servletContext;
        this.filterChains = new SlingFilterChainHelper[FilterChainType.values().length];
        this.filterChains[FilterChainType.REQUEST.ordinal()] = new SlingFilterChainHelper();
        this.filterChains[FilterChainType.ERROR.ordinal()] = new SlingFilterChainHelper();
        this.filterChains[FilterChainType.INCLUDE.ordinal()] = new SlingFilterChainHelper();
        this.filterChains[FilterChainType.FORWARD.ordinal()] = new SlingFilterChainHelper();
        this.filterChains[FilterChainType.COMPONENT.ordinal()] = new SlingFilterChainHelper();
        this.compatMode = compatMode;
    }

    public SlingFilterChainHelper getFilterChain(final FilterChainType chain) {
        return filterChains[chain.ordinal()];
    }

    public Filter[] getFilters(final FilterChainType chain) {
        return getFilterChain(chain).getFilters();
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = super.addingService(reference);
        if (service instanceof Filter) {
            initFilter(reference, (Filter) service);
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        if (service instanceof Filter) {
            destroyFilter(reference, (Filter) service);
            initFilter(reference, (Filter) service);
        }

        super.modifiedService(reference, service);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        if (service instanceof Filter) {
            destroyFilter(reference, (Filter) service);
        }
        super.removedService(reference, service);
    }

    /**
     * Check if the filter should be excluded.
     */
    private boolean excludeFilter(final ServiceReference reference) {
        // if the service has a filter scope property, we include it
        if ( reference.getProperty(EngineConstants.SLING_FILTER_SCOPE) != null
             || reference.getProperty(EngineConstants.FILTER_SCOPE) != null ) {
            return false;
        }
        // in compat mode we allow all filters not having the felix pattern prop!
        if ( this.compatMode ) {
            // Check if filter will be registered by Felix HttpService Whiteboard
            if (reference.getProperty(FELIX_WHITEBOARD_PATTERN_PROPERTY) != null) {
                return true;
            }
            return false;
        }
        return true;
    }

    private void initFilter(final ServiceReference reference,
            final Filter filter) {
        if ( this.excludeFilter(reference) ) {
            return;
        }
        final String filterName = SlingFilterConfig.getName(reference);
        if (filterName == null) {
            log.error("initFilter: Missing name for filter {}", reference);
        } else {

            // initialize the filter first
            try {
                final FilterConfig config = new SlingFilterConfig(
                    servletContext, reference, filterName);
                filter.init(config);

                // service id
                final Long serviceId = (Long) reference.getProperty(Constants.SERVICE_ID);

                // get the order, Integer.MAX_VALUE by default
                final String orderSource;
                Object orderObj = reference.getProperty(Constants.SERVICE_RANKING);
                if (orderObj == null) {
                    // filter order is defined as lower value has higher
                    // priority while service ranking is the opposite In
                    // addition we allow different types than Integer
                    orderObj = reference.getProperty(EngineConstants.FILTER_ORDER);
                    if (orderObj != null) {
                        // we can use 0 as the default as this will be applied
                        // in the next step anyway if this props contains an
                        // invalid value
                        orderSource = EngineConstants.FILTER_ORDER + "=" + orderObj;
                        orderObj = Integer.valueOf(-1 * OsgiUtil.toInteger(orderObj, 0));
                    } else {
                        orderSource = "none";
                    }
                } else {
                    orderSource = Constants.SERVICE_RANKING + "=" + orderObj;
                }
                final int order = (orderObj instanceof Integer) ? ((Integer) orderObj).intValue() : 0;

                // register by scope
                String[] scopes = OsgiUtil.toStringArray(
                        reference.getProperty(EngineConstants.SLING_FILTER_SCOPE), null);
                if ( scopes == null ) {
                    scopes = OsgiUtil.toStringArray(
                        reference.getProperty(EngineConstants.FILTER_SCOPE), null);
                }
                if (scopes != null && scopes.length > 0) {
                    for (String scope : scopes) {
                        scope = scope.toUpperCase();
                        try {
                            FilterChainType type = FilterChainType.valueOf(scope.toString());
                            getFilterChain(type).addFilter(filter, serviceId,
                                order, orderSource);

                            if (type == FilterChainType.COMPONENT) {
                                getFilterChain(FilterChainType.INCLUDE).addFilter(
                                    filter, serviceId, order, orderSource);
                                getFilterChain(FilterChainType.FORWARD).addFilter(
                                    filter, serviceId, order, orderSource);
                            }

                        } catch (IllegalArgumentException iae) {
                            // TODO: log ...
                        }
                    }
                } else {
                    log.warn(String.format(
                        "A Filter (Service ID %s) has been registered without a filter.scope property.",
                        reference.getProperty(Constants.SERVICE_ID)));
                    getFilterChain(FilterChainType.REQUEST).addFilter(filter,
                        serviceId, order, orderSource);
                }

            } catch (ServletException ce) {
                log.error("Filter " + filterName + " failed to initialize", ce);
            } catch (Throwable t) {
                log.error("Unexpected Problem initializing ComponentFilter "
                    + "", t);
            }
        }
    }

    private void destroyFilter(final ServiceReference reference,
            final Filter filter) {
        // service id
        Object serviceId = reference.getProperty(Constants.SERVICE_ID);
        boolean removed = false;
        for (SlingFilterChainHelper filterChain : filterChains) {
            removed |= filterChain.removeFilterById(serviceId);
        }

        // destroy it
        if (removed) {
            try {
                filter.destroy();
            } catch (Throwable t) {
                log.error("Unexpected problem destroying Filter {}", filter, t);
            }
        }
    }
}
