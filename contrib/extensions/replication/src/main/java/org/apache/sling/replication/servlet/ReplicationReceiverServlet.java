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

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * Servlet to handle reception of replication content.
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({ @Property(name = "sling.servlet.paths", value = "/system/replication/receive"),
        @Property(name = "sling.servlet.methods", value = "POST") })
public class ReplicationReceiverServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
                    throws ServletException, IOException {
        boolean success = false;
        final long start = System.currentTimeMillis();
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");
        ReplicationActionType actionType = ReplicationActionType.fromName(request
                        .getHeader("Action"));

        String path = Text.unescape(request.getHeader("Path"));
        try {

            ReplicationPackage replicationPackage = request.adaptTo(ReplicationPackage.class);
            if (replicationPackage != null) {
                if (log.isInfoEnabled()) {
                    log.info("replication package read and installed for path(s) {}",
                                    Arrays.toString(replicationPackage.getPaths()));
                }
                success = true;
            } else {
                if (log.isInfoEnabled()) {
                    log.info("could not read a replication package for path(s) {}", path);
                }
            }
        } catch (final Exception e) {
            response.setStatus(400);
            if (log.isErrorEnabled()) {
                log.error("Error during replication: {}", e.getMessage(), e);
            }
            response.getWriter().print("error: " + e.toString());
        }
        finally {
            final long end = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("Processed replication request in {}ms: {} of {} : {}", new Object[] {
                        end - start, actionType, path, success });
            }
        }

    }

}
