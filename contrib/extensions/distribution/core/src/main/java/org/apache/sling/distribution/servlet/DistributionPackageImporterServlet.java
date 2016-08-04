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
import java.io.InputStream;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle reception of distribution content.
 */
@SuppressWarnings("serial")
@SlingServlet(resourceTypes = DistributionResourceTypes.IMPORTER_RESOURCE_TYPE, methods = "POST")
public class DistributionPackageImporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        DistributionPackageImporter distributionPackageImporter = request
                .getResource()
                .adaptTo(DistributionPackageImporter.class);

        final long start = System.currentTimeMillis();
        response.setContentType("application/json");


        InputStream stream = request.getInputStream();
        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            if (request.getParameter("forceError") != null) {
                throw new Exception("manually forced error");
            }

            DistributionPackageInfo distributionPackageInfo = distributionPackageImporter.importStream(resourceResolver, stream);

            long end = System.currentTimeMillis();

            log.info("Package {} imported successfully in {}ms", distributionPackageInfo, end - start);
            ServletJsonUtils.writeJson(response, 200, "package imported successfully", null);

        } catch (final Throwable e) {
            ServletJsonUtils.writeJson(response, 500, "an unexpected error has occurred during distribution import", null);
            log.error("Error during distribution import", e);
        } finally {
            long end = System.currentTimeMillis();
            log.debug("Processed package import request in {} ms", end - start);
        }
    }

}
