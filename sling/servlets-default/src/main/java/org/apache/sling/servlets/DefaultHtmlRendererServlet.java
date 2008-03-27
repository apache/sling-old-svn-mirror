/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.helpers.DefaultHtmlRenderer;

/**
 * A SlingSafeMethodsServlet that renders the current Resource as simple HTML
 *
 * @scr.service
 *  interface="javax.servlet.Servlet"
 *
 * @scr.component
 *  immediate="true"
 *  metatype="false"
 *
 * @scr.property
 *  name="service.description"
 *  value="Default HTML Renderer Servlet"
 *
 * @scr.property
 *  name="service.vendor"
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for html get requests for Sling
 * @scr.property
 *  name="sling.servlet.resourceTypes"
 *  value="sling/servlet/default"
 * @scr.property
 *  name="sling.servlet.extensions"
 *  value="html"
 */
public class DefaultHtmlRendererServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -5815904221043005085L;

    private static final String responseContentType = "text/html";

    private final DefaultHtmlRenderer renderer;

    public DefaultHtmlRendererServlet() {
        this.renderer = new DefaultHtmlRenderer();
    }

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws ServletException, IOException {
        final Resource r = req.getResource();

        resp.setContentType(responseContentType);
        resp.setCharacterEncoding("UTF-8");

        final PrintWriter pw = resp.getWriter();

        final Node node = r.adaptTo(Node.class);
        /*
         * TODO final SyntheticResourceData srd =
         * r.adaptTo(SyntheticResourceData.class);
         */
        final Property p = r.adaptTo(Property.class);

        try {
            /*
             * TODO if(srd != null) { renderer.render(pw, r, srd); } else
             */
            if (node != null) {
                pw.println("<html><body>");
                renderer.render(pw, r, node);
                pw.println("</body></html>");
                
            } else if(p != null) {
                // for properties, we just output the String value
                renderer.render(pw, r, p);
            }

        } catch (RepositoryException re) {
            throw new ServletException("Cannot dump contents of "
                + req.getResource().getPath(), re);
        }
    }
}
