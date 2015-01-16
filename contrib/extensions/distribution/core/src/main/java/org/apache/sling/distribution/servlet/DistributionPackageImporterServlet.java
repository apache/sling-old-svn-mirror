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
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle reception of distribution content.
 */
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
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        InputStream stream = request.getInputStream();
        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            DistributionPackage distributionPackage = distributionPackageImporter.importStream(resourceResolver, stream);
            if (distributionPackage != null) {
                log.info("Package {} imported successfully", distributionPackage);
                distributionPackage.delete();
            } else {
                log.warn("Cannot import distribution package from request {}", request);
                response.setStatus(400);
                response.getWriter().print("error: could not import a package from the request stream");
            }
        } catch (final Exception e) {
            response.setStatus(400);
            response.getWriter().print("error: " + e.toString());
            log.error("Error during distribution import: {}", e.getMessage(), e);
        } finally {
            long end = System.currentTimeMillis();
            log.info("Processed package import request in {} ms", end - start);
        }
    }

}
