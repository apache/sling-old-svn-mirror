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
package org.apache.sling.replication.servlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triggers Server Sent Events servlet
 */
@SlingServlet(resourceTypes = "sling/replication/service/trigger", extensions = "event", methods = "GET")
public class ReplicationTriggerServlet extends SlingAllMethodsServlet {

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

        ReplicationTrigger replicationTrigger = request.getResource().adaptTo(ReplicationTrigger.class);

        // setup SSE headers
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // needed to allow e.g. the JavaScript EventSource API to make a call from author to this server and listen for the events
//        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin")); // allowed origins should be explicitly configured
//        response.setHeader("Access-Control-Allow-Credentials", "true");

        final PrintWriter writer = response.getWriter();

        ReplicationRequestHandler replicationRequestHandler = new ReplicationRequestHandler() {
            public void handle(ReplicationRequest request) {
                writeEvent(writer, request);
            }
        };
        try {
            replicationTrigger.register(replicationRequestHandler);

            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                log.error("thread interrupted", e);
            }

            replicationTrigger.unregister(replicationRequestHandler);

        } catch (ReplicationTriggerException e) {
            response.setStatus(400);
            response.getWriter().write("error while (un)registering trigger " + e.toString());
        }
    }

    /* Write a single server-sent event to the response stream for the given event and message */
    private void writeEvent(PrintWriter writer, ReplicationRequest replicationRequest) {

        // write the event type (make sure to include the double newline)
        writer.write("id: " + replicationRequest.getTime() + "\n");

        // write the actual data
        // this could be simple text or could be JSON-encoded text that the
        // client then decodes
        writer.write("data: " + replicationRequest.getAction() + " " + Arrays.toString(replicationRequest.getPaths()) + "\n\n");

        // flush the buffers to make sure the container sends the bytes
        writer.flush();
        log.debug("SSE event {}: {} {}", new Object[]{replicationRequest.getTime(), replicationRequest.getAction(),
                replicationRequest.getPaths()});
    }
}
