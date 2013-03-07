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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * Test Servlet which outputs the current namespace mappings.
 */
@SlingServlet(
        paths="/testing/NamespaceTestServlet/output"
        )
@SuppressWarnings("serial")
public class NamespaceTestServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        try {
            response.setContentType("text/plain");
            Session session = request.getResourceResolver().adaptTo(Session.class);

            response.getWriter().printf("userid=%s", session.getUserID());
            response.getWriter().println();

            for (String prefix : session.getNamespacePrefixes()) {
                response.getWriter().printf("%s=%s", prefix, session.getNamespaceURI(prefix));
                response.getWriter().println();
            }
            response.getWriter().flush();
        } catch (RepositoryException e) {
            throw new ServletException("Unable to output namespace mappings", e);
        }
    }

}
