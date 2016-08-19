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

import org.apache.sling.engine.impl.SlingMainServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.LoggerFactory;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = Filter.class,
           property = {
                   HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT+ "=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + SlingMainServlet.SERVLET_CONTEXT_NAME + ")",
                   HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/",
                   Constants.SERVICE_RANKING + ":Integer=32768",
                   Constants.SERVICE_DESCRIPTION + "=Request Logger Filter",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public final class RequestLoggerFilter implements Filter {

    private static final RequestLoggerService[] NONE = new RequestLoggerService[0];

    private RequestLoggerService[] requestEntry = NONE;

    private RequestLoggerService[] requestExit = NONE;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
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

    @Override
    public void destroy() {
        FileRequestLog.dispose();
    }

    // ---------- SCR Integration ----------------------------------------------

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
    private void bindRequestLoggerService(RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            this.requestEntry = this.addService(this.requestEntry, requestLoggerService);
        } else {
            this.requestExit = this.addService(this.requestExit, requestLoggerService);
        }
    }

    @SuppressWarnings("unused")
    private void unbindRequestLoggerService(RequestLoggerService requestLoggerService) {
        if (requestLoggerService.isOnEntry()) {
            this.requestEntry = this.removeService(this.requestEntry, requestLoggerService);
        } else {
            this.requestExit = this.removeService(this.requestExit, requestLoggerService);
        }
    }

    private RequestLoggerService[] addService(RequestLoggerService[] list, RequestLoggerService requestLoggerService) {
        if (list.length == 0) {
            return new RequestLoggerService[] {
                requestLoggerService
            };
        }

        // add the service to the list, must not be in the list yet due to
        // the SCR contract
        RequestLoggerService[] newList = new RequestLoggerService[list.length + 1];
        System.arraycopy(list, 0, newList, 0, list.length);
        newList[list.length] = requestLoggerService;

        return newList;
    }

    private RequestLoggerService[] removeService(RequestLoggerService[] list, RequestLoggerService requestLoggerService) {

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
                    System.arraycopy(list, i + 1, newList, 0, newList.length - i);
                }
            }
        }

        // return the new list if at least one entry is contained
        return (newList.length > 0) ? newList : NONE;
    }

    private void log(RequestLoggerService[] services, final RequestLoggerRequest request,
            final RequestLoggerResponse response) {
        for (RequestLoggerService service : services) {
            try {
                service.log(request, response);
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).debug("log: RequestLoggerService failed logging", e);
            }
        }
    }
}
