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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.service.component.annotations.Component;

/**
 * The <code>SlingInfoServlet</code>
 */
@SuppressWarnings("serial")
@Component(service = Servlet.class,
    property = {
            "service.description=Sling Info Servlet",
            "service.vendor=The Apache Software Foundation",
            "sling.servlet.paths=/system/sling/info"
    })
public class SlingInfoServlet extends SlingSafeMethodsServlet {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    private static final String CACHE_CONTROL_HEADER_VALUE =
        "private, no-store, no-cache, max-age=0, must-revalidate";

    static final String PROVIDER_LABEL = "sessionInfo";

    private Map<String, String> getInfo(final SlingHttpServletRequest request) {
        final Map<String, String> result = new HashMap<>();

        final ResourceResolver resolver = request.getResourceResolver();

        result.put("userID", resolver.getUserID());

        if (request.getAuthType() != null) {
            result.put("authType", request.getAuthType());
        }

        return result;
    }

    @Override
    protected void doGet(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws IOException {

        Map<String, String> data = null;

        if (request.getRequestPathInfo().getSelectors().length > 0) {
            final String label = request.getRequestPathInfo().getSelectors()[0];
            if ( PROVIDER_LABEL.equals(label) ) {
                data = this.getInfo(request);
            }
        }

        if (data == null) {

            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Unknown Info Request");

        } else {
            response.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_HEADER_VALUE);

            final String extension = request.getRequestPathInfo().getExtension();
            if ("json".equals(extension)) {
                renderJson(response, data);
            } else if ("txt".equals(extension)) {
                renderPlainText(response, data);
            } else { // default to html
                renderHtml(response, data);
            }

        }
    }

    private void renderJson(final SlingHttpServletResponse response,
            final Map<String, String> data) throws IOException {
        // render data in JSON format
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final Writer out = response.getWriter();
        final JSONWriter w = new JSONWriter(out);

        try {
            w.object();
            for (final Map.Entry<String, String> e : data.entrySet()) {
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

    private void renderHtml(final SlingHttpServletResponse response,
            final Map<String, String> data) throws IOException {
        // render data in JSON format
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        out.println("<html><head><title>Apche Sling Info</title></head>");
        out.println("<body><h1>Apache Sling Info</h1>");

        out.println("<table>");
        for (final Map.Entry<String, String> e : data.entrySet()) {
            out.print("<tr><td>");
            out.print(ResponseUtil.escapeXml(e.getKey()));
            out.print("</td><td>");
            out.print(ResponseUtil.escapeXml(e.getValue()));
            out.println("</td></tr>");
        }
        out.println("</table>");

        out.println("</body>");
        out.flush();
    }

    private void renderPlainText(final SlingHttpServletResponse response,
            final Map<String, String> data) throws IOException {

        // render data in JSON format
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter out = response.getWriter();

        for (final Map.Entry<String, String> e : data.entrySet()) {
            out.print(e.getKey());
            out.print(": ");
            out.println(e.getValue());
        }

        out.flush();
    }
}