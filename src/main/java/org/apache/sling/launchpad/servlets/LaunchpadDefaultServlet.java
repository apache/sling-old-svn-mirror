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
package org.apache.sling.launchpad.servlets;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.launchpad.renderers.DefaultHtmlRendererServlet;
import org.apache.sling.launchpad.renderers.JsonRendererServlet;
import org.apache.sling.launchpad.renderers.PlainTextRendererServlet;
import org.apache.sling.ujax.UjaxPostServlet;

/**
 * Replaces the Sling default servlet (that bundle must NOT be
 * active) for the Launchpad, by delegating to our default 
 * renderers for GET requests, and to the ujax POST servlet 
 * for POST requests.
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
 *  value="Launchpad Default Servlet"
 *  
 * @scr.property 
 *  name="service.vendor" 
 *  value="The Apache Software Foundation"
 *
 * Use this as the default servlet for Sling 
 * @scr.property 
 *  name="sling.servlet.resourceTypes" 
 *  value="sling/servlet/default"
 *  
 */
public class LaunchpadDefaultServlet extends SlingAllMethodsServlet {

    private Servlet postServlet;

    private Servlet defaultGetServlet;

    private Servlet ujaxInfoServlet;

    private Map<String, Servlet> getServlets;

    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        // setup our "internal" servlets
        postServlet = new UjaxPostServlet();
        postServlet.init(config);

        ujaxInfoServlet = new UjaxInfoServlet();
        ujaxInfoServlet.init(config);

        defaultGetServlet = new PlainTextRendererServlet("text/plain");

        getServlets = new HashMap<String, Servlet>();
        getServlets.put("html", new DefaultHtmlRendererServlet("text/html"));
        getServlets.put("json", new JsonRendererServlet("application/json"));
        getServlets.put("txt", defaultGetServlet);
    }

    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {
        final Resource resource = request.getResource();

        if (request.getPathInfo().startsWith(UjaxInfoServlet.PATH_PREFIX)) {
            ujaxInfoServlet.service(request, response);
            return;
        }

        // cannot handle the request for missing resources
        if (resource instanceof NonExistingResource) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Resource not found at path " + resource.getPath());
            return;
        }

        // render using a servlet or binary streaming
        Servlet s = defaultGetServlet;
        InputStream stream = null;
        final String ext = request.getRequestPathInfo().getExtension();
        if (ext != null && ext.length() > 0) {
            // if there is an extension, lookup our getServlets
            s = getServlets.get(ext);
        } else {
            // no extension means we're addressing a static file directly
            // check whether the resource adapts to a stream, spool then
            stream = resource.adaptTo(InputStream.class);
        }

        // render using stream, s, or fail
        if (stream != null) {
            stream(request, response, resource, stream);
        } else if (s != null) {
            s.service(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No default renderer found for extension='" + ext + "'");
        }
    }

    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
        postServlet.service(request, response);
    }

    /** Stream the Resource to response */
    private void stream(HttpServletRequest request,
            HttpServletResponse response, Resource resource, InputStream stream)
            throws IOException {

        ResourceMetadata meta = resource.getResourceMetadata();

        // check the last modification time and If-Modified-Since header
        Long modifTime = (Long) meta.get(ResourceMetadata.MODIFICATION_TIME);
        if (unmodified(request, modifTime)) {

            response.setStatus(SC_NOT_MODIFIED);

        } else {

            if (modifTime != null) {    
                response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
            }

            final String defaultContentType = "application/octet-stream";
            String contentType = (String) meta.get(ResourceMetadata.CONTENT_TYPE);
            if (contentType == null || defaultContentType.equals(contentType)) {
                // if repository doesn't provide a content-type, or provides the
                // default one,
                // try to do better using our servlet context
                final String ct = getServletContext().getMimeType(
                    resource.getPath());
                if (ct != null) {
                    contentType = ct;
                }
            }
            if (contentType != null) {
                response.setContentType(contentType);
            }

            String encoding = (String) meta.get(ResourceMetadata.CHARACTER_ENCODING);
            if (encoding != null) {
                response.setCharacterEncoding(encoding);
            }

            try {
                OutputStream out = response.getOutputStream();

                byte[] buf = new byte[1024];
                int rd;
                while ((rd = stream.read(buf)) >= 0) {
                    out.write(buf, 0, rd);
                }

            } finally {
                try {
                    stream.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if the request has a
     * <code>If-Modified-Since</code> header whose date value is later than
     * the last modification time given as <code>modifTime</code>.
     * 
     * @param request The <code>ComponentRequest</code> checked for the
     *            <code>If-Modified-Since</code> header.
     * @param modifTime The last modification time to compare the header to.
     * @return <code>true</code> if the <code>modifTime</code> is less than
     *         or equal to the time of the <code>If-Modified-Since</code>
     *         header.
     */
    private boolean unmodified(HttpServletRequest request, Long modifTime) {
        if (modifTime != null) {
            long modTime = modifTime / 1000; // seconds
            long ims = request.getDateHeader(HEADER_IF_MODIFIED_SINCE) / 1000;
            return modTime <= ims;
        }
        
        // we have no modification time value, assume modified
        return false;
    }

}
