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
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to retrieve a {@link org.apache.sling.distribution.queue.DistributionQueue} status.
 */
@SlingServlet(resourceTypes = DistributionResourceTypes.AGENT_QUEUE_RESOURCE_TYPE, methods = {"POST"})
public class DistributionAgentQueueServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        @SuppressWarnings("unchecked")
        String operation = request.getParameter("operation");

        DistributionQueue queue = request.getResource().adaptTo(DistributionQueue.class);

        String limitParam = request.getParameter("limit");
        String[] idParam = request.getParameterValues("id");

        if ("delete".equals(operation)) {

            if (idParam != null) {
                deleteItem(queue, idParam);
            }
            else {
                int limit = 1;
                try {
                    limit = Integer.parseInt(limitParam);
                }
                catch (NumberFormatException ex) {

                }
                deleteItems(queue, limit);
            }
        }
    }

    protected void deleteItems(DistributionQueue queue, int limit) {
       for(DistributionQueueItem item : queue.getItems(0, limit)) {
           queue.remove(item.getId());
       }
    }

    protected void deleteItem(DistributionQueue queue, String[] ids) {
        for(String id : ids) {
            queue.remove(id);
        }
    }
}
