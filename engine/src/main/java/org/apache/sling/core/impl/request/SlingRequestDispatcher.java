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
package org.apache.sling.core.impl.request;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingRequestDispatcher implements RequestDispatcher {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Resource resource;

    private RequestDispatcherOptions options;

    private String path;

    public SlingRequestDispatcher(String path, RequestDispatcherOptions options) {
        this.path = path;
        this.options = options;
        this.resource = null;
    }

    public SlingRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {

        this.resource = resource;
        this.options = options;
        this.path = resource.getPath();
    }

    public void include(ServletRequest request, ServletResponse sResponse)
            throws ServletException, IOException {

        /**
         * TODO: I have made some quick fixes in this method for SLING-221 and
         * SLING-222, but haven't had time to do a proper review. This method
         * might deserve a more extensive rewrite.
         */

        SlingHttpServletRequest cRequest = RequestData.unwrap(request);
        RequestData rd = RequestData.getRequestData(cRequest);
        String absPath = getAbsolutePath(cRequest, path);

        // if the response is not an HttpServletResponse, fail gracefully not
        // doing anything
        if (!(sResponse instanceof HttpServletResponse)) {
            log.error("include: Failed to include {}, response has wrong type",
                absPath);
            return;
        }

        final HttpServletResponse response = (HttpServletResponse) sResponse;

        if (resource == null) {

            // the absolute path may have the context path, cut it off
            if (absPath.startsWith(cRequest.getContextPath())) {
                absPath = absPath.substring(cRequest.getContextPath().length());
            }

            // resolve the absolute path in the resource resolver, using
            // only those parts of the path as if it would be request path
            resource = cRequest.getResourceResolver().resolve(absPath);

            // if the resource could not be resolved, fail gracefully
            if (resource == null) {
                log.error(
                    "include: Could not resolve {} to a resource, not including",
                    absPath);
                return;
            }
        }

        // ensure request path info and optional merges
        SlingRequestPathInfo info = new SlingRequestPathInfo(resource, absPath);
        info = info.merge(cRequest.getRequestPathInfo());

        // merge request dispatcher options and resource type overwrite
        if (options != null) {
            info = info.merge(options);

            // ensure overwritten resource type
            String rtOverwrite = options.getForceResourceType();
            if (rtOverwrite != null
                && !rtOverwrite.equals(resource.getResourceType())) {
                resource = new TypeOverwritingResourceWrapper(resource,
                    rtOverwrite);
            }
        }

        cRequest.getRequestProgressTracker().log("Including resource " + info.getResourcePath());
        rd.getSlingMainServlet().includeContent(request, response, resource,
            info);
    }

    public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        // TODO, use servlet container dispatcher !!
    }

    private String getAbsolutePath(SlingHttpServletRequest request, String path) {
        // path is already absolute
        if (path.startsWith("/")) {
            return path;
        }

        // get parent of current request
        String uri = request.getResource().getPath();
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0) {
            uri = uri.substring(0, lastSlash);
        }

        // append relative path to parent
        return uri + '/' + path;
    }

    private static class TypeOverwritingResourceWrapper extends ResourceWrapper {

        private final String resourceType;

        TypeOverwritingResourceWrapper(Resource delegatee, String resourceType) {
            super(delegatee);
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }

    }
}