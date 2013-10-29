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

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * Test HTML rendering of resources provided by our PlanetResourceProvider
 */
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="HTML renderer for Planet resources"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.resourceTypes", value="sling/test-services/planet"),
    @Property(name="sling.servlet.extensions", value="html"),
    @Property(name="sling.servlet.methods", value="GET")
})
@SuppressWarnings("serial")
public class PlanetResourceRenderingServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        final ValueMap properties = request.getResource().adaptTo(ValueMap.class);
        
        // TODO should escape output - good enough for our tests 
        final PrintWriter pw = response.getWriter();
        pw.println(String.format("<html><head><title>Planet at %s</title></head><body>", request.getResource().getPath()));
        pw.println(String.format("<p>Name: %s</p>", properties.get("name")));
        pw.println(String.format("<p>Distance: %s</p>", properties.get("distance")));
        pw.println("</body></html>");
        pw.flush();
    }
    
}