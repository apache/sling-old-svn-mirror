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

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.entity.ContentType;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.communication.ReplicationParameter;
import org.apache.sling.replication.resources.ReplicationConstants;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Servlet to handle reception of replication content.
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


        try {
            // get first item
            ReplicationPackage replicationPackage = replicationPackageExporter.exportPackage(null);

            if (replicationPackage != null) {
                InputStream inputStream = null;
                int bytesCopied = -1;
                try {
                    inputStream = replicationPackage.createInputStream();
                    bytesCopied = IOUtils.copy(inputStream, response.getOutputStream());
                }
                finally {
                    IOUtils.closeQuietly(inputStream);
                }

                setPackageHeaders(response, replicationPackage);

                // delete the package permanently
                replicationPackage.delete();

                log.info("{} bytes written into the response", bytesCopied);

            } else {
                log.info("nothing to fetch");
            }
            // everything ok
            response.setStatus(200);
        } catch (Exception e) {
            response.setStatus(503);
            log.error("error while reverse replicating from agent", e);
        }
        finally {
            final long end = System.currentTimeMillis();
            log.info("Processed replication export request in {}ms: : {}", new Object[]{end - start, success});
        }
    }

    void setPackageHeaders(SlingHttpServletResponse response, ReplicationPackage replicationPackage){
        response.setHeader(ReplicationHeader.TYPE.toString(), replicationPackage.getType());
        response.setHeader(ReplicationHeader.ACTION.toString(), replicationPackage.getAction());
        for (String path : replicationPackage.getPaths()){
            response.setHeader(ReplicationHeader.PATH.toString(), path);
        }
    }

}
