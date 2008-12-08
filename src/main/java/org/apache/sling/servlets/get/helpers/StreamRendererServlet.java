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

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * The <code>StreamRendererServlet</code> streams the current resource to the
 * client on behalf of the
 * {@link org.apache.sling.servlets.get.DefaultGetServlet}. If the current
 * resource cannot be streamed it is rendered using the
 * {@link PlainTextRendererServlet}.
 */
public class StreamRendererServlet extends SlingSafeMethodsServlet {

    public static final String EXT_RES = "res";

    private static final long serialVersionUID = -1L;

    private boolean index;

    private String[] indexFiles;

    public StreamRendererServlet(boolean index, String[] indexFiles) {
        this.index = index;
        this.indexFiles = indexFiles;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // ensure no extension or "res"
        String ext = request.getRequestPathInfo().getExtension();
        if (ext != null && !ext.equals(EXT_RES)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No default renderer found for extension='" + ext + "'");
            return;
        }

        final Resource resource = request.getResource();
        if (ResourceUtil.isNonExistingResource(resource)) {
            throw new ResourceNotFoundException("No data to render.");
        }

        // trailing slash on url means directory listing
        if ("/".equals(request.getRequestPathInfo().getSuffix())) {
            renderDirectory(request, response);
            return;
        }

        // check the last modification time and If-Modified-Since header
        ResourceMetadata meta = resource.getResourceMetadata();
        long modifTime = meta.getModificationTime();
        if (unmodified(request, modifTime)) {
            response.setStatus(SC_NOT_MODIFIED);
            return;
        }

        // fall back to plain text rendering if the resource has no stream
        InputStream stream = resource.adaptTo(InputStream.class);
        if (stream != null) {
            
            streamResource(resource, stream, response);
            
        } else {
            
            // the resource is the root, do not redirect, immediately index
            if ("/".equals(resource.getPath())) {
                
                renderDirectory(request, response);
                
            } else {
                
                // redirect to this with trailing slash to render the index
                String url = request.getResourceResolver().map(request,
                    resource.getPath())
                    + "/";
                response.sendRedirect(url);
                
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
    private boolean unmodified(HttpServletRequest request, long modifTime) {
        if (modifTime > 0) {
            long modTime = modifTime / 1000; // seconds
            long ims = request.getDateHeader(HEADER_IF_MODIFIED_SINCE) / 1000;
            return modTime <= ims;
        }

        // we have no modification time value, assume modified
        return false;
    }

    private void streamResource(Resource resource, InputStream stream,
            SlingHttpServletResponse response) throws IOException {
        // finally stream the resource
        try {

            ResourceMetadata meta = resource.getResourceMetadata();
            long modifTime = meta.getModificationTime();

            if (modifTime > 0) {
                response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
            }

            final String defaultContentType = "application/octet-stream";
            String contentType = meta.getContentType();
            if (contentType == null || defaultContentType.equals(contentType)) {
                // if repository doesn't provide a content-type, or
                // provides the
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

            String encoding = meta.getCharacterEncoding();
            if (encoding != null) {
                response.setCharacterEncoding(encoding);
            }

            long length = meta.getContentLength();
            if (length > 0 && length < Integer.MAX_VALUE) {
                response.setContentLength((int) length);
            }

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
    
    private void renderDirectory(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        Resource resource = request.getResource();
        ResourceResolver resolver = request.getResourceResolver();

        // check for an index file
        for (String index : indexFiles) {
            Resource fileRes = resolver.getResource(resource, index);
            if (fileRes != null && !ResourceUtil.isSyntheticResource(fileRes)) {

                // include the index resource with no suffix and selectors !
                RequestDispatcherOptions rdo = new RequestDispatcherOptions();
                rdo.setReplaceSuffix("");
                rdo.setReplaceSelectors("");

                RequestDispatcher dispatcher;
                if (index.indexOf('.') < 0) {
                    String filePath = fileRes.getPath() + ".html";
                    dispatcher = request.getRequestDispatcher(filePath, rdo);
                } else {
                    dispatcher = request.getRequestDispatcher(fileRes, rdo);
                }
                
                dispatcher.include(request, response);
                return;
            }
        }

        if (index) {
//            RequestDispatcherOptions rdo = new RequestDispatcherOptions();
//            rdo.setReplaceSelectors("sling.index");
//            request.getRequestDispatcher(resource, rdo).include(request, response);
            renderIndex(resource, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }
    
    private void renderIndex(Resource resource,
            SlingHttpServletResponse response) throws IOException {
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String path = resource.getPath();

        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">");
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>Index of " + path + "</title>");
        pw.println("</head>");

        pw.println("<body>");
        pw.println("<h1>Index of " + path + "</h1>");

        pw.println("<pre>");
        pw.println("Name                               Last modified                   Size  Description");
        pw.println("<hr>");

        if (!"/".equals(path)) {
            pw.println("<a href='../'>../</a>                                                                 -     Parent");
        }

        // render the children
        Iterator<Resource> children = ResourceUtil.listChildren(resource);
        while (children.hasNext()) {
            renderChild(pw, children.next());
        }

        pw.println("</pre>");
        pw.println("</body>");
        pw.println("</html>");

    }

    private void renderChild(PrintWriter pw, Resource resource) {

        String name = ResourceUtil.getName(resource.getPath());

        InputStream ins = resource.adaptTo(InputStream.class);
        if (ins == null) {
            name += "/";
        } else {
            try {
                ins.close();
            } catch (IOException ignore) {
            }
        }

        String displayName = name;
        String suffix;
        if (displayName.length() >= 32) {
            displayName = displayName.substring(0, 29) + "...";
            suffix = "";
        } else {
            suffix = "                                               ".substring(
                0, 32 - displayName.length());
        }
        pw.printf("<a href='%s'>%s</a>%s", name, displayName, suffix);

        ResourceMetadata meta = resource.getResourceMetadata();
        long lastModified = meta.getModificationTime();
        pw.print("    " + new Date(lastModified) + "    ");

        long length = meta.getContentLength();
        if (length > 0) {
            pw.print(length);
        } else {
            pw.print('-');
        }

        pw.println();
    }
}