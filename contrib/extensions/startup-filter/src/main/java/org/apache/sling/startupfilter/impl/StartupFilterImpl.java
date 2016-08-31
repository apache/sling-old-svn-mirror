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
package org.apache.sling.startupfilter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.startupfilter.StartupFilter;
import org.apache.sling.startupfilter.StartupFilterDisabler;
import org.apache.sling.startupfilter.StartupInfoProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** StartupFilter implementation. Initially registered
 *  as a StartupFilter only, the Filter registration
 *  is dynamic, on-demand. */
@Component(immediate=true, metatype=true)
@Service(value=StartupFilter.class)
public class StartupFilterImpl implements StartupFilter, Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ServiceRegistration<Filter> filterServiceRegistration;
    private BundleContext bundleContext;
    private ServiceTracker<StartupInfoProvider, StartupInfoProvider> providersTracker;
    private int providersTrackerCount = -1;

    private final List<StartupInfoProvider> providers = new ArrayList<StartupInfoProvider>();

    @Property(boolValue=true)
    public static final String ACTIVE_BY_DEFAULT_PROP = "active.by.default";
    private boolean defaultFilterActive;

    public static final String DEFAULT_MESSAGE = "Startup in progress";

    @Property(value=DEFAULT_MESSAGE)
    public static final String DEFAULT_MESSAGE_PROP = "default.message";
    private String defaultMessage;

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
    private volatile StartupFilterDisabler startupFilterDisabler;

    private static final String FRAMEWORK_PROP_MANAGER_ROOT = "felix.webconsole.manager.root";
    static final String DEFAULT_MANAGER_ROOT = "/system/console";
    private String managerRoot;

    /** @inheritDoc */
    @Override
    public void doFilter(ServletRequest request, ServletResponse sr, FilterChain chain) throws IOException, ServletException {

        // Disable if a StartupFilterDisabler is present
        if(startupFilterDisabler!= null) {
            log.info("StartupFilterDisabler service present, disabling StartupFilter ({})",
                    startupFilterDisabler.getReason());
            disable();
            chain.doFilter(request, sr);
            return;
        }

        // Bypass for the managerRoot path
        if(request instanceof HttpServletRequest) {
            final String pathInfo = ((HttpServletRequest)request).getPathInfo();
            if(managerRoot != null && managerRoot.length() > 0 && pathInfo.startsWith(managerRoot)) {
                log.debug("Bypassing filter for path {} which starts with {}", pathInfo, managerRoot);
                chain.doFilter(request, sr);
                return;
            }
        }

        updateProviders();

        final StringBuilder sb = new StringBuilder();
        sb.append(defaultMessage);
        for(StartupInfoProvider p : providers) {
            sb.append('\n');
            sb.append(p.getProgressInfo());
        }

        // Do not use setError to avoid triggering the container's error page,
        // as that might cascade other errors during startup
        final HttpServletResponse response = (HttpServletResponse)sr;
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(sb.toString());
        response.getWriter().flush();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + (isEnabled() ? "enabled" : "disabled");
    }

    /** @inheritDoc */
    @Override
    public void destroy() {
    }

    /** @inheritDoc */
    @Override
    public void init(FilterConfig cfg) throws ServletException {
    }

    /** If needed, update our list of providers */
    private void updateProviders() {
        if(providersTracker.getTrackingCount() != providersTrackerCount) {
            synchronized(this) {
                if(providersTracker.getTrackingCount() != providersTrackerCount) {
                    providers.clear();
                    final ServiceReference<StartupInfoProvider> [] refs = providersTracker.getServiceReferences();
                    if(refs != null) {
                        for(ServiceReference<StartupInfoProvider> ref : refs) {
                            providers.add(bundleContext.getService(ref));
                        }
                    }
                }
                providersTrackerCount = providersTracker.getTrackingCount();
                log.info("Reloaded list of StartupInfoProvider: {}", providers);
            }
        }
    }

    @Activate
    protected void activate(final BundleContext ctx, final Map<String, Object> properties) throws InterruptedException {
        bundleContext = ctx;

        providersTracker = new ServiceTracker(bundleContext, StartupInfoProvider.class, null);
        providersTracker.open();

        Object prop = properties.get(DEFAULT_MESSAGE_PROP);
        defaultMessage = prop == null ? DEFAULT_MESSAGE : prop.toString();

        prop = properties.get(ACTIVE_BY_DEFAULT_PROP);
        defaultFilterActive = (prop instanceof Boolean ? (Boolean)prop : false);

        prop = bundleContext.getProperty(FRAMEWORK_PROP_MANAGER_ROOT);
        managerRoot = prop == null ? DEFAULT_MANAGER_ROOT : prop.toString();

        if(defaultFilterActive) {
            enable();
        }
        log.info("Activated, enabled={}, managerRoot={}", isEnabled(), managerRoot);
    }

    @Deactivate
    protected void deactivate() throws InterruptedException {
        disable();
        providersTracker.close();
        providersTracker = null;
        bundleContext = null;
    }


    @Override
    public synchronized void enable() {
        if (filterServiceRegistration == null) {
            final String pattern = "/";
            final Hashtable<String, Object> params = new Hashtable<String, Object>();
            params.put(Constants.SERVICE_RANKING, 0x9000); // run before RequestLoggerFilter (0x8000)
            params.put("sling.filter.scope", "REQUEST");
            params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                    "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)");
            params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, pattern);
            filterServiceRegistration = bundleContext.registerService(Filter.class, this, params);
            log.info("Registered {} as a servlet filter service with pattern {}", this, pattern);
        }
    }

    @Override
    public synchronized void disable() {
        if (filterServiceRegistration != null) {
            filterServiceRegistration.unregister();
            filterServiceRegistration = null;
            log.info("Filter service disabled");
        }
    }

    @Override
    public synchronized boolean isEnabled() {
        return filterServiceRegistration != null;
    }
}
