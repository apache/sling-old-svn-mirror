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
package org.apache.sling.engine.impl.debug;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.engine.impl.request.SlingRequestProgressTracker;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter that dumps the output of the RequestProgressTracker to the log after
 * processing the request.
 *
 */
@Designate(ocd=RequestProgressTrackerLogFilter.Config.class)
@Component(service=Filter.class,
           property={
                   Constants.SERVICE_DESCRIPTION + "=RequestProgressTracker dump filter",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
                   EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST
           })
public class RequestProgressTrackerLogFilter implements Filter {

    @ObjectClassDefinition(name="Apache Sling Request Progress Tracker Log Filter",
            description="Filter that enables logging of request progress tracker " +
                        "information. To enable the log output, the category " +
                        "org.apache.sling.engine.impl.debug.RequestProgressTrackerLogFilter needs to be " +
                        "logged on debug level.")
    public static @interface Config {
        @AttributeDefinition(name="Extension filter",
                             description="Only requests with the listed extensions will be logged. " +
                                         "If no extensions are configured all requests are logged. Empty by default.")
        String[] extensions() default {};

        @AttributeDefinition(name="Min duration (ms)",
                description="Only requests that take at least the minimum duration " +
                            "in milliseconds are logged. Default is 0.")
        int minDurationMs() default 0;

        @AttributeDefinition(name="Max duration (ms)",
                             description="Only requests that take at most the maximum duration " +
                                         "in milliseconds are logged. Default is 2147483647, i.e. Integer.MAX_VALUE.")
        int maxDurationMs() default Integer.MAX_VALUE;

        @AttributeDefinition(name="Compact Log Format",
                description="Whether or not to use the compact format. In compact " +
                            "one log entry is logged per request, detailing the request progress tracker " +
                            "information in individual lines, like stack-traces. This keeps log files smaller " +
                            "and makes them more readable. In the older (non-compact) format, one log entry is " +
                            "printed per line, thus potentially containing more noise. Default is false.")
        boolean compactLogFormat() default false;
    }

    private final Logger log = LoggerFactory.getLogger(RequestProgressTrackerLogFilter.class);

    private int requestCounter;

    private Config configuration;

    private String[] extensions;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);

        if (request instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            final RequestProgressTracker rpt = slingRequest.getRequestProgressTracker();
            rpt.done();

            if (log.isDebugEnabled() && allowDuration(rpt) && allowExtension(extractExtension(slingRequest))) {
                if (configuration.compactLogFormat()) {
                    logCompactFormat(rpt);
                } else {
                    logDefaultFormat(rpt);
                }
            }
        }
    }

    @Override
    public void destroy() {
    }

    private void logCompactFormat(RequestProgressTracker rpt) {
        final Iterator<String> messages = rpt.getMessages();
        final StringBuilder sb = new StringBuilder("\n");
        while (messages.hasNext()) {
            sb.append(messages.next());
        }
        sb.setLength(sb.length() - 1);
        log.debug(sb.toString());
    }

    private void logDefaultFormat(RequestProgressTracker rpt) {
        int requestId = 0;
        synchronized (getClass()) {
            requestId = ++requestCounter;
        }
        final Iterator<String> it = rpt.getMessages();
        while (it.hasNext()) {
            log.debug("REQUEST_{} - " + it.next(), requestId);
        }
    }

    private String extractExtension(SlingHttpServletRequest request) {
        final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String extension = requestPathInfo.getExtension();
        if (extension == null) {
            final String pathInfo = requestPathInfo.getResourcePath();
            final int extensionIndex = pathInfo.lastIndexOf('.') + 1;
            extension = pathInfo.substring(extensionIndex);
        }
        return extension;
    }

    private boolean allowExtension(final String extension) {
        return extensions == null
               || extensions.length == 0
               || Arrays.binarySearch(extensions, extension) > -1;
    }

    private boolean allowDuration(final RequestProgressTracker rpt) {
        if (rpt instanceof SlingRequestProgressTracker) {
            long duration = ((SlingRequestProgressTracker) rpt).getDuration();
            return configuration.minDurationMs() <= duration && duration <= configuration.maxDurationMs();
        } else {
            log.debug("Logged requests can only be filtered by duration if the SlingRequestProgressTracker is used.");
            return true;
        }

    }

    /**
     * Sorts the String array and removes any empty values, which are
     * assumed to be sorted to the beginning.
     *
     * @param strings An array of Strings
     * @return The sorted array with empty strings removed.
     */
    private String[] sortAndClean(String[] strings) {
        if (strings == null || strings.length == 0) {
            return strings;
        }

        Arrays.sort(strings);
        int skip = 0;
        for (int i = strings.length - 1; i > -1; i--) {
            // skip all empty strings that are sorted to the beginning of the array
            if (strings[i].length() == 0) {
                skip++;
            }
        }
        return skip == 0 ? strings : Arrays.copyOfRange(strings, skip, strings.length);
    }

    @Activate
    private void activate(final Config config) {
        this.configuration = config;
        // extensions needs to be sorted for Arrays.binarySearch() to work
        this.extensions = sortAndClean(this.configuration.extensions());
        log.debug("activated: extensions = {}, min = {}, max = {}, compact = {}",
                new Object[]{extensions, configuration.minDurationMs(), configuration.maxDurationMs(), configuration.compactLogFormat()});
    }
}