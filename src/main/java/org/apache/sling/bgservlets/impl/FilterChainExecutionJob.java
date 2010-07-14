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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.bgservlets.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runnable that executes a FilterChain, using 
 * 	a ServletResponseWrapper to capture the output.
 */
class FilterChainExecutionJob implements Runnable, JobStatus {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final FilterChain chain;
	private final BackgroundHttpServletResponse response;
	private final SuspendableOutputStream stream;
	private final String path;
	private final SlingHttpServletRequest request;
	
	FilterChainExecutionJob(FilterChain chain, HttpServletRequest request, HttpServletResponse hsr) throws IOException {
		this.chain = chain;
		this.request = new SlingHttpServletRequestWrapper(new BackgroundHttpServletRequest(request));
		
		// TODO write output to the Sling repository. For now: just a temp file
		final File output = File.createTempFile(getClass().getSimpleName(), ".data");
		output.deleteOnExit();
		path = output.getAbsolutePath();
		stream = new SuspendableOutputStream(new FileOutputStream(output));
		response  = new BackgroundHttpServletResponse(hsr, stream);
	}
	
	public String toString() {
		return getClass().getSimpleName() + ", state=" + getState() + ", path=" + path;
	}
	
	public void run() {
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
	}

	public String getPath() {
		return path;
	}

	public State getState() {
		return stream.getState();
	}

	public void requestStateChange(State s) {
		stream.requestStateChange(s);
	}
}
