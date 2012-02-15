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
package org.apache.sling.servlets.get.impl.impl.info;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>SlingInfoServlet</code> TODO
 */
@SuppressWarnings("serial")
@Component(immediate=true)
@Service(Servlet.class)
@Properties({
    @Property(name="service.description", value="Sling Info Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.paths", value="/system/sling/info")
})
public class SlingInfoServlet extends SlingSafeMethodsServlet {
    
    /**
     * 
     */
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    private static final String CACHE_CONTROL_HEADER_VALUE = 
        "private, no-store, no-cache, max-age=0, must-revalidate";

    private Map<String, SlingInfoProvider> infoProviders = new HashMap<String, SlingInfoProvider>();

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        Map<String, String> data = null;

        if (request.getRequestPathInfo().getSelectors().length > 0) {
            String label = request.getRequestPathInfo().getSelectors()[0];
            SlingInfoProvider uip = infoProviders.get(label);
            if (uip != null) {
                data = uip.getInfo(request);
            }
        }

        if (data == null) {

            // listOptions(response);
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Unknown Info Request");

        } else {
            response.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE);

            String extension = request.getRequestPathInfo().getExtension();
            if ("json".equals(extension)) {
                renderJson(response, data);
            } else if ("txt".equals(extension)) {
                renderPlainText(response, data);
            } else { // default to html
                renderHtml(response, data);
            }

        }
    }
/*
    private void listOptions(SlingHttpServletResponse response)
            throws IOException {

        // render data in JSON format
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        out.println("<html><head><title>Sling Info Providers</title></head>");
        out.println("<body><h1>Select from the following Providers</h1>");

        out.println("<table>");
        for (String label : infoProviders.keySet()) {
            out.print("<tr><td>");
            out.print("<a href='sling.");
            out.print(label);
            out.print(".html'>");
            out.print(label);
            out.print("</a>");
            out.println("</td></tr>");
        }
        out.println("</table>");

        out.println("</body>");
        out.flush();

    }
*/
    private void renderJson(SlingHttpServletResponse response,
            Map<String, String> data) throws IOException {
        // render data in JSON format
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final Writer out = response.getWriter();
        final JSONWriter w = new JSONWriter(out);

        try {
            w.object();
            for (Map.Entry<String, String> e : data.entrySet()) {
                w.key(e.getKey());
                w.value(e.getValue());
            }
            w.endObject();

        } catch (JSONException jse) {
            out.write(jse.toString());

        } finally {
            out.flush();
        }
    }

    private void renderHtml(SlingHttpServletResponse response,
            Map<String, String> data) throws IOException {
        // render data in JSON format
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        out.println("<html><head><title>Sling Info</title></head>");
        out.println("<body><h1>Sling Info</h1>");

        out.println("<table>");
        for (Map.Entry<String, String> e : data.entrySet()) {
            out.print("<tr><td>");
            out.print(e.getKey());
            out.print("</td><td>");
            out.print(e.getValue());
            out.println("</td></tr>");
        }
        out.println("</table>");

        out.println("</body>");
        out.flush();
    }

    private void renderPlainText(SlingHttpServletResponse response,
            Map<String, String> data) throws IOException {

        // render data in JSON format
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter out = response.getWriter();

        for (Map.Entry<String, String> e : data.entrySet()) {
            out.print(e.getKey());
            out.print(": ");
            out.println(e.getValue());
        }

        out.flush();
    }

    // --------- SCR integration -----------------------------------------------

    protected void activate(ComponentContext context) {
        infoProviders.put(SessionInfoProvider.PROVIDER_LABEL,
            new SessionInfoProvider());
    }
}