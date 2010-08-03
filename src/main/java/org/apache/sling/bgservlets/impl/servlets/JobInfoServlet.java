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
package org.apache.sling.bgservlets.impl.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.bgservlets.BackgroundServletConstants;
import org.apache.sling.bgservlets.JobConsole;
import org.apache.sling.bgservlets.JobData;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/** Default rendering of a job node in various formats,
 *  meant to be displayed when the job is started.
 */
@Component
@Service
@Properties({
    @Property(name = "sling.servlet.resourceTypes", value = BackgroundServletConstants.JOB_RESOURCE_TYPE),
    @Property(name = "sling.servlet.methods", value="GET")
})
@SuppressWarnings("serial")
public class JobInfoServlet extends SlingSafeMethodsServlet {

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String DEFAULT_EXT = "txt";

    private static final Map<String, Renderer> renderers;
    static {
        renderers = new HashMap<String, Renderer>();
        renderers.put("txt", new TextRenderer());
        renderers.put("html", new HtmlRenderer());
        renderers.put("json", new JsonRenderer());
    }

    @Reference
    private JobConsole jobConsole;

    static interface Renderer {
        void render(PrintWriter pw, String streamPath, String streamResource) throws IOException;
    }

    private static class TextRenderer implements Renderer {
        public void render(PrintWriter pw, String streamPath, String streamResource) {
            pw.println("Background execution: job output available at ");
            pw.println(streamPath);
        }
    }

    private static class HtmlRenderer implements Renderer {
        public void render(PrintWriter pw, String streamPath, String streamResource) {
            pw.println("<html><head><title>Background job</title>");
            pw.println("<link rel='stream' href='" + streamPath + "'/>");
            pw.println("</head><body>");
            pw.println("<h1>Background job information</h1>");
            pw.println("Job output available at");
            pw.println("<a href='" + streamPath + "'>" + streamResource + "</a>");
            pw.println("</body>");
        }
    }

    private static class JsonRenderer implements Renderer {
        public void render(PrintWriter pw, String streamPath, String streamResource) throws IOException {
            JSONWriter w = new JSONWriter(pw);
            try {
                w.object();
                w.key("info");
                w.value("Background job information");
                w.key("jobStreamPath");
                w.value(streamPath);
                w.endObject();
            } catch (JSONException e) {
                throw (IOException)new IOException("JSONException in " + getClass().getSimpleName()).initCause(e);
            }
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
        try {
            final Node n = request.getResource().adaptTo(Node.class);
            final JobStatus j = jobConsole.getJobStatus(n.getSession(), n.getPath());
            final String streamPath =  j.getStreamPath();
            final String fullStreamPath = request.getContextPath() + streamPath; 
            final String ext = request.getRequestPathInfo().getExtension();
            Renderer r = renderers.get(ext);
            if(r == null) {
                r = renderers.get(DEFAULT_EXT);
            }
            if(r == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JobRenderer available for extension '" + ext + "'");
            }
            response.setContentType(request.getResponseContentType());
            response.setCharacterEncoding(DEFAULT_ENCODING);
            r.render(response.getWriter(), fullStreamPath, streamPath);
        } catch(RepositoryException re) {
            throw new ServletException("RepositoryException in doGet", re);
        }
    }
}
