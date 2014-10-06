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
package org.apache.sling.servlets.get.impl.helpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>HtmlRendererServlet</code> renders the current resource in HTML
 * on behalf of the {@link org.apache.sling.servlets.get.impl.DefaultGetServlet}.
 */
public class HtmlRendererServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -5815904221043005085L;

    public static final String EXT_HTML = "html";

    @Override
    protected void doGet(SlingHttpServletRequest req,
            SlingHttpServletResponse resp) throws ServletException, IOException {
        final Resource r = req.getResource();

        if (ResourceUtil.isNonExistingResource(r)) {
            throw new ResourceNotFoundException("No data to render.");
        }

        resp.setContentType(req.getResponseContentType());
        resp.setCharacterEncoding("UTF-8");

        final PrintWriter pw = resp.getWriter();

        final boolean isIncluded = req.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null;

        @SuppressWarnings("unchecked")
        final Map map = r.adaptTo(Map.class);
        if ( map != null ) {
            printProlog(pw, isIncluded);
            printResourceInfo(pw, r);
            render(pw, r, map);
            printEpilog(pw, isIncluded);
        } else if ( r.adaptTo(String.class) != null ) {
            printProlog(pw, isIncluded);
            printResourceInfo(pw, r);
            render(pw, r, r.adaptTo(String.class));
            printEpilog(pw, isIncluded);
        } else if ( r.adaptTo(String[].class) != null ) {
            printProlog(pw, isIncluded);
            printResourceInfo(pw, r);
            render(pw, r, r.adaptTo(String[].class));
            printEpilog(pw, isIncluded);
        } else {
            if ( !isIncluded ) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // NO Content
            }
        }
    }

    private void printProlog(PrintWriter pw, boolean isIncluded) {
        if ( !isIncluded ) {
            pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"");
            pw.println("    \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
            pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
            pw.println("<head><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head>");
            pw.println("<body>");
        }
    }

    private void printEpilog(PrintWriter pw, boolean isIncluded) {
        if ( !isIncluded ) {
            pw.println("</body></html>");
        }
    }

    private void printResourceInfo(PrintWriter pw, Resource r) {
        pw.println("<h1>Resource dumped by " + getClass().getSimpleName() + "</h1>");
        pw.println("<p>Resource path: <b>" + r.getPath() + "</b></p>");
        pw.println("<p>Resource metadata: <b>"
            + StringEscapeUtils.escapeHtml(String.valueOf(r.getResourceMetadata()))
            + "</b></p>");

        pw.println("<p>Resource type: <b>" + r.getResourceType() + "</b></p>");

        String resourceSuperType = ResourceUtil.findResourceSuperType(r);
        if (resourceSuperType == null) {
            resourceSuperType = "-";
        }
        pw.println("<p>Resource super type: <b>" + resourceSuperType
            + "</b></p>");
    }

    @SuppressWarnings("unchecked")
    private void render(PrintWriter pw, Resource r, Map map) {
        pw.println("<h2>Resource properties</h2>");
        pw.println("<p>");
        final Iterator<Map.Entry> pi = map.entrySet().iterator();
        while ( pi.hasNext() ) {
            final Map.Entry p = pi.next();
            printPropertyValue(pw, p.getKey().toString(), p.getValue());
            pw.println();
        }
        pw.println("</p>");
    }

    private void render(PrintWriter pw, Resource r, String value) {
        printPropertyValue(pw, "Resource Value", value);
    }

    private void render(PrintWriter pw, Resource r, String[] values) {
        for (String value : values) {
            printPropertyValue(pw, "Resource Value", value);
        }
    }

    private void printPropertyValue(PrintWriter pw, String name, Object value) {

        pw.print(name + ": <b>");

        if ( value.getClass().isArray() ) {
            Object[] values = (Object[])value;
            pw.print('[');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print(values[i].toString());
            }
            pw.print(']');
        } else {
            pw.print(value.toString());
        }

        pw.print("</b><br />");
    }

}
