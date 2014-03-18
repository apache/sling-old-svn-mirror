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

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.resources.ReplicationConstants;

/**
 * Servlet to retrieve a {@link org.apache.sling.replication.queue.ReplicationQueue} status.
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationConstants.AGENT_QUEUE_RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = { "GET", "POST", "DELETE" } ) })
public class ReplicationAgentQueueServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String queueName = request.getParameter(ReplicationHeader.QUEUE.toString());

        ReplicationAgent agent = request.getResource().adaptTo(ReplicationAgent.class);

        if (agent != null) {
            try {
                ReplicationQueue queue = agent.getQueue("");

                response.getWriter().write(toJSoN(queue));
            } catch (Exception e) {
                response.getWriter().write("{\"status\" : \"error\",\"message\":\"error reading from the queue\"}");
            }
        } else {
            response.getWriter().write("{\"status\" : \"error\",\"message\":\"queue not found\"}");
        }
    }


    @Override
     protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        @SuppressWarnings("unchecked")
        String operation = request.getParameter(":operation");

        if("delete".equals(operation)) {
            doDelete(request, response);
            return;
        };
    }


    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        ReplicationQueue queue = request.getResource().adaptTo(ReplicationQueue.class);

        while(!queue.isEmpty()){
            queue.removeHead();
        }
    }

    private String toJSoN(ReplicationQueue queue) throws Exception {
        StringBuilder builder = new StringBuilder("{\"name\":\"" + queue.getName() + "\",\"empty\":" + queue.isEmpty());
        if (!queue.isEmpty()) {
            builder.append(",\"items\":[");
            for (ReplicationQueueItem item : queue.getItems()) {
                builder.append('{');
                builder.append(toJSoN(item));
                builder.append(',');
                builder.append(toJSoN(queue.getStatus(item)));
                builder.append("},");
            }
            if (queue.getItems().size() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }

    private String toJSoN(ReplicationQueueItemState status) {
        return "\"attempts\":" + status.getAttempts() + ",\"state\":\"" + status.getItemState().name() + "\",\"entered\":\"" + status.getEntered().getTime() + "\"";
    }

    private String toJSoN(ReplicationQueueItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"id\":\"").append(item.getId().replace("\\", "\\\\"));
        builder.append("\",\"paths\":[");
        for (int i = 0; i < item.getPaths().length; i++) {
            builder.append("\"");
            builder.append(item.getPaths()[i]);
            builder.append("\",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(']');
        builder.append(",\"action\":\"").append(item.getAction());
        builder.append("\",\"type\":\"").append(item.getType());
        builder.append("\"");
        return builder.toString();
    }
}
