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

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * The <code>FileServlet</code> spools nt:file nodes
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *          value="Servlet to handle nt:file nodes"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.resourceTypes" value="nt:file"
 * @scr.service
 */
public class FileServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        Resource resource = request.getResource();
        FileObject file = resource.adaptTo(FileObject.class);
        if (file == null) {
            throw new SlingException("Missing mapped object for file "
                + resource.getURI());
        }

        // just render the child content
        Resource jcrContent = request.getResourceResolver().getResource(
            resource, "jcr:content");
        if (jcrContent != null) {
            RequestDispatcher rd = request.getRequestDispatcher(jcrContent);
            rd.include(request, response);
        } else {
            throw new SlingException("File " + resource.getURI()
                + " has no content");
        }
    }
}
