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
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to ask {@link org.apache.sling.distribution.agent.DistributionAgent}s to distribute (via HTTP POST).
 */
@SlingServlet(resourceTypes = DistributionResourceTypes.AGENT_RESOURCE_TYPE, methods = "POST")
public class DistributionAgentServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        DistributionRequest distributionRequest = RequestUtils.fromServletRequest(request);

        log.debug("distribution request : {}", distributionRequest);

        DistributionAgent agent = request.getResource().adaptTo(DistributionAgent.class);

        ResourceResolver resourceResolver = request.getResourceResolver();

        if (agent != null) {
            try {
                DistributionResponse distributionResponse = agent.execute(resourceResolver, distributionRequest);

                ServletJsonUtils.writeJson(response, distributionResponse);

                log.debug("distribution response : {}", distributionResponse);
            } catch (Throwable e) {
                log.error("an unexpected error has occurred", e);
                ServletJsonUtils.writeJson(response, 503, "an unexpected error has occurred", null);
            }
        } else {
            ServletJsonUtils.writeJson(response, 404, "agent not found", null);
        }
    }

}
