/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.execution.HealthCheckSelector;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.hc.api.execution.HealthCheckSelector.tags;
import static org.apache.sling.hc.api.execution.HealthCheckSelector.empty;

/**
 * Select from available {@link HealthCheck} services.
 * Once this filter object and the returned health check services are no longer
 * be used {@link #dispose()} should be called, to free the service
 * references.
 *
 * This class is not thread safe and instances shouldn't be used concurrently
 * from different threads.
 */
@ProviderType
public class HealthCheckFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BundleContext bundleContext;


    public static final String OMIT_PREFIX = "-";

    private final Set<ServiceReference> usedReferences = new HashSet<ServiceReference>();

    /**
     * Create a new filter object
     */
    public HealthCheckFilter(final BundleContext bc) {
        bundleContext = bc;
    }

    public List<HealthCheck> getHealthChecks(final HealthCheckSelector selector) {
        final ServiceReference [] refs = this.getHealthCheckServiceReferences(selector);
        final List<HealthCheck> result = new ArrayList<HealthCheck>();

        if ( refs != null ) {
            final List<ServiceReference> sortedRefs = Arrays.asList(refs);
            Collections.sort(sortedRefs);

            for(final ServiceReference ref : sortedRefs) {
                final HealthCheck hc = (HealthCheck)bundleContext.getService(ref);
                log.debug("Selected HealthCheck service {}", hc);
                if ( hc != null ) {
                    this.usedReferences.add(ref);
                    result.add(hc);
                }
            }
        }

        return result;
    }

    public ServiceReference[] getHealthCheckServiceReferences(final HealthCheckSelector selector) {
        return getHealthCheckServiceReferences(selector, false);
    }

    public ServiceReference[] getHealthCheckServiceReferences(final HealthCheckSelector selector, boolean combineTagsWithOr) {
        final CharSequence filterBuilder = selector != null ? getServiceFilter(selector, combineTagsWithOr) : getServiceFilter(empty(), combineTagsWithOr);

        log.debug("OSGi service filter in getHealthCheckServiceReferences(): {}", filterBuilder);

        try {
            final String filterString = filterBuilder.length() == 0 ? null : filterBuilder.toString();
            bundleContext.createFilter(filterString); // check syntax early
            final ServiceReference[] refs = bundleContext.getServiceReferences(HealthCheck.class.getName(), filterString);
            if (refs == null) {
                log.debug("Found no HealthCheck services with filter [{}]", filterString);
                return new ServiceReference[0];
            } else {
                log.debug("Found {} HealthCheck services with filter [{}]", refs.length, filterString);
            }
            return refs;
        } catch (final InvalidSyntaxException ise) {
            // this should not happen, but we fail gracefully
            log.error("Invalid OSGi filter syntax in '" + filterBuilder + "'", ise);
            return new ServiceReference[0];
        }
    }

    /**
     * Get all health check services with one of the supplied tags.
     * @return A list of services - might be the empty list if none matches
     * @deprecated use getHealthChecks() instead
     */
    @Deprecated
    public List<HealthCheck> getTaggedHealthChecks(final String... tags) {
        final HealthCheckSelector selector = tags(tags);
        return getHealthChecks(selector);
    }

    /**
     * Get all service references for health check services with one of the supplied tags. Uses logical "and" to combine tags.
     * @return An array of service references - might be an empty error if none matches
     * @deprecated use getHealthCheckServiceReferences() instead
     */
    @Deprecated
    public ServiceReference[] getTaggedHealthCheckServiceReferences(final String... tags) {
        return getHealthCheckServiceReferences(tags(tags), false);
    }

    /**
     * Get all service references for health check services with one of the supplied tags.
     * 
     * @param combineWithOr If true will return all health checks that have at least one of the tags set. 
     *        If false will return only health checks that have all given tags assigned.
     * @param tags the tags to look for
     * @return An array of service references - might be an empty error if none matches
     * @deprecated use getHealthCheckServiceReferences() instead
     */
    @Deprecated
    public ServiceReference[] getTaggedHealthCheckServiceReferences(boolean combineWithOr, final String... tags) {
        final HealthCheckSelector selector = tags(tags);
        return getHealthCheckServiceReferences(selector, combineWithOr);
    }

    /**
     * Dispose all used service references
     */
    public void dispose() {
        for(final ServiceReference ref : this.usedReferences) {
            this.bundleContext.ungetService(ref);
        }
        this.usedReferences.clear();
    }

    CharSequence getServiceFilter(HealthCheckSelector selector, boolean combineTagsWithOr) {
        // Build service filter
        final StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("(&(objectClass=").append(HealthCheck.class.getName()).append(")");
        final int prefixLen = HealthCheckFilter.OMIT_PREFIX.length();
        final StringBuilder filterBuilderForOrOperator = new StringBuilder(); // or filters
        final StringBuilder tagsBuilder = new StringBuilder();
        int tagsAndClauses = 0;
        if (selector.tags() != null) {
            for (String tag : selector.tags()) {
                tag = tag.trim();
                if (tag.length() == 0) {
                    continue;
                }
                if (tag.startsWith(HealthCheckFilter.OMIT_PREFIX)) {
                    // ommit tags always have to be added as and-clause
                    filterBuilder.append("(!(").append(HealthCheck.TAGS).append("=").append(tag.substring(prefixLen)).append("))");
                } else {
                    // add regular tags in the list either to outer and-clause or inner or-clause
                    if (combineTagsWithOr) {
                        filterBuilderForOrOperator.append("(").append(HealthCheck.TAGS).append("=").append(tag).append(")");
                    } else {
                        tagsBuilder.append("(").append(HealthCheck.TAGS).append("=").append(tag).append(")");
                        tagsAndClauses++;
                    }
                }
            }
        }
        boolean addedNameToOrBuilder = false;
        if (selector.names() != null) {
            for (String name : selector.names()) {
                name = name.trim();
                if (name.length() == 0) {
                    continue;
                }
                if (name.startsWith(HealthCheckFilter.OMIT_PREFIX)) {
                    // ommit tags always have to be added as and-clause
                    filterBuilder.append("(!(").append(HealthCheck.NAME).append("=").append(name.substring(prefixLen)).append("))");
                } else {
                    // names are always ORd
                    filterBuilderForOrOperator.append("(").append(HealthCheck.NAME).append("=").append(name).append(")");
                    addedNameToOrBuilder = true;
                }
            }
        }
        if (addedNameToOrBuilder) {
            if (tagsAndClauses > 1) {
                filterBuilderForOrOperator.append("(&").append(tagsBuilder).append(")");
            } else {
                filterBuilderForOrOperator.append(tagsBuilder);
            }
        } else {
            filterBuilder.append(tagsBuilder);
        }
        // add "or" clause if we have accumulated any
        if (filterBuilderForOrOperator.length() > 0) {
            filterBuilder.append("(|").append(filterBuilderForOrOperator).append(")");
        }
        filterBuilder.append(")");
        return filterBuilder;
    }
}
