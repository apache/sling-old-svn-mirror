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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.resources.ReplicationConstants;
import org.apache.sling.replication.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle fetching of replication content.
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes", value = ReplicationConstants.EXPORTER_RESOURCE_TYPE),
        @Property(name = "sling.servlet.methods", value = "POST")})
public class ReplicationPackageExporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ReplicationPackageExporter replicationPackageExporter = request
                .getResource()
                .adaptTo(ReplicationPackageExporter.class);

        boolean success = false;
        final long start = System.currentTimeMillis();

        response.setContentType(ContentType.APPLICATION_OCTET_STREAM.toString());

        ReplicationRequest replicationRequest = RequestUtils.fromServletRequest(request);
        ResourceResolver resourceResolver = request.getResourceResolver();

        try {
            // get first item
            List<ReplicationPackage> replicationPackages = replicationPackageExporter.exportPackage(resourceResolver, replicationRequest);

            if (replicationPackages.size() > 0) {
                log.info("{} package(s) available for fetching, the first will be delivered", replicationPackages.size());

                ReplicationPackage replicationPackage = replicationPackages.get(0);
                if (replicationPackage != null) {
                    InputStream inputStream = null;
                    int bytesCopied = -1;
                    try {
                        inputStream = replicationPackage.createInputStream();
                        bytesCopied = IOUtils.copy(inputStream, response.getOutputStream());
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                    }

                    // delete the package permanently
                    replicationPackage.delete();

                    // everything ok
                    response.setStatus(200);
                    log.info("{} bytes written into the response", bytesCopied);
                    success = true;
                } else {
                    log.warn("fetched a null package");
                }
            } else {
                response.setStatus(204);
                log.info("nothing to fetch");
            }

        } catch (Exception e) {
            response.setStatus(503);
            log.error("error while reverse replicating from agent", e);
        } finally {
            final long end = System.currentTimeMillis();
            log.info("Processed replication export request in {}ms: : {}", new Object[]{end - start, success});
        }
    }

}
