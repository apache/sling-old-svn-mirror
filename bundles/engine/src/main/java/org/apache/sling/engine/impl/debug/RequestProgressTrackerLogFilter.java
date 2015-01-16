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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.impl.request.SlingRequestProgressTracker;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;

/**
 * Filter that dumps the output of the RequestProgressTracker to the log after
 * processing the request.
 *
 */
@Component(
        metatype = true,
        immediate = true,
        label = "%rpt.log.name",
        description = "%rpt.log.description"
)
@Service
@Properties({
    @Property(name="service.description", value="RequestProgressTracker dump filter", propertyPrivate = true),
    @Property(name="service.vendor", value="The Apache Software Foundation", propertyPrivate = true),
    @Property(name="filter.scope", value="request", propertyPrivate = true)
})
public class RequestProgressTrackerLogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestProgressTrackerLogFilter.class);

    private int requestCounter;

    private static final String[] DEFAULT_EXTENSIONS = new String[0];
    private String[] extensions = DEFAULT_EXTENSIONS;
    @Property(value = {}, cardinality = Integer.MAX_VALUE)
    private static final String NAME_EXTENSIONS = "extensions";

    private static final int DEFAULT_MIN_DURATION = 0;
    private int minDurationMs = DEFAULT_MIN_DURATION;
    @Property(intValue = DEFAULT_MIN_DURATION)
    private static final String NAME_MIN_DURATION = "minDurationMs";

    private static final int DEFAULT_MAX_DURATION = Integer.MAX_VALUE;
    private int maxDurationMs = DEFAULT_MAX_DURATION;
    @Property(intValue = DEFAULT_MAX_DURATION)
    private static final String NAME_MAX_DURATION = "maxDurationMs";

    private static final boolean DEFAULT_COMPACT_LOG_FORMAT = false;
    private boolean compactLogFormat = DEFAULT_COMPACT_LOG_FORMAT;
    @Property(boolValue = DEFAULT_COMPACT_LOG_FORMAT)
    private static final String NAME_COMPACT_LOG_FORMAT = "compactLogFormat";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response);

        if (request instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            final RequestProgressTracker rpt = slingRequest.getRequestProgressTracker();
            rpt.done();

            if (log.isDebugEnabled() && allowDuration(rpt) && allowExtension(extractExtension(slingRequest))) {
                if (compactLogFormat) {
                    logCompactFormat(rpt);
                } else {
                    logDefaultFormat(rpt);
                }
            }
        }
    }

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
        return extensions.length == 0 || Arrays.binarySearch(extensions, extension) > -1;
    }

    private boolean allowDuration(final RequestProgressTracker rpt) {
        if (rpt instanceof SlingRequestProgressTracker) {
            long duration = ((SlingRequestProgressTracker) rpt).getDuration();
            return minDurationMs <= duration && duration <= maxDurationMs;
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
        if (strings.length == 0) {
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
    private void activate(ComponentContext ctx) {
        final Dictionary p = ctx.getProperties();
        // extensions needs to be sorted for Arrays.binarySearch() to work
        extensions = sortAndClean(PropertiesUtil.toStringArray(p.get(NAME_EXTENSIONS), DEFAULT_EXTENSIONS));
        minDurationMs = PropertiesUtil.toInteger(p.get(NAME_MIN_DURATION), DEFAULT_MIN_DURATION);
        maxDurationMs = PropertiesUtil.toInteger(p.get(NAME_MAX_DURATION), DEFAULT_MAX_DURATION);
        compactLogFormat = PropertiesUtil.toBoolean(p.get(NAME_COMPACT_LOG_FORMAT), DEFAULT_COMPACT_LOG_FORMAT);
        log.debug("activated: extensions = {}, min = {}, max = {}, compact = {}",
                new Object[]{extensions, minDurationMs, maxDurationMs, compactLogFormat});
    }
}