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
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.resources.DistributionConstants;
import org.apache.sling.distribution.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle fetching of distribution content.
 */
@SlingServlet(resourceTypes = DistributionConstants.EXPORTER_RESOURCE_TYPE, methods = "POST")
public class DistributionPackageExporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        DistributionPackageExporter distributionPackageExporter = request
                .getResource()
                .adaptTo(DistributionPackageExporter.class);

        final long start = System.currentTimeMillis();

        response.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());

        DistributionRequest distributionRequest = RequestUtils.fromServletRequest(request);
        ResourceResolver resourceResolver = request.getResourceResolver();

        int consumed = 0;
        int fetched = 0;
        try {
            // get all items
            List<DistributionPackage> distributionPackages = distributionPackageExporter.exportPackages(resourceResolver, distributionRequest);
            fetched = distributionPackages.size();

            if (distributionPackages.size() > 0) {
                log.info("{} package(s) available for fetching", distributionPackages.size());

                for (DistributionPackage distributionPackage : distributionPackages) {
                    if (distributionPackage != null) {
                        InputStream inputStream = null;
                        int bytesCopied = -1;
                        try {
                            inputStream = distributionPackage.createInputStream();
                            bytesCopied = IOUtils.copy(inputStream, response.getOutputStream());
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }

                        // delete the package permanently
                        distributionPackage.delete();

                        // everything ok
                        response.setStatus(200);
                        log.info("{} bytes written into the response", bytesCopied);
                    } else {
                        log.warn("fetched a null package");
                    }
                }
            } else {
                response.setStatus(204);
                log.info("nothing to fetch");
            }

        } catch (Exception e) {
            response.setStatus(503);
            log.error("error while exporting from {}", request.getRequestURL(), e);
        } finally {
            long end = System.currentTimeMillis();
            log.info("Processed distribution export request in {} ms: : consumed {} of {}", new Object[]{end - start, consumed, fetched});
        }
    }

}
