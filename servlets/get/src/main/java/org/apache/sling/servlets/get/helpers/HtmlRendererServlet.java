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
import java.util.Iterator;
import java.util.Map;

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

        pw.println("<html><body>");
        pw.println("<h1>Resource dumped by " + getClass().getSimpleName() + "</h1>");
        pw.println("<p>Resource path: <b>" + r.getPath() + "</b></p>");
        pw.println("<p>Resource metadata: <b>" + r.getResourceMetadata()
            + "</b></p>");

        @SuppressWarnings("unchecked")
        final Map map = r.adaptTo(Map.class);
        if ( map != null ) {
            render(pw, r, map);
        } else if ( r.adaptTo(String.class) != null ) {
            render(pw, r, r.adaptTo(String.class));
        } else {
            pw.println("<p>Resource can't be adapted to a map or a string.</p>");
        }
        pw.println("</body></html>");
    }

    @SuppressWarnings("unchecked")
    private void render(PrintWriter pw, Resource r, Map map) {
        pw.println("<h2>Resource properties</h2>");
        final Iterator<Map.Entry> pi = map.entrySet().iterator();
        while ( pi.hasNext() ) {
            final Map.Entry p = pi.next();
            printPropertyValue(pw, p.getKey().toString(), p.getValue());
            pw.println();
        }
    }

    private void render(PrintWriter pw, Resource r, String value) {
        printPropertyValue(pw, "Resource Value", value);
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

        pw.print("</b><br/>");
    }

}
