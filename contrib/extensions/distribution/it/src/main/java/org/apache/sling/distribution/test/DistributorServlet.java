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
package org.apache.sling.distribution.test;


import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;

import javax.servlet.ServletException;
import java.io.IOException;

@SlingServlet(paths = "/bin/test/distributor")
public class DistributorServlet extends SlingAllMethodsServlet {


    @Reference
    Distributor distributor;


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");
        String agentName = request.getParameter("agent");
        String[] paths = request.getParameterValues("path");

        if (agentName == null) {
            response.getWriter().print("agent is required");
            return;
        }

        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.fromName(action),
                paths);

        DistributionResponse distributionResponse = distributor.distribute(agentName, request.getResourceResolver(), distributionRequest);

        if (distributionResponse.isSuccessful()) {
            response.setStatus(200);
        }
        else {
            response.setStatus(400);
        }
    }
}
