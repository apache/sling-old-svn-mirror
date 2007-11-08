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
package org.apache.sling.core.impl.filter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.apache.sling.core.impl.helper.AbstractServiceReferenceConfig;
import org.apache.sling.core.impl.helper.SlingServletConfig;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ServletBindingFilter implements Filter {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ComponentContext componentContext;

    private FilterConfig filterConfig;

    private Map<ServiceReference, Servlet> servlets = new HashMap<ServiceReference, Servlet>();

    private List<ServiceReference> pendingServlets;

    public synchronized void init(FilterConfig config) {
        this.filterConfig = config;
        registerPendingServlets();
    }

    public synchronized void destroy() {
        unregisterServlets();
        this.filterConfig = null;
    }

    protected Iterator<Servlet> getServlets() {
        return servlets.values().iterator();
    }

    protected abstract void servletBound(ServiceReference reference,
            Servlet servlet);

    protected abstract void servletUnbound(ServiceReference reference,
            Servlet servlet);

    protected synchronized void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;
        registerPendingServlets();
    }

    protected synchronized void deactivate(ComponentContext componentContext) {
        unregisterServlets();
        this.componentContext = null;
    }

    protected synchronized void bindComponent(ServiceReference reference) {

        if (this.filterConfig == null) {

            // not initialized yet, keep the registration pending
            if (this.pendingServlets == null) {
                this.pendingServlets = new LinkedList<ServiceReference>();
            }
            this.pendingServlets.add(reference);

        } else {

            // initalize the servlet
            registerServlet(reference);
        }
    }

    protected synchronized void unbindComponent(ServiceReference reference) {

        // check whether the component is an unintialized one
        if (this.pendingServlets != null) {
            if (this.pendingServlets.remove(reference)) {
                log.debug(
                    "unbindComponent: Component {} pending initialization unbound",
                    reference);
                return;
            }
        }

        // only try "removing" if we are active (initialized and not
        // destroyed)
        unregisterServlet(reference);
    }

    private void registerPendingServlets() {
        if (pendingServlets != null && filterConfig != null
            && componentContext != null) {

            List<ServiceReference> pc = this.pendingServlets;
            this.pendingServlets = null;

            for (ServiceReference reference : pc) {
                registerServlet(reference);
            }
        }

    }

    private void registerServlet(ServiceReference reference) {
        Servlet servlet = (Servlet) componentContext.locateService("Servlets",
            reference);
        if (servlet == null) {
            log.error(
                "bindServlet: Servlet service not available from reference {}",
                reference);
            return;
        }

        String name = AbstractServiceReferenceConfig.getName(reference);
        if (name == null) {
            log.error(
                "registerServlet: Cannot register servlet {} without a servlet name",
                reference);
            return;
        }

        ServletConfig config = createServletConfig(reference, name);
        try {
            servlet.init(config);

            // only register it internally, if initialization succeeds
            log.debug("addComponent: Adding componnent {}", name);
            servlets.put(reference, servlet);
            servletBound(reference, servlet);
        } catch (ComponentException ce) {
            log.error("Component " + name + " failed to initialize", ce);
        } catch (Throwable t) {
            log.error("Unexpected problem initializing component " + name, t);
        }
    }

    private void unregisterServlets() {
        ServiceReference[] refs = servlets.keySet().toArray(
            new ServiceReference[servlets.keySet().size()]);
        for (ServiceReference serviceReference : refs) {
            unregisterServlet(serviceReference);
        }
    }

    private void unregisterServlet(ServiceReference reference) {
        Servlet servlet = servlets.remove(reference);
        if (servlet != null) {

            servletUnbound(reference, servlet);

            String name = servlet.getServletConfig().getServletName();
            log.debug("removeComponent: Component {} removed", name);

            // only call destroy, if init succeeded and hence the component
            // was registered
            try {
                servlet.destroy();
            } catch (Throwable t) {
                log.error("Unexpected problem destroying component " + name, t);
            }
        }
    }

    private ServletConfig createServletConfig(ServiceReference reference,
            String servletName) {
        return new SlingServletConfig(filterConfig.getServletContext(),
            reference, servletName);
    }

}