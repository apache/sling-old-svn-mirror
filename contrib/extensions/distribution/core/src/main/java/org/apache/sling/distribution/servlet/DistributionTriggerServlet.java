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
package org.apache.sling.distribution.servlet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triggers Server Sent Events servlet
 */
@SuppressWarnings("serial")
@SlingServlet(resourceTypes = DistributionResourceTypes.TRIGGER_RESOURCE_TYPE, extensions = "event", methods = "GET")
public class DistributionTriggerServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static int DEFAULT_NUMBER_OF_SECONDS = 60;
    private final static int MAX_NUMBER_OF_SECONDS = 3600;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String secondsParameter = request.getParameter("sec");

        int seconds = secondsParameter != null && secondsParameter.length() > 0 ? Integer.parseInt(secondsParameter) :
                DEFAULT_NUMBER_OF_SECONDS;

        if (seconds > MAX_NUMBER_OF_SECONDS) {
            seconds = MAX_NUMBER_OF_SECONDS;
        } else if (seconds < 0) {
            seconds = DEFAULT_NUMBER_OF_SECONDS;
        }

        DistributionTrigger distributionTrigger = request.getResource().adaptTo(DistributionTrigger.class);

        // setup SSE headers
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // needed to allow e.g. the JavaScript EventSource API to make a call from author to this server and listen for the events
//        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin")); // allowed origins should be explicitly configured
//        response.setHeader("Access-Control-Allow-Credentials", "true");

        final PrintWriter writer = response.getWriter();

        DistributionRequestHandler distributionRequestHandler = new DistributionRequestHandler() {
            public void handle(@Nullable ResourceResolver resourceResolver, @Nonnull DistributionRequest request) {
                writeEvent(writer, request);
            }
        };
        try {
            distributionTrigger.register(distributionRequestHandler);

            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                log.error("thread interrupted", e);
            }

            distributionTrigger.unregister(distributionRequestHandler);

        } catch (DistributionException e) {
            response.setStatus(400);
            response.getWriter().write("error while (un)registering trigger " + e.toString());
        }
    }

    /* Write a single server-sent event to the response stream for the given event and message */
    private void writeEvent(PrintWriter writer, DistributionRequest distributionRequest) {

        // write the event type (make sure to include the double newline)
        writer.write("id: " + System.currentTimeMillis() + "\n");

        // write the actual data
        // this could be simple text or could be JSON-encoded text that the
        // client then decodes
        writer.write("data: " + distributionRequest.getRequestType() + " " + Arrays.toString(distributionRequest.getPaths()) + "\n\n");

        // flush the buffers to make sure the container sends the bytes
        writer.flush();
        log.debug("SSE event {} {}", new Object[]{distributionRequest.getRequestType(), distributionRequest.getPaths()});
    }
}
