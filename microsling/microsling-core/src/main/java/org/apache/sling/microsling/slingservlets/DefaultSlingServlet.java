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
package org.apache.sling.microsling.slingservlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NodeProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.microsling.servlet.MicroslingServletConfig;
import org.apache.sling.microsling.slingservlets.renderers.DefaultHtmlRendererServlet;
import org.apache.sling.microsling.slingservlets.renderers.JsonRendererServlet;
import org.apache.sling.microsling.slingservlets.renderers.PlainTextRendererServlet;

/**
 * The default SlingServlet, used if no other SlingServlet wants to process the
 * current request.
 */
public class DefaultSlingServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = -2259461041692895761L;
    private Map<String, Servlet> renderingServlets = new HashMap <String, Servlet>();
    private Servlet postServlet;
    private Servlet microjaxGetServlet;

    @Override
    public void init() throws ServletException {
        postServlet = new MicrojaxPostServlet();
        postServlet.init(new MicroslingServletConfig("Microjax POST servlet",getServletContext()));

        microjaxGetServlet = new MicrojaxGetServlet();
        microjaxGetServlet.init(new MicroslingServletConfig("Microjax GET servlet",getServletContext()));
        
        String contentType = null;
        final String ctSuffix = "; charset=UTF-8";

        contentType = getServletContext().getMimeType("dummy.txt");
        renderingServlets.put(contentType, new PlainTextRendererServlet(contentType + ctSuffix));

        contentType = getServletContext().getMimeType("dummy.html");
        renderingServlets.put(contentType, new DefaultHtmlRendererServlet(contentType + ctSuffix));

        contentType = getServletContext().getMimeType("dummy.json");
        renderingServlets.put(contentType, new JsonRendererServlet(contentType + ctSuffix));
    }

    @Override
    /** Delegate rendering to one of our renderingServlets, based on the request extension */
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws ServletException, IOException {

        // ensure the resource or try web app contents
        final Resource  r = req.getResource();
        if (Resource.RESOURCE_TYPE_NON_EXISTING.equals(r.getResourceType())) {

            String path = r.getURI();
            if (path.startsWith(MicrojaxGetServlet.URI_PREFIX)) {
                microjaxGetServlet.service(req, resp);
                return;
                
            } else if (path.startsWith("/WEB-INF") || path.startsWith("/META-INF")) {
                throw new HttpStatusCodeException(HttpServletResponse.SC_FORBIDDEN,
                        "Access to " + path + " denied");
            }
            
            URL url = getServletContext().getResource(path);
            if (url != null) {
                spool(url, resp);
            } else {
                throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                        "Resource not found: " + r.getURI());
            }
        }

        // make sure we have an Item, and render it via one of our renderingServlets
        if(r instanceof NodeProvider) {
            final String suffix = req.getRequestPathInfo().getSuffix();
            if(suffix != null && suffix.length() > 0) {
                // accept exact addressing only for default rendering:
                // a non-empty suffix means there was extra stuff after the path
                // of the resource
                throw new HttpStatusCodeException(
                        HttpServletResponse.SC_NOT_FOUND,
                        "Ancestor resource found (" + r.getResourceMetadata().get(ResourceMetadata.RESOLUTION_PATH) + ")"
                        + " but URL suffix must be empty for default rendering (suffix=" + suffix + ")"
                );
            }
            final String contentType = req.getResponseContentType();
            final Servlet s = renderingServlets.get(contentType);
            if(s!=null) {
                s.service(req, resp);
            } else {
                throw new HttpStatusCodeException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No default renderer found for Content-Type='" + contentType + "'"
                        + ", use one of these Content-types: " + renderingServlets.keySet()
                );
            }

        } else {
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Not implemented: resource " + req.getResource().getURI()
                + " cannot be dumped by " + getClass().getSimpleName());
        }
    }

    @Override
    /** delegate to our postServlet */
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
    throws ServletException, IOException
    {
        postServlet.service(request, response);
    }

    protected void spool(URL url, SlingHttpServletResponse res) throws IOException {
        URLConnection conn = url.openConnection();

        // this previously used conn.getContentType(), but see SLING-112
        res.setContentType(getServletContext().getMimeType(url.getFile()));
        if (conn.getContentLength() > 0 ) {
            res.setContentLength(conn.getContentLength());
        }
        if (conn.getContentEncoding() != null) {
            res.setCharacterEncoding(conn.getContentEncoding());
        }
        if (conn.getLastModified() > 0) {
            res.setDateHeader(HttpConstants.HEADER_LAST_MODIFIED, conn.getLastModified());
        }

        InputStream ins = null;
        OutputStream out = null;
        try {
            ins = conn.getInputStream();
            out = res.getOutputStream();

            byte[] buf = new byte[2048];
            int num;
            while ((num = ins.read(buf)) >= 0) {
                out.write(buf, 0, num);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }

    }
}
