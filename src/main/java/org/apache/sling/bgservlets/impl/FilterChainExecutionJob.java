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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runnable that executes a FilterChain, using 
 * 	a ServletResponseWrapper to capture the output.
 */
class FilterChainExecutionJob implements Runnable {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final FilterChain chain;
	private final ServletResponseWrapper response;
	
	// TODO is it ok to keep a reference to the request until run() is called??
	private final HttpServletRequest request;
	
	FilterChainExecutionJob(FilterChain chain, HttpServletRequest request, HttpServletResponse hsr) throws IOException {
		this.chain = chain;
		this.request = request;
		response  = new ServletResponseWrapper(hsr);
	}
	
	public String toString() {
		return "Background request job: " + response;
	}
	
	public void run() {
		log.info("{} execution starts", this);
		try {
			chain.doFilter(request, response);
		} catch(Exception e) {
			// TODO report errors in the background job's output
			log.error("chain.doFilter failed", e);
		} finally {
			try {
				response.cleanup();
			} catch(IOException ioe) {
				// TODO report errors in the background job's output
				log.error("ServletResponseWrapper cleanup failed", ioe);
			}
		}
		log.info("{} execution ends", this);
	}
}
