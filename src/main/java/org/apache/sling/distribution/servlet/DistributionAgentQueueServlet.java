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

import javax.servlet.ServletException;
import java.io.IOException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.communication.DistributionParameter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.resources.DistributionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to retrieve a {@link org.apache.sling.distribution.queue.DistributionQueue} status.
 */
@SlingServlet(resourceTypes = DistributionConstants.AGENT_QUEUE_RESOURCE_TYPE, methods = {"GET", "POST", "DELETE"})
public class DistributionAgentQueueServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String queueName = request.getParameter(DistributionParameter.QUEUE.toString());

        DistributionAgent agent = request.getResource().adaptTo(DistributionAgent.class);

        if (agent != null) {
            try {
                DistributionQueue queue = agent.getQueue(queueName != null ? queueName : DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME);
                response.getWriter().write(toJSoN(queue)); // TODO : use json writer
                response.setStatus(200);
            } catch (Exception e) {
                response.setStatus(400);
                response.getWriter().write("{\"status\" : \"error\",\"message\":\"error reading from the queue\",\"reason\":\""
                        + e.getLocalizedMessage() + "\"}");
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

        if ("delete".equals(operation)) {
            doDelete(request, response);
        }
    }


    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        DistributionQueue queue = request.getResource().adaptTo(DistributionQueue.class);

        while (!queue.isEmpty()) {
            DistributionQueueItem queueItem = queue.getHead();
            DistributionQueueItem removedItem = queue.remove(queueItem.getId());
            if (removedItem != null) {
                log.info("item {} removed from the queue {}", removedItem, queue);
                response.setStatus(200);
            } else {
                log.warn("could not remove item {} from the queue {}", queueItem, queue);
                response.setStatus(400);
            }
        }
    }

    private String toJSoN(DistributionQueue queue) throws Exception {
        StringBuilder builder = new StringBuilder("{\"name\":\"" + queue.getName() + "\",\"empty\":" + queue.isEmpty());
        if (!queue.isEmpty()) {
            builder.append(",\"items\":[");
            for (DistributionQueueItem item : queue.getItems(null)) {
                builder.append('{');
                builder.append(toJSoN(item));
                builder.append(',');
                builder.append(toJSoN(queue.getStatus(item)));
                builder.append("},");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(']');
        }
        builder.append('}');
        return builder.toString();
    }

    private String toJSoN(DistributionQueueItemStatus status) {
        StringBuilder builder = new StringBuilder("\"attempts\":" + status.getAttempts() + ",\"state\":\"" +
                status.getItemState().name() + "\"");
        if (status.getEntered() != null) {
            builder.append(",\"entered\":\"").append(status.getEntered().getTime()).append("\"");
        }
        return builder.toString();
    }

    private String toJSoN(DistributionQueueItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"id\":\"").append(item.getId().replace("\\", "\\\\"));
        builder.append("\",\"paths\":[");
        for (int i = 0; i < item.getPackageInfo().getPaths().length; i++) {
            builder.append("\"");
            builder.append(item.getPackageInfo().getPaths()[i]);
            builder.append("\",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(']');
        builder.append(",\"action\":\"").append(item.getPackageInfo().getRequestType());
        builder.append("\",\"type\":\"").append(item.getType());
        builder.append("\"");
        return builder.toString();
    }
}
