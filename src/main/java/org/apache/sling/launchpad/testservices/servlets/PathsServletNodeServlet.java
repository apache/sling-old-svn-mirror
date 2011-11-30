/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * Servlet which can create and delete a node at /testing/PathsServlet/foo in
 * order to test case where a servlet is registered at a path for which a node
 * exists.
 */
@SuppressWarnings("serial")
@Component
@Service
@Properties({ @Property(name = "service.description", value = "Paths Servlet Node Servlet"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation"),
        @Property(name = "sling.servlet.paths", value = "/testing/PathsServletNodeServlet") })
public class PathsServletNodeServlet extends SlingAllMethodsServlet {
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        String action = request.getParameter("action");
        try {
            Session session = request.getResourceResolver().adaptTo(Session.class);
            if ("create".equals(action)) {
                Node testing = JcrUtils.getOrAddNode(session.getRootNode(), "testing", "nt:unstructured");
                Node servlet = JcrUtils.getOrAddNode(testing, "PathsServlet", "nt:unstructured");
                JcrUtils.getOrAddNode(servlet, "foo", "nt:unstructured");
                if (session.hasPendingChanges()) {
                    session.save();
                    response.setStatus(HttpServletResponse.SC_CREATED);
                }
            } else if ("delete".equals(action)) {
                if (session.nodeExists("/testing/PathsServlet/foo")) {
                    session.getNode("/testing/PathsServlet/foo").remove();
                    if (session.hasPendingChanges()) {
                        session.save();
                    }
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            }

            response.getWriter().println("ok");
        } catch (RepositoryException e) {
            throw new ServletException("Unable to create or delete test node.", e);
        }
    }
}
