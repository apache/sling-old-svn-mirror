/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.tools.junit;

import static org.apache.sling.testing.tools.junit.RemoteLogDumper.TEST_CLASS;
import static org.apache.sling.testing.tools.junit.RemoteLogDumper.TEST_NAME;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *  Filter which logs the test class and name being executed, if set in
 *  request headers.
 */
@Component
@Service
@Properties({
    @Property(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, value="/"),
    @Property(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
              value = "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)")
})
public class TestNameLoggingFilter implements Filter{

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to init
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final String className = httpRequest.getHeader(TEST_CLASS);

        if(className == null) {
            filterChain.doFilter(servletRequest,servletResponse);
            return;
        }

        final String testName = httpRequest.getHeader(TEST_NAME);
        try {
            MDC.put(TEST_NAME,testName);
            MDC.put(TEST_CLASS,className);

            log.info("Starting request as part of test ==== {}.{} ====",className,testName);

            filterChain.doFilter(servletRequest,servletResponse);

        } finally {
            log.info("Finishing request as part of test ==== {}.{} ====",className,testName);

            MDC.remove(TEST_NAME);
            MDC.remove(TEST_CLASS);
        }
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }
}
