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
package org.apache.sling.launchpad.renderers;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamRendererServlet extends PlainTextRendererServlet {

    private static final long serialVersionUID = -1L;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    public StreamRendererServlet(String contentType, ServletConfig config)
            throws ServletException {
        super(contentType);

        // not quite correct, but ok
        init(config);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        Resource resource = request.getResource();
        ResourceMetadata meta = resource.getResourceMetadata();

        // check the last modification time and If-Modified-Since header
        Long modifTime = (Long) meta.get(ResourceMetadata.MODIFICATION_TIME);
        if (unmodified(request, modifTime)) {
            response.setStatus(SC_NOT_MODIFIED);
            return;
        }

        // fall back to plain text rendering if the resource has no stream
        InputStream stream = resource.adaptTo(InputStream.class);
        if (stream == null) {
            super.doGet(request, response);
            return;
        }

        // finally stream the resource
        try {

            if (modifTime != null) {
                response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);
            }

            final String defaultContentType = "application/octet-stream";
            String contentType = (String) meta.get(ResourceMetadata.CONTENT_TYPE);
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

            String encoding = (String) meta.get(ResourceMetadata.CHARACTER_ENCODING);
            if (encoding != null) {
                response.setCharacterEncoding(encoding);
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