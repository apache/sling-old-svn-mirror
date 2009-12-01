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
package org.apache.sling.jcr.webdav.impl.servlets;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.webdav.impl.helper.SlingResourceConfig;

/**
 * The <code>SlingSimpleWebDavServlet</code> extends the
 * JCR <code>SimpleWebdavServlet</code> with some
 * Sling-specific features
 */
public class SlingSimpleWebDavServlet extends SimpleWebdavServlet {

    private final SlingResourceConfig resourceConfig;

    private final Repository repository;

    /* package */ SlingSimpleWebDavServlet(SlingResourceConfig resourceConfig,
            Repository repository) {
        this.resourceConfig = resourceConfig;
        this.repository = repository;
    }

    // ---------- AbstractWebdavServlet overwrite ------------------------------

    @Override
    public void init() throws ServletException {
        super.init();

        setResourceConfig(resourceConfig);
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        // According to the spec the path info is either null or
        // a string starting with a slash. Thus a string of length 1
        // will be a string containing just the slash, which should not
        // be handled by the base class
        final String pinfo = request.getPathInfo();
        if (pinfo != null && pinfo.length() > 1) {

            // regular request, have the SimpleWebDAVServlet handle the request
            super.service(request, response);

        } else if ("OPTIONS".equals(request.getMethod())) {

            // OPTIONS request on the root, answer with the Allow header
            // without DAV-specific headers
            response.setContentLength(0);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Allow", "OPTIONS, GET, HEAD");

        } else {

            // request to the "root", redirect to the default workspace if
            // directly addressing the servlet and if the default workspace name
            // is not null (in which case we'd need to login to find out the
            // actual workspace name, SLING-256)
            SlingRepository slingRepo = (SlingRepository) getRepository();
            if (slingRepo.getDefaultWorkspace() == null) {

                // if we don't have a default workspace to redirect to, we
                // cannot handle the request and fail with not found
                response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "JCR workspace name required, please add it to the end of the URL"
                        + " (for the Jackrabbit embedded repository the default name is 'default') ");

            } else {

                // else redirect to the same URI with the default workspace
                // appended
                String uri = request.getRequestURI();
                if (pinfo == null) {
                    uri += "/";
                }
                uri += slingRepo.getDefaultWorkspace();
                response.sendRedirect(uri);

            }
        }
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

}
