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
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Repository;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/** Test servlet that dumps our repository descriptors */ 
@SuppressWarnings("serial")
@Component(immediate = true)
@Service
@Properties({ @Property(name = "service.description", value = "Repository Descriptors Servlet"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation"),
        @Property(name = "sling.servlet.paths", value = "/testing/RepositoryDescriptors"),
        @Property(name = "sling.servlet.extensions", value = "txt")})
public class RepositoryDescriptorsServlet extends SlingSafeMethodsServlet {

    @Reference
    private Repository repository;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException,IOException {
        final PrintWriter pw = response.getWriter();
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        
        for(String key : repository.getDescriptorKeys()) {
            final String v = repository.getDescriptor(key);
            pw.print(key);
            pw.print("=");
            pw.println(v);
        }
        pw.flush();
    }
    
}