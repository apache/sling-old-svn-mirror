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

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to retrieve a {@link org.apache.sling.distribution.queue.DistributionQueue} status.
 */
@SlingServlet(resourceTypes = DistributionResourceTypes.AGENT_QUEUE_RESOURCE_TYPE, methods = {"POST"})
public class DistributionAgentQueueServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    DistributionPackageBuilderProvider packageBuilderProvider;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        @SuppressWarnings("unchecked")
        String operation = request.getParameter("operation");

        DistributionQueue queue = request.getResource().adaptTo(DistributionQueue.class);


        ResourceResolver resourceResolver = request.getResourceResolver();


        if ("delete".equals(operation)) {
            String limitParam = request.getParameter("limit");
            String[] idParam = request.getParameterValues("id");

            if (idParam != null) {
                deleteItems(resourceResolver, queue, idParam);
            } else {
                int limit = 1;
                try {
                    limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException ex) {
                    log.warn("limit param malformed : "+limitParam, ex);
                }
                deleteItems(resourceResolver, queue, limit);
            }
        } else if ("copy".equals(operation)) {
            String sourceQueue = request.getParameter("queuePath");
            String[] idParam = request.getParameterValues("id");

            if (idParam != null) {
                addItems(resourceResolver, queue, sourceQueue, idParam);
            }
        }
    }

    private void addItems(ResourceResolver resourceResolver, DistributionQueue targetQueue, String queuePath, String[] ids) {
        DistributionQueue sourceQueue = null;
        Resource resource = resourceResolver.getResource(queuePath);

        if (resource != null) {
            sourceQueue = resource.adaptTo(DistributionQueue.class);
        }

        if (sourceQueue == null) {
            log.warn("cannot find source queue {}", queuePath);
        }

        for (String id: ids) {
            DistributionQueueEntry entry = sourceQueue.getItem(id);
            if (entry != null) {
                targetQueue.add(new DistributionQueueItem(id, entry.getItem()));
                DistributionPackage distributionPackage = getPackage(resourceResolver, entry.getItem());
                DistributionPackageUtils.acquire(distributionPackage, targetQueue.getName());
            }
        }
    }

    protected void deleteItems(ResourceResolver resourceResolver, DistributionQueue queue, int limit) {
        for (DistributionQueueEntry item : queue.getItems(0, limit)) {
            deleteItem(resourceResolver, queue, item);
        }
    }

    protected void deleteItems(ResourceResolver resourceResolver, DistributionQueue queue, String[] ids) {
        for (String id : ids) {
            DistributionQueueEntry item = queue.getItem(id);
            deleteItem(resourceResolver, queue, item);
        }
    }

    protected void deleteItem(ResourceResolver resourceResolver, DistributionQueue queue, DistributionQueueEntry entry) {
        DistributionQueueItem item = entry.getItem();
        String id = item.getId();
        queue.remove(id);

        DistributionPackage distributionPackage = getPackage(resourceResolver, item);
        DistributionPackageUtils.releaseOrDelete(distributionPackage, queue.getName());
    }

    DistributionPackage getPackage(ResourceResolver resourceResolver, DistributionQueueItem item) {
        DistributionPackageInfo info = DistributionPackageUtils.fromQueueItem(item);
        String type = info.getType();

        DistributionPackageBuilder packageBuilder = packageBuilderProvider.getPackageBuilder(type);

        if (packageBuilder != null) {

            try {
                DistributionPackage distributionPackage = packageBuilder.getPackage(resourceResolver, item.getId());

                return distributionPackage;
            } catch (DistributionException e) {
                log.error("cannot get package", e);
            }
        }

        return null;
    }
}
