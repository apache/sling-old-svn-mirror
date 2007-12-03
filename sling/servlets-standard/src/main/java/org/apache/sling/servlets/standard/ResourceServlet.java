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
package org.apache.sling.servlets.standard;

import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_IF_MODIFIED_SINCE;
import static org.apache.sling.api.servlets.HttpConstants.HEADER_LAST_MODIFIED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * The <code>ResourceServlet</code> handles nt:resource nodes
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *      value="Servlet to handle nt:resource nodes"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.resourceTypes" value="nt:resource"
 * @scr.service
 */
public class ResourceServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        Resource resource = request.getResource();
        ResourceObject content = resource.adaptTo(ResourceObject.class);

        // check the last modification time and If-Modified-Since header
        long modifTime = content.getLastModificationTime();
        if (unmodified(request, modifTime)) {

            response.setStatus(SC_NOT_MODIFIED);

        } else {

            response.setContentType(content.getMimeType());
            response.setDateHeader(HEADER_LAST_MODIFIED, modifTime);

            OutputStream out = response.getOutputStream();
            InputStream ins = null;
            try {
                ins = content.getValue().getStream();
                IOUtils.copy(ins, out);
            } catch (RepositoryException re) {
                throw (IOException) new IOException("Cannot get content from"
                    + content.getPath()).initCause(re);
            } finally {
                IOUtils.closeQuietly(ins);
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
    private boolean unmodified(SlingHttpServletRequest request, long modifTime) {
        long ims = request.getDateHeader(HEADER_IF_MODIFIED_SINCE);
        return modifTime <= ims;
    }
}
