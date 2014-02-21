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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.impl.ReplicationAgentResource;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to ask {@link ReplicationAgent}s to replicate (via HTTP POST).
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationAgentResource.RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = "POST") })
public class ReplicationAgentServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getHeader(ReplicationHeader.ACTION.toString());

        if(ReplicationActionType.POLL.getName().equalsIgnoreCase(action)){
            doRemove(request, response);
        }
        else {
            doCreate(request, response);
        }
    }

    private void doCreate(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        String action = request.getHeader(ReplicationHeader.ACTION.toString());
        String[] path = toStringArray(request.getHeaders(ReplicationHeader.PATH.toString()));

        ReplicationRequest replicationRequest = new ReplicationRequest(System.currentTimeMillis(),
                ReplicationActionType.valueOf(action), path);

        ReplicationAgent agent = request.getResource().adaptTo(ReplicationAgent.class);

        if (agent != null) {
            try {
                ReplicationResponse replicationResponse = agent.execute(replicationRequest);
                if (replicationResponse.isSuccessful()
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

    private void doRemove(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());

        String queueName = request.getParameter(ReplicationHeader.QUEUE.toString());

        ReplicationAgent agent = request.getResource().adaptTo(ReplicationAgent.class);

        /* directly polling an agent queue is only possible if such an agent doesn't have its own endpoint
        (that is it just adds items to its queue to be polled remotely)*/
        if (agent != null) {
            try {
                // TODO : consider using queue distribution strategy and validating who's making this request
                log.info("getting item from queue {}", queueName);

                // get first item
                ReplicationPackage head = agent.removeHead(queueName);

                if (head != null) {
                    InputStream inputStream = null;
                    int bytesCopied = -1;
                    try {
                        inputStream = head.createInputStream();
                        bytesCopied = IOUtils.copy(inputStream, response.getOutputStream());
                    }
                    finally {
                        IOUtils.closeQuietly(inputStream);
                    }

                    setPackageHeaders(response, head);

                    // delete the package permanently
                    head.delete();

                    log.info("{} bytes written into the response", bytesCopied);

                } else {
                    log.info("nothing to fetch");
                }
            } catch (Exception e) {
                response.setStatus(503);
                log.error("error while reverse replicating from agent", e);
            }
            // everything ok
            response.setStatus(200);
        } else {
            response.setStatus(404);
        }
    }

    String[] toStringArray(Enumeration<String> e){
        List<String> l = new ArrayList<String>();
        while (e.hasMoreElements()){
            l.add(e.nextElement());
        }

        return l.toArray(new String[0]);

    }

    void setPackageHeaders(SlingHttpServletResponse response, ReplicationPackage replicationPackage){
        response.setHeader(ReplicationHeader.TYPE.toString(), replicationPackage.getType());
        response.setHeader(ReplicationHeader.ACTION.toString(), replicationPackage.getAction());
        for (String path : replicationPackage.getPaths()){
            response.setHeader(ReplicationHeader.PATH.toString(), path);
        }

    }
}
