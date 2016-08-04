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
import java.util.Date;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.bgservlets.BackgroundHttpServletRequest;
import org.apache.sling.bgservlets.BackgroundHttpServletResponse;
import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.JobProgressInfo;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.JobStorage;
import org.apache.sling.bgservlets.RuntimeState;
import org.apache.sling.engine.SlingRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable that executes a FilterChain, using a ServletResponseWrapper to
 * capture the output.
 */
class BackgroundRequestExecutionJob implements Runnable, JobStatus, RuntimeState, JobProgressInfo {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final HttpServletRequest request;
    private final BackgroundHttpServletResponse response;
    private final SuspendableOutputStream stream;
    
    /** ResourceResolver used for the job processing */
    private final ResourceResolver processingResourceResolver;
    
    /** ResourceResolver used for the stats and to save the processing output */
    private final ResourceResolver outputResourceResolver;
    private final SlingRequestProcessor slingRequestProcessor;
    private final String path;
    private final String streamPath;
    private final Date creationTime;
    private Date estimatedCompletionTime;
    private String progressMessage;

    BackgroundRequestExecutionJob(SlingRequestProcessor slingRequestProcessor,
            JobStorage storage, SlingHttpServletRequest request,
            HttpServletResponse hsr, String[] parametersToRemove)
            throws IOException, LoginException {
        this.request = new BackgroundHttpServletRequest(request,
                parametersToRemove);
        this.slingRequestProcessor = slingRequestProcessor;

        // Provide this as the RuntimeState for the background servlet
        this.request.setAttribute(RuntimeState.class.getName(), this);

        // Need a new ResourceResolver with the same credentials as the
        // current request, for the background request.
        processingResourceResolver = request.getResourceResolver().clone(null);
        
        // And a dedicated session for the response object
        outputResourceResolver = request.getResourceResolver().clone(null);

        // Get JobData, defines path and used to save servlet output to the repository
        final Session outputSession = outputResourceResolver.adaptTo(Session.class);
        if(outputSession == null) {
            throw new IOException("Unable to get Session from ResourceResolver " + processingResourceResolver);
        }
        final JobData d = storage.createJobData(outputSession);
        final String ext = request.getRequestPathInfo().getExtension();
        if(ext != null) {
            d.setProperty(JobData.PROP_EXTENSION, ext);
        }
        path = d.getPath();
        creationTime = d.getCreationTime();
        streamPath = d.getPath() + STREAM_PATH_SUFFIX + (ext == null ? "" : "." + ext);
        stream = new SuspendableOutputStream(d.getOutputStream());
        response = new BackgroundHttpServletResponse(hsr, stream);
    }

    public String toString() {
        return getClass().getSimpleName() + ", state=" + getState() + ", path="
                + path;
    }

    public void run() {
        try {
            slingRequestProcessor.processRequest(request, response, processingResourceResolver);
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

            // cleanup the resource resolvers
            processingResourceResolver.close();
            outputResourceResolver.close();
        }
    }

    /** @inheritDoc */
    public String getPath() {
        return path;
    }

    /** @inheritDoc */
    public State getState() {
        return stream.getState();
    }

    /** @inheritDoc */
    public void requestStateChange(State s) {
        stream.requestStateChange(s);
    }

    /** @inheritDoc */
    public State[] getAllowedHumanStateChanges() {
        return stream.getAllowedHumanStateChanges();
    }

    public String getStreamPath() {
        return streamPath;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    /** @inheritDoc */
    public JobProgressInfo getProgressInfo() {
        return this;
    }

    /** @inheritDoc */
    public String getProgressMessage() {
        return progressMessage;
    }

    /** @inheritDoc */
    public Date getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public void setEstimatedCompletionTime(Date d) {
        estimatedCompletionTime = d;
    }

    public void setProgressMessage(String str) {
        progressMessage = str;
    }
}