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
package org.apache.sling.distribution.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;

/**
 * {@link Filter} to avoid creation of duplicate agents.
 */
@Component(immediate = true, metatype = false)
@Service(value = Filter.class)
@Properties({
        @Property(name = "service.description", value = "Duplicate replication agents IDs checking Filter"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation"),
        @Property(name = "sling.filter.scope", value = "request"),
        @Property(name = "sling.filter.pattern", value = "/libs/sling/distribution/settings/agents/.*"),
        @Property(name = "osgi.http.whiteboard.filter.regex", value = "/libs/sling/distribution/settings/agents/.*"),
        @Property(name = "service.ranking", intValue = Integer.MAX_VALUE)
})
public final class DistributionAgentCreationFilter implements Filter {

    private static final String METHOD_POST = "POST";

    private static final String NAME = "name";

    private static final String TYPE = "type";

    private static final String FACTORY_FILTER_PATTERN = "(&(name=%s)(!(type=%s)))";

    private BundleContext context;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SlingHttpServletRequest servletRequest = (SlingHttpServletRequest) request;

        // only intercept POST requests
        if (METHOD_POST.equalsIgnoreCase(servletRequest.getMethod())) {
            String name = request.getParameter(NAME);
            String type = request.getParameter(TYPE);

            if (type != null && name != null) {
                String filter = format(FACTORY_FILTER_PATTERN, name, type);
                try {
                    ServiceReference[] services = context.getAllServiceReferences(DistributionAgent.class.getName(), filter);
                    if (services != null && services.length > 0) {
                        String errorMessage = format("An agent named '%s' of different type than '%s' was already previously registered, please change the Agent name.",
                                name, type);
                        ((HttpServletResponse) response).sendError(SC_CONFLICT, errorMessage);
                        return;
                    }
                } catch (InvalidSyntaxException e) {
                    // should not happen...
                    log.error("Impossible to access to {} references", DistributionAgent.class.getName(), e);
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // do nothing
    }

}
