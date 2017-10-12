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
package org.apache.sling.testing.samples.bundlewit.impl;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.testing.samples.bundlewit.api.ResourceMimeTypeDetector;

/** Servlet that uses the ResourceMimeTypeDetector to output
 *  the mime-type of the current Resource.
 */
@SlingServlet(
        resourceTypes="sling/servlet/default",
        selectors="mimetype",
        extensions="txt",
        methods="GET")
public class MimeTypeServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 7373752152329079226L;
    
    public static final String PREFIX = MimeTypeServlet.class.getSimpleName() + " mime-type is ";
    
    @Reference
    ResourceMimeTypeDetector detector;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        final String mimeType = detector.getMimeType(request.getResource());
        response.getWriter().write(PREFIX); 
        response.getWriter().write(String.valueOf(mimeType));
    }
}