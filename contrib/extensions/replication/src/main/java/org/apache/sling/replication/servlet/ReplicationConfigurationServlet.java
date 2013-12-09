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
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.apache.sling.replication.agent.impl.ReplicationAgentConfigurationResource;

@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationAgentConfigurationResource.RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = { "POST", "GET" }) })
public class ReplicationConfigurationServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationAgentConfigurationManager agentConfigurationManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
                    throws ServletException, IOException {
        response.setContentType("application/json");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameterMap = request.getParameterMap();
        Resource agentResource = request.getResource().getParent();
        ReplicationAgent agent = agentResource.adaptTo(ReplicationAgent.class);
        ReplicationAgentConfiguration configuration;
        try {
            configuration = agentConfigurationManager.updateConfiguration(agent, parameterMap);
            response.getWriter().write(configuration.toString());
        } catch (AgentConfigurationException e) {
            if (log.isErrorEnabled()) {
                log.error("cannot update configuration for agent {}", agent, e);
            }
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
                    throws ServletException, IOException {
        response.setContentType("application/json");
        Resource resource = request.getResource();
        ReplicationAgentConfiguration configuration = resource
                        .adaptTo(ReplicationAgentConfiguration.class);
        response.getWriter().write(configuration.toString());
    }

}
