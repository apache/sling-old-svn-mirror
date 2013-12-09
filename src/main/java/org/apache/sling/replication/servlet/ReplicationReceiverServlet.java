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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.communication.ReplicationHeader;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle reception of replication content.
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({@Property(name = "sling.servlet.paths", value = "/system/replication/receive"),
        @Property(name = "sling.servlet.methods", value = "POST")})
public class ReplicationReceiverServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationPackageBuilderProvider replicationPackageBuilderProvider;

    @Reference
    private ReplicationEventFactory replicationEventFactory;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        boolean success = false;
        final long start = System.currentTimeMillis();
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        try {
            ReplicationPackage replicationPackage = null;
            ServletInputStream stream = request.getInputStream();
            String typeHeader = request.getHeader(ReplicationHeader.TYPE.toString());
            if (typeHeader != null) {
                ReplicationPackageBuilder replicationPackageBuilder = replicationPackageBuilderProvider.getReplicationPackageBuilder(typeHeader);
                if (replicationPackageBuilder != null) {
                    replicationPackage = replicationPackageBuilder.readPackage(stream, true);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("cannot read streams of type {}", typeHeader);
                    }
                }
            } else {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(stream); // needed to allow for multiple reads
                for (ReplicationPackageBuilder replicationPackageBuilder : replicationPackageBuilderProvider.getAvailableReplicationPackageBuilders()) {
                    try {
                        replicationPackage = replicationPackageBuilder.readPackage(bufferedInputStream, true);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("received stream cannot be read with {}", replicationPackageBuilder);
                        }
                    }
                }
            }

            if (replicationPackage != null) {
                if (log.isInfoEnabled()) {
                    log.info("replication package read and installed for path(s) {}",
                            Arrays.toString(replicationPackage.getPaths()));
                }
                success = true;

                Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
                dictionary.put("replication.action", replicationPackage.getAction());
                dictionary.put("replication.path", replicationPackage.getPaths());
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_INSTALLED, dictionary);
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("could not read a replication package");
                }
            }
        } catch (final Exception e) {
            response.setStatus(400);
            if (log.isErrorEnabled()) {
                log.error("Error during replication: {}", e.getMessage(), e);
            }
            response.getWriter().print("error: " + e.toString());
        } finally {
            final long end = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("Processed replication request in {}ms: : {}", new Object[]{
                        end - start, success});
            }
        }

    }

}
