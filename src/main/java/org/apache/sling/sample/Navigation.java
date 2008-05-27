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
package org.apache.sling.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>Navigation</code> class is a very simple navigation component
 * which just draws a (nested) list of child "pages". A child content is
 * considered a "page" if the Content object is an instance of the
 * {@link SamplePage} class.
 *
 * @scr.component immediate="true" metatype="no"
 * @scr.property name="service.description" value="Sample Navigation Component"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/sample.navigation"
 * @scr.property name="sling.servlet.methods" value="html"
 * @scr.service interface="javax.servlet.Servlet"
 */
public class Navigation extends SlingSafeMethodsServlet {

    public static final String RESOURCE_TYPE = "sling/sample.navigation";

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
        listChildren(request, response, request.getResource());
    }

    private void listChildren(SlingHttpServletRequest request,
            SlingHttpServletResponse response, Resource current)
            throws ServletException, IOException {

        // / the children of the current content, terminate if there are none
        Iterator<Resource> children = request.getResourceResolver().listChildren(
            current);
        if (!children.hasNext()) {
            return;
        }

        // to not draw the link to the content of the current page, we
        // retrieve the path of the page level content
        Resource requestContent = (Resource) request.getAttribute(SlingConstants.ATTR_REQUEST_CONTENT);
        String requestPath = requestContent.getPath();

        PrintWriter pw = response.getWriter();
        pw.println("<ul>");

        while (children.hasNext()) {
            Resource childResource = children.next();

            // if the child is a page, add an entry with optional link and
            // recursively call this method to draw the children of the child
            SamplePage page = childResource.adaptTo(SamplePage.class);
            if (page != null) {
                String title = page.getTitle();
                pw.print("<li>");

                if (page.getPath().equals(requestPath)) {
                    pw.print(title);
                } else {
                    pw.print("<a href=\"" + request.getContextPath() + page.getPath() + ".html\">");
                    pw.print(title);
                    pw.print("</a>");
                }
                pw.println("</li>");

                // children of the current page, too
                listChildren(request, response, childResource);
            }
        }

        pw.println("</ul>");
    }
}
