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
import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.startupfilter.StartupFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** StartupFilter implementation. Initially registered
 *  as a StartupFilter only, the Filter registration
 *  is dynamic, on-demand. */
@Component(immediate=true, metatype=true)
@Service(value=StartupFilter.class)
public class StartupFilterImpl implements StartupFilter, Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ServiceRegistration filterServiceRegistration;
    private BundleContext bundleContext;
    private final Stack<ProgressInfoProvider> providers = new Stack<ProgressInfoProvider>();
    
    @Property(boolValue=true)
    public static final String DEFAULT_FILTER_ACTIVE_PROP = "default.filter.active";
    private boolean defaultFilterActive;
    
    /** @inheritDoc */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ProgressInfoProvider pip = null;
        synchronized (this) {
            if(!providers.isEmpty()) {
                pip = providers.peek();
            }
        }
        if(pip != null) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, pip.getInfo());
        } else {
            chain.doFilter(request, response);
        }
    }

    /** @inheritDoc */
    public void destroy() {
    }

    /** @inheritDoc */
    public void init(FilterConfig cfg) throws ServletException {
    }

    @Activate
    protected void activate(ComponentContext ctx) throws InterruptedException {
        bundleContext = ctx.getBundleContext();
        defaultFilterActive = (Boolean)ctx.getProperties().get(DEFAULT_FILTER_ACTIVE_PROP);
        if(defaultFilterActive) {
            addProgressInfoProvider(DEFAULT_INFO_PROVIDER);
        }
        log.info("Activated, defaultFilterActive={}", defaultFilterActive);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) throws InterruptedException {
        unregisterFilter();
        bundleContext = null;
    }
    
    
    /** @inheritDoc */
    public synchronized void addProgressInfoProvider(ProgressInfoProvider pip) {
        providers.push(pip);
        log.info("Added {}", pip);
        if(filterServiceRegistration == null) {
            final Hashtable<String, String> params = new Hashtable<String, String>();
            params.put("filter.scope", "REQUEST");
            filterServiceRegistration = bundleContext.registerService(Filter.class.getName(), this, params);
            log.info("Registered {} as a Filter service", this);
        }
    }
    
    /** @inheritDoc */
    public synchronized void removeProgressInfoProvider(ProgressInfoProvider pip) {
        providers.remove(pip);
        log.info("Removed {}", pip);
        if(providers.isEmpty()) {
            log.info("No more ProgressInfoProviders, unregistering Filter service");
            unregisterFilter();
        }
    }
    
    private synchronized void unregisterFilter() {
        if(filterServiceRegistration != null) {
            filterServiceRegistration.unregister();
            filterServiceRegistration = null;
        }
    }
     
}