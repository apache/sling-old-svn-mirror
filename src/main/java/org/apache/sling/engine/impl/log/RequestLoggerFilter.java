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
package org.apache.sling.engine.impl.log;

import java.io.IOException;
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
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;

/**
 * The <code>RequestLogger</code> is a request level filter, which
 * provides customizable logging or requests handled by Sling. This filter is
 * inserted as the first filter in the request level filter chain and therefore
 * is the first filter called when processing a request and the last filter
 * acting just before the request handling terminates.
 *
 */
@Component(immediate = true, policy = ConfigurationPolicy.IGNORE)
@Properties({
    @Property(name = "service.description", value = "Request Logger Filter"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation")
})
@Service(value = Filter.class)
@Reference(
        name = "RequestLoggerService",
        referenceInterface = RequestLoggerService.class,
        cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
@Property(name = "pattern", value = "/.*")
public final class RequestLoggerFilter implements Filter {

    private static final RequestLoggerService[] NONE = new RequestLoggerService[0];

    /**
     * The list of {@link RequestLoggerService} called when the request enters
     * processing. The order of the services in this list determined by the
     * registration order.
     */
    private RequestLoggerService[] requestEntry = NONE;

    /**
     * The list of {@link RequestLoggerService} called when the request is about
     * to exit processing. The order of the services in this list determined by
     * the registration order.
     */
    private RequestLoggerService[] requestExit = NONE;

    public void init(FilterConfig filterConfig) {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final RequestLoggerRequest rlreq = new RequestLoggerRequest((HttpServletRequest) request);
        final RequestLoggerResponse rlres = new RequestLoggerResponse((HttpServletResponse) response);

        log(this.requestEntry, rlreq, rlres);
        try {
            chain.doFilter(rlreq, rlres);
        } finally {
            rlres.requestEnd();
            log(this.requestExit, rlreq, rlres);
        }
    }

    public void destroy() {
    }

    // ---------- SCR Integration ----------------------------------------------

    /**
     * Activates this component by setting up the special request entry and exit
     * request loggers and the access logger as configured in the context
     * properties. In addition the <code>FileRequestLog</code> class is
     * initialized with the value of the <code>sling.home</code> context
     * property to resolve relative log file names.
     */
    @Activate
    @SuppressWarnings("unused")
    private void activate(BundleContext bundleContext) {

        // initialize the FileRequestLog with sling.home as the root for
        // relative log file paths
        FileRequestLog.init(bundleContext.getProperty("sling.home"));

    }

    /**
     * Deactivates this component by unbinding and shutting down all loggers
     * setup during activation and finally dispose off the
     * <code>FileRequestLog</code> class to make sure all shared writers are
     * closed.
     */
    @Deactivate
    @SuppressWarnings("unused")
    private void deactivate() {
        // hack to ensure all log files are closed
        FileRequestLog.dispose();
    }

    /**
     * Binds a <code>RequestLoggerService</code> to be used during request
     * filter.
     *
     * @param requestLoggerService The <code>RequestLoggerService</code> to
     *            use.
     */
    @SuppressWarnings("unused")
    private void bindRequestLoggerService(
            RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            this.requestEntry = this.addService(this.requestEntry, requestLoggerService);
        } else {
            this.requestExit = this.addService(this.requestExit, requestLoggerService);
        }
    }

    /**
     * Binds a <code>RequestLoggerService</code> to be used during request
     * filter.
     *
     * @param requestLoggerService The <code>RequestLoggerService</code> to
     *            use.
     */
    @SuppressWarnings("unused")
    private void unbindRequestLoggerService(
            RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            this.requestEntry = this.removeService(this.requestEntry, requestLoggerService);
        } else {
            this.requestExit = this.removeService(this.requestExit, requestLoggerService);
        }
    }

    /**
     * Creates a new list of request logger services from the existing list
     * appending the new logger. This method does not check, whether the logger
     * has already been added or not and so may add the the logger multiple
     * times. It is the responsibility of the caller to make sure to not add
     * services multiple times.
     *
     * @param list The list to add the new service to
     * @param requestLoggerService The service to append to the list
     * @param A new list with the added service at the end.
     */
    private RequestLoggerService[] addService(RequestLoggerService[] list,
            RequestLoggerService requestLoggerService) {
        if (list == NONE) {
            return new RequestLoggerService[] { requestLoggerService };
        }

        // add the service to the list, must not be in the list yet due to
        // the SCR contract
        RequestLoggerService[] newList = new RequestLoggerService[list.length + 1];
        System.arraycopy(list, 0, newList, 0, list.length);
        newList[list.length] = requestLoggerService;

        return newList;
    }

    /**
     * Creates a new list of request logger services from the existing list by
     * removing the named logger. The logger is searched for by referential
     * equality (comparing the object references) and not calling the
     * <code>equals</code> method. If the last element is being removed from
     * the list, <code>NONE</code> is returned instead of an empty list.
     *
     * @param list The list from which the service is to be removed.
     * @param requestLoggerService The service to remove.
     * @return The list without the service. This may be the same list if the
     *         service is not in the list or may be <code>NONE</code> if the
     *         last service has just been removed from the list.
     */
    private RequestLoggerService[] removeService(RequestLoggerService[] list,
            RequestLoggerService requestLoggerService) {

        RequestLoggerService[] newList = NONE;
        for (int i = 0; i < list.length; i++) {
            if (list[i] == requestLoggerService) {
                newList = new RequestLoggerService[list.length - 1];

                // if not first take over the leading elements
                if (i > 0) {
                    System.arraycopy(list, 0, newList, 0, i);
                }

                // if not the last element, shift rest to the left
                if (i < list.length - 1) {
                    System.arraycopy(list, i + 1, newList, 0, newList.length
                        - i);
                }
            }
        }

        // return the new list if at least one entry is contained
        return (newList.length > 0) ? newList : NONE;
    }

    private void log(RequestLoggerService[] services, final RequestLoggerRequest request,
            final RequestLoggerResponse response) {
        for (int i = 0; i < services.length; i++) {
            services[i].log(request, response);
        }
    }
}
