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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.commons.auth.spi.AuthenticationInfo;
import org.apache.sling.engine.SlingServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable that executes a FilterChain, using a ServletResponseWrapper to
 * capture the output.
 */
class BackgroundRequestExecutionJob implements Runnable, JobStatus {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HttpServletRequest request;
    private final BackgroundHttpServletResponse response;
    private final SuspendableOutputStream stream;
    private final ResourceResolver resourceResolver;
    private final SlingServlet slingServlet;
    private final String path;

    BackgroundRequestExecutionJob(SlingServlet slingServlet,
            ResourceResolverFactory rrf, HttpServletRequest request,
            HttpServletResponse hsr, String[] parametersToRemove)
            throws IOException, LoginException {
        this.request = new BackgroundHttpServletRequest(request,
                parametersToRemove);
        this.slingServlet = slingServlet;

        // TODO we might
        // In a normal request the ResourceResolver is added to the request
        // attributes
        // by the authentication service, need to do the same here as we can't
        // reuse the
        // original one which is closed once main request is done
        final AuthenticationInfo aa = (AuthenticationInfo) request
                .getAttribute(AuthenticationInfo.class.getName());
        if (aa == null) {
            throw new IllegalArgumentException(
                    "Missing AuthenticationInfo attribute");
        }
        resourceResolver = rrf.getResourceResolver(aa);

        // TODO write output to the Sling repository. For now: just a temp file
        final File output = File.createTempFile(getClass().getSimpleName(),
                ".data");
        output.deleteOnExit();
        path = output.getAbsolutePath();
        stream = new SuspendableOutputStream(new FileOutputStream(output));
        response = new BackgroundHttpServletResponse(hsr, stream);
    }

    public String toString() {
        return getClass().getSimpleName() + ", state=" + getState() + ", path="
                + path;
    }

    public void run() {
        try {
            slingServlet.processRequest(request, response, resourceResolver);
        } catch (Exception e) {
            // TODO report errors in the background job's output
            log.error("Exception in background request processing", e);
        } finally {
            try {
                response.cleanup();
            } catch (IOException ioe) {
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
