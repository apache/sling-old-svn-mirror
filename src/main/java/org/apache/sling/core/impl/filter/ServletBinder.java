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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.sling.core.impl.helper.AbstractServiceReferenceConfig;
import org.apache.sling.core.impl.helper.SlingServletConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ServletBinder implements ServiceTrackerCustomizer {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final BundleContext bundleContext;

    private final ServletContext servletContext;

    private final ServiceTracker servletTracker;

    private Servlet[] servletCache = null;
    private int cacheTracker = -1;

    protected ServletBinder(BundleContext bundleContext, ServletContext servletContext, String clazz) {
        this.bundleContext = bundleContext;
        this.servletContext = servletContext;

        this.servletTracker = new ServiceTracker(bundleContext, clazz, this);
        this.servletTracker.open();
    }

    public void dispose() {
        servletTracker.close();
    }

    protected Servlet[] getServlets() {
        if (servletCache == null || cacheTracker != servletTracker.getTrackingCount()) {
            Object[] servletServices = servletTracker.getServices();
            Servlet[] sc = new Servlet[servletServices.length];
            for (int i=0; i < servletServices.length; i++) {
                sc[i] = (Servlet) servletServices[i];
            }
            servletCache = sc;
            cacheTracker = servletTracker.getTrackingCount();
        }
        return servletCache;
    }

    // ---------- ServiceTrackerCustomizer interface --------------------------

    public Object addingService(ServiceReference reference) {
        return registerServlet(reference);
    }

    public void modifiedService(ServiceReference reference, Object service) {
    }

    public void removedService(ServiceReference reference, Object service) {
        unregisterServlet((Servlet) service);
    }


    private Servlet registerServlet(ServiceReference reference) {
        String name = AbstractServiceReferenceConfig.getName(reference);
        if (name == null) {
            log.error(
                "registerServlet: Cannot register servlet {} without a servlet name",
                reference);
            return null;
        }

        Servlet servlet = (Servlet) bundleContext.getService(reference);
        if (servlet == null) {
            log.error(
                "bindServlet: Servlet service not available from reference {}",
                reference);
            return null;
        }

        try {
            servlet.init(new SlingServletConfig(servletContext, reference, name));
            log.debug("registerServlet: Servlet {} added", name);
        } catch (ComponentException ce) {
            log.error("Component " + name + " failed to initialize", ce);
            servlet = null;
        } catch (Throwable t) {
            log.error("Unexpected problem initializing component " + name, t);
            servlet = null;
        } finally {
            // unget the service in cae of initialization failure
            if (servlet == null) {
                bundleContext.ungetService(reference);
            }
        }

        return servlet;
    }

    private void unregisterServlet(Servlet servlet) {
        String name = servlet.getServletConfig().getServletName();
        log.debug("unregisterServlet: Servlet {} removed", name);

        try {
            servlet.destroy();
        } catch (Throwable t) {
            log.error("Unexpected problem destroying component " + name, t);
        }
    }

}