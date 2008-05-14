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
package org.apache.sling.servlets.get.helpers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>HtmlRendererServlet</code> renders the current resource in HTML
 * on behalf of the {@link org.apache.sling.servlets.get.DefaultGetServlet}.
 */
public class HtmlRendererServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -5815904221043005085L;

    public static final String EXT_HTML = "html";

    private static final String responseContentType = "text/html";

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
                render(pw, r, node);
                pw.println("</body></html>");

            } else if (p != null) {
                // for properties, we just output the String value
                render(pw, r, p);
            }

        } catch (RepositoryException re) {
            throw new ServletException("Cannot dump contents of "
                + req.getResource().getPath(), re);
        }
    }

    public void render(PrintWriter pw, Resource r, Node n)
            throws RepositoryException {
        pw.println("<h1>Node dumped by " + getClass().getSimpleName() + "</h1>");
        pw.println("<p>Node path: <b>" + n.getPath() + "</b></p>");
        pw.println("<p>Resource metadata: <b>" + r.getResourceMetadata()
            + "</b></p>");

        pw.println("<h2>Node properties</h2>");
        for (PropertyIterator pi = n.getProperties(); pi.hasNext();) {
            final Property p = pi.nextProperty();
            printPropertyValue(pw, p);
        }
    }

    public void render(PrintWriter pw, Resource r, Property p)
            throws RepositoryException {
        pw.print(p.getValue().getString());
    }

    protected void dump(PrintWriter pw, Resource r, Property p)
            throws RepositoryException {
        pw.println("<h2>Property dumped by " + getClass().getSimpleName()
            + "</h1>");
        pw.println("<p>Property path:" + p.getPath() + "</p>");
        pw.println("<p>Resource metadata: " + r.getResourceMetadata() + "</p>");

        printPropertyValue(pw, p);
    }

    protected void printPropertyValue(PrintWriter pw, Property p)
            throws RepositoryException {

        pw.print(p.getName() + ": <b>");

        if (p.getDefinition().isMultiple()) {
            Value[] values = p.getValues();
            pw.print('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(values[i].getString());
            }
            pw.print(']');
        } else {
            pw.print(p.getValue().getString());
        }

        pw.print("</b><br/>");
    }

}
