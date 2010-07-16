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
package org.apache.sling.bgservlets.impl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingServlet;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Filter that runs the current request in the background
 * 	if specific request parameters are set.
 *  TODO: define the position of this filter in the chain,
 *  and how do we enforce it?
 */
@Component
@Service
@Properties({
	@Property(name="filter.scope", value="request"),
	@Property(name="filter.order", intValue=java.lang.Integer.MIN_VALUE)
})
public class BackgroundServletStarterFilter implements Filter{

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Reference
	private ExecutionEngine executionEngine;
	
	@Reference
	private SlingServlet slingServlet;
	
	@Reference
	private ResourceResolverFactory resourceResolverFactory;
	
	/** 
	 * Request runs in the background if this request parameter is present 
	 * TODO should be configurable, and maybe use other decision methods */
	public static final String BG_PARAM = "sling:bg";
	private static final String [] PARAM_TO_REMOVE = { BG_PARAM };
	
	public void doFilter(final ServletRequest sreq, final ServletResponse sresp, 
			final FilterChain chain) throws IOException, ServletException {
		if(!(sreq instanceof HttpServletRequest)) {
			throw new ServletException("request is not an HttpServletRequest: " + sresp.getClass().getName());
		}
		if(!(sresp instanceof HttpServletResponse)) {
			throw new ServletException("response is not an HttpServletResponse: " + sresp.getClass().getName());
		}
		final HttpServletRequest request = (HttpServletRequest)sreq;
		final SlingHttpServletRequest slingRequest = 
			(request instanceof SlingHttpServletRequest ? (SlingHttpServletRequest)request : null); 
		final HttpServletResponse response = (HttpServletResponse)sresp; 
		final String bgParam = sreq.getParameter(BG_PARAM); 
		if(Boolean.valueOf(bgParam)) {
			try {
				final BackgroundRequestExecutionJob job = new BackgroundRequestExecutionJob(
						slingServlet, resourceResolverFactory, request, response, PARAM_TO_REMOVE);
				log.debug("{} parameter true, running request in the background ({})", BG_PARAM, job);
				if(slingRequest != null) {
					slingRequest.getRequestProgressTracker().log(
							BG_PARAM + " parameter true, running request in background (" + job + ")");
				}
				executionEngine.queueForExecution(job);
				
				// TODO not really an error, should send a nicer message
				response.sendError(HttpServletResponse.SC_ACCEPTED, "Running request in the background using " + job);
			} catch (org.apache.sling.api.resource.LoginException e) {
				throw new ServletException("LoginException in doFilter", e);
			}
		} else {
			chain.doFilter(sreq, sresp);
		}
	}

	public void destroy() {
	}

	public void init(FilterConfig cfg) throws ServletException {
	}
}