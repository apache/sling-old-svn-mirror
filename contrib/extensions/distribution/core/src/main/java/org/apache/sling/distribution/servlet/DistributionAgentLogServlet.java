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
import java.io.PrintWriter;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to ask {@link org.apache.sling.distribution.agent.DistributionAgent}s to distribute (via HTTP POST).
 */
@SuppressWarnings("serial")
@SlingServlet(resourceTypes = DistributionResourceTypes.LOG_RESOURCE_TYPE, methods = "GET", extensions = "txt")
public class DistributionAgentLogServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");

        DistributionRequest distributionRequest = RequestUtils.fromServletRequest(request);

        log.debug("distribution request : {}", distributionRequest);

        DistributionLog distributionLog = request.getResource().adaptTo(DistributionLog.class);
        PrintWriter writer = response.getWriter();

        if (distributionLog != null) {
            for (String line : distributionLog.getLines()) {
                writer.append(line);
                writer.append("\n");
            }
        } else {
            response.setStatus(404);
            writer.append("agent not found");
        }
    }
}
