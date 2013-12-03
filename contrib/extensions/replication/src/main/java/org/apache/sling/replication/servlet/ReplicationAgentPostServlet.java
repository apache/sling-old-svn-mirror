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

import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.impl.ReplicationAgentResource;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;

/**
 * Servlet to ask {@link ReplicationAgent}s to replicate (via HTTP POST).
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationAgentResource.RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = "POST") })
public class ReplicationAgentPostServlet extends SlingAllMethodsServlet {

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
                    throws ServletException, IOException {

        response.setContentType("application/json");

        String action = request.getParameter("action");
        String[] path = request.getParameterValues("path");

        ReplicationRequest replicationRequest = new ReplicationRequest(System.currentTimeMillis(),
                        ReplicationActionType.valueOf(action), path);

        ReplicationAgent agent = request.getResource().adaptTo(ReplicationAgent.class);

        if (agent != null) {
            try {
                ReplicationResponse replicationResponse = agent.execute(replicationRequest);
                if (replicationResponse.isSuccessfull()
                                || ItemState.DROPPED.toString().equals(
                                                replicationResponse.getStatus())) {
                    response.setStatus(200);
                } else if (ItemState.QUEUED.toString().equals(replicationResponse.getStatus())
                                || ItemState.ACTIVE.toString().equals(
                                                replicationResponse.getStatus())) {
                    response.setStatus(202);
                } else {
                    response.setStatus(400);
                }
                response.getWriter().append(replicationResponse.toString());
            } catch (AgentReplicationException e) {
                response.setStatus(503);
                response.getWriter().append("{\"error\" : \"").append(e.toString()).append("\"}");
            }
        } else {
            response.setStatus(404);
            response.getWriter().append("{\"error\" : \"agent ").append(request.getServletPath())
                            .append(" not found\"}");
        }
    }
}
