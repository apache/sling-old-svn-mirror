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
package org.apache.sling.sample.slingshot.impl;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.sample.slingshot.Constants;

/**
 * This opting servlet "converts" the standard resource types
 * nt:file, sling:Folder and nt:filder to the resource types used inside
 * Slingshot.
 *
 * We only accept requests to those resource types and only
 * if the resource is within the Slingshot albums folder.
 * The selector needs to be "slingshot"
 *
 */
@SuppressWarnings("serial")
@Component(metatype=false)
@Service(value=javax.servlet.Servlet.class)
@Properties({
   @Property(name="sling.servlet.resourceTypes",value={Constants.RESOURCETYPE_FILE,
                                                       Constants.RESOURCETYPE_FOLDER,
                                                       Constants.RESOURCETYPE_EXT_FOLDER}),
   @Property(name="service.description",
             value="Apache Sling - Slingshot Resource Type Conversion Servlet")
})
public class ResourceTypeConversionServlet
    extends SlingAllMethodsServlet
    implements OptingServlet {

    @Property(name="sling.servlet.extensions")
    protected static final String EXTENSION = "html";

    @Property(name="sling.servlet.selectors")
    protected static final String SELECTOR = "slingshot";

    /**
     * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
     */
    public boolean accepts(SlingHttpServletRequest request) {
        // check everything again (except the resource type)
        return request.getResource().getPath().startsWith(Constants.ALBUMS_ROOT)
               && EXTENSION.equals(request.getRequestPathInfo().getExtension())
               && SELECTOR.equals(request.getRequestPathInfo().getSelectorString());
    }

    private String getResourceType(final Resource resource) {
        if ( ResourceUtil.isA(resource, Constants.RESOURCETYPE_FILE) ) {
            return Constants.RESOURCETYPE_PHOTO;
        }
        return Constants.RESOURCETYPE_ALBUM;
    }

    /**
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
     */
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
    throws ServletException, IOException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        // force new resource type
        options.setForceResourceType(this.getResourceType(request.getResource()));
        // remove all selectors
        options.setReplaceSelectors("");
        request.getRequestDispatcher(request.getResource(), options).forward(request, response);
    }
}
