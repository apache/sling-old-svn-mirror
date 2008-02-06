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
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

public class SlingRequestDispatcher implements RequestDispatcher {

    private Resource resource;

    private RequestDispatcherOptions options;

    private String path;

    public SlingRequestDispatcher(String path,
            RequestDispatcherOptions options) {
        this.path = path;
        this.options = options;

        this.resource = null;
    }

    public SlingRequestDispatcher(Resource resource,
            RequestDispatcherOptions options) {
        this.resource = resource;
        this.options = options;
        // TODO Is the empty path ok?
        this.path = (resource == null ? "" : resource.getPath());
    }

    public void include(ServletRequest request, ServletResponse sResponse)
            throws ServletException, IOException {

        /** TODO: I have made some quick fixes in this method for SLING-221
         *  and SLING-222, but haven't had time to do a proper review. This
         *  method might deserve a more extensive rewrite. 
         */
        
        // this may throw an exception in case loading fails, which is
        // ok here, if no content is available at that path null is
        // return, which results in using the servlet container
        SlingHttpServletRequest cRequest = RequestData.unwrap(request);
        RequestData rd = RequestData.getRequestData(cRequest);
        String absPath = getAbsolutePath(cRequest, path);
        
        if( ! (sResponse instanceof HttpServletResponse )) {
            throw new ServletException("Response is not an HttpServletResponse, cannot continue");
        }
        final HttpServletResponse response = (HttpServletResponse)sResponse;

        if (resource == null) {
            ResourceResolver rr = cRequest.getResourceResolver();
            
            if(absPath.startsWith(cRequest.getContextPath())) {
                absPath = absPath.substring(cRequest.getContextPath().length());
            }
            
            // remove extension before attempting to resolve, like in a "normal" request
            // TODO use the same parsing as for normal resources?
            final int lastSlash = absPath.lastIndexOf('/');
            final int lastDot = absPath.lastIndexOf('.');
            if(lastDot > lastSlash && lastDot >= 0) {
                absPath = absPath.substring(0, lastDot);
            }
            resource = rr.getResource(absPath);
        }

        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found in include: " + absPath);
            
            // The code below was previously used but causes SLING-222...not sure what's best
            // rd.getSlingMainServlet().includeServlet(request, response, path);

        } else {

            // ensure request path info and optional merges
            SlingRequestPathInfo info = new SlingRequestPathInfo(resource, absPath);
            info = info.merge(cRequest.getRequestPathInfo());

            if (options != null) {
                info = info.merge(options);

                // ensure overwritten resource type
                String rtOverwrite = options.get(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE);
                if (rtOverwrite != null
                    && !rtOverwrite.equals(resource.getResourceType())) {
                    resource = new ResourceWrapper(resource, rtOverwrite);
                }
            }

            rd.getSlingMainServlet().includeContent(request, response,
                resource, info);
        }
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

    private static class ResourceWrapper implements Resource {

        private final Resource delegatee;

        private final String resourceType;

        ResourceWrapper(Resource delegatee, String resourceType) {
            this.delegatee = delegatee;
            this.resourceType = resourceType;
        }

        public String getPath() {
            return delegatee.getPath();
        }

        public String getResourceType() {
            return resourceType;
        }

        public ResourceMetadata getResourceMetadata() {
            return delegatee.getResourceMetadata();
        }

        public ResourceProvider getResourceProvider() {
            return delegatee.getResourceProvider();
        }

        public <Type> Type adaptTo(Class<Type> type) {
            return delegatee.adaptTo(type);
        }
    }
}