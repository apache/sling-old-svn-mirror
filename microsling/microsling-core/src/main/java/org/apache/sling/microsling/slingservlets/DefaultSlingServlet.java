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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.microsling.slingservlets.renderers.DefaultHtmlRendererServlet;
import org.apache.sling.microsling.slingservlets.renderers.PlainTextRendererServlet;

/**
 * The default SlingServlet, used if no other SlingServlet wants to process the
 * current request.
 */
public class DefaultSlingServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = -2259461041692895761L;
    
    private Map<String, Servlet> renderingServlets = new HashMap <String, Servlet>();
    
    public DefaultSlingServlet() {
        renderingServlets.put("txt", new PlainTextRendererServlet());
        renderingServlets.put("html", new DefaultHtmlRendererServlet());
    }
    
    @Override
    /** Delegate rendering to one of our renderingServlets, based on the request extension */
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws ServletException, IOException {

        // ensure the resource or try web app contents
        final Resource  r = req.getResource();
        if (Resource.RESOURCE_TYPE_NON_EXISTING.equals(r.getResourceType())) {

            URL url = getServletContext().getResource(r.getURI());
            if (url != null) {
                spool(url, resp);
                return;
            }

            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                "Resource not found: " + r.getURI());
        }

        // make sure we have an Item, and render it via one of our renderingServlets 
        final Object data = r.getRawData();
        if(data!=null && (data instanceof Item)) {
            String ext = req.getRequestPathInfo().getExtension();
            if(ext==null || ext.length() == 0) {
                ext = "txt";
            }
            final Servlet s = renderingServlets.get(ext);
            if(s!=null) {
                s.service(req, resp);
            } else {
                throw new HttpStatusCodeException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "No default renderer found for extension='" + ext + "'"
                        + ", use one of these extensions: " + renderingServlets.keySet()
                );
            }
            
        } else {
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Not implemented: resource " + req.getResource().getURI()
                + " cannot be dumped by " + getClass().getSimpleName());
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws ServletException, IOException {

        // require a node resource
        Resource r = req.getResource();
        if (!(req.getResource().getRawData() instanceof Node)
            && !Resource.RESOURCE_TYPE_NON_EXISTING.equals(r.getResourceType())) {
            throw new HttpStatusCodeException(HttpServletResponse.SC_NOT_FOUND,
                "Resource not found: " + r.getURI() + " must be missing or a Node");
        }

        String redirectPath = req.getPathInfo();
        Session s = null;
        try {
            Node current = (Node) req.getResource().getRawData();
            if (current == null) {
                Resource root = req.getResourceResolver().getResource("/");
                if (root != null) {
                    current = (Node) root.getRawData();
                } else {
                    throw new ServletException("Cannot get resource for root node");
                }
            }
            s = current.getSession();

            // Decide whether to create or update a node
            // TODO: this is a simplistic way of deciding, for now: if we have
            // no Resource or if the Node that it points to already has child nodes,
            // we create a new node. Else we update the current node.
            if(current.hasNodes()) {
                final RequestPathInfo pathInfo = req.getRequestPathInfo();
                final String parentPath = pathInfo.getResourcePath();
                final String newNodePath = (pathInfo.getSuffix() == null || pathInfo.getSuffix().length() == 0)
                        ? String.valueOf(System.currentTimeMillis())
                        : pathInfo.getSuffix();
                current = deepCreateNode(s, parentPath + "/" + newNodePath);
            }

            // Copy request parameters to node properties and save
            setPropertiesFromRequest(current, req);
            s.save();
            redirectPath = current.getPath();

        } catch (RepositoryException re) {
            throw new ServletException("Failed to modify content: "
                + re.getMessage(), re);

        } finally {
            try {
                if (s != null && s.hasPendingChanges()) {
                    s.refresh(false);
                }
            } catch (RepositoryException re) {
                // TODO: might want to log, but don't further care
            }
        }

        // redirect to the created node, so that it is displayed using a user-supplied extension
        String redirectExtension = req.getParameter("slingDisplayExtension");
        final String redirectUrl =
            req.getContextPath() + req.getServletPath() + redirectPath
            + (redirectExtension == null ? "" : "." + redirectExtension)
        ;
        resp.sendRedirect(redirectUrl);
    }

    /** Set node properties from current request (only handles Strings for now) */
    protected void setPropertiesFromRequest(Node n, HttpServletRequest req)
            throws RepositoryException {
        // TODO ignore sling-specific properties like slingDisplayExtension
        for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
            final String name = (String) e.nextElement();
            final String[] values = req.getParameterValues(name);
            if (values.length==1) {
            	n.setProperty(name, values[0]);
            } else {
            	n.setProperty(name, values);
            }
        }
    }

    /**
     * Deep creates a node, parent-padding with nt:unstructured nodes
     *
     * @param path absolute path to node that needs to be deep-created
     */
    protected Node deepCreateNode(Session s, String path)
            throws RepositoryException {
        String[] pathelems = path.substring(1).split("/");
        int i = 0;
        String mypath = "";
        Node parent = s.getRootNode();
        while (i < pathelems.length) {
            String name = pathelems[i];
            mypath += "/" + name;
            if (!s.itemExists(mypath)) {
                parent.addNode(name);
            }
            parent = (Node) s.getItem(mypath);
            i++;
        }
        return (parent);
    }


    protected void spool(URL url, SlingHttpServletResponse res) throws IOException {
        URLConnection conn = url.openConnection();

        if (conn.getContentType() != null) {
            res.setContentType(conn.getContentType());
        }
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
