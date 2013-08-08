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

import static org.apache.sling.testing.tools.junit.TestDescriptionInterceptor.TEST_CLASS_HEADER;
import static org.apache.sling.testing.tools.junit.TestDescriptionInterceptor.TEST_NAME_HEADER;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *  Filter which logs the test class and name being executed, if set in
 *  request headers.
 */
@Component
@Service
@Property(name = "pattern",value="/.*")
public class TestNameLoggingFilter implements Filter{

    private Logger log = LoggerFactory.getLogger(getClass());

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to init
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final String className = httpRequest.getHeader(TEST_CLASS_HEADER);
        
        if(className == null) {
            filterChain.doFilter(servletRequest,servletResponse);
            return;
        }
        
        final String testName = httpRequest.getHeader(TEST_NAME_HEADER);
        try {
            MDC.put(TEST_NAME_HEADER,testName);
            MDC.put(TEST_CLASS_HEADER,className);

            log.info("Starting request as part of test ==== {}.{} ====",className,testName);

            filterChain.doFilter(servletRequest,servletResponse);

        } finally {
            log.info("Finishing request as part of test ==== {}.{} ====",className,testName);

            MDC.remove(TEST_NAME_HEADER);
            MDC.remove(TEST_CLASS_HEADER);
        }
    }

    public void destroy() {
        // nothing to destroy
    }
}
