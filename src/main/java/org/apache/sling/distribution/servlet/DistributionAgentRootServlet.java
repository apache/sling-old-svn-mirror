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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;
import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.resources.DistributionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for aggregate distribution on all agents
 */
@SlingServlet(resourceTypes = DistributionConstants.AGENT_ROOT_RESOURCE_TYPE, methods = "POST")
public class DistributionAgentRootServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PATH_PARAMETER = "path";

    private static final String ACTION_PARAMETER = "action";


    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        DistributionAgent[] agents = request.getResource().adaptTo(DistributionAgent[].class);

        String a = request.getParameter(ACTION_PARAMETER);
        String[] paths = request.getParameterValues(PATH_PARAMETER);

        DistributionActionType action = DistributionActionType.fromName(a);

        DistributionRequest distributionRequest = new DistributionRequest(action, paths);

        ResourceResolver resourceResolver = request.getResourceResolver();

        boolean failed = false;
        for (DistributionAgent agent : agents) {
            try {
                agent.execute(resourceResolver, distributionRequest);
            } catch (DistributionAgentException e) {
                log.warn("agent {} execution failed", agent, e);

                response.getWriter().append("error :'").append(e.toString()).append("'");
                if (!failed) {
                    failed = true;
                }
            }
        }
        if (failed) {
            response.setStatus(503);
            response.getWriter().append("status : ").append("503");
        } else {
            response.setStatus(200);
        }
    }
}
