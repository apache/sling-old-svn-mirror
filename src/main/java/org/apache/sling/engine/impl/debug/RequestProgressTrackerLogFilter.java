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
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Filter that dumps the output of the RequestProgressTracker to the log
 *  after processing the request.
 *  
 *  @scr.component immediate="true" metatype="false"
 *  @scr.property name="service.description" value="RequestProgressTracker dump filter"
 *  @scr.property name="service.vendor" value="The Apache Software Foundation"
 *  @scr.property name="filter.scope" value="request" private="true"
 *  @scr.service
 */
public class RequestProgressTrackerLogFilter implements Filter {
        
    private static final Logger log = LoggerFactory.getLogger(RequestProgressTrackerLogFilter.class);
    private int requestCounter;
    
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    throws IOException, ServletException {
        
        chain.doFilter(request, response);
        
        if(request instanceof SlingHttpServletRequest) {
            final RequestProgressTracker t = ((SlingHttpServletRequest)request).getRequestProgressTracker();
            t.done();
            
            if(log.isDebugEnabled()) {
                int requestId = 0;
                synchronized (getClass()) {
                    requestId = ++requestCounter;
                }
                final Iterator<String> it = t.getMessages();
                while(it.hasNext()) {
                    log.debug("REQUEST_{} - " + it.next(), requestId);
                }
            }
        }
    }

    public void destroy() {
    }

}