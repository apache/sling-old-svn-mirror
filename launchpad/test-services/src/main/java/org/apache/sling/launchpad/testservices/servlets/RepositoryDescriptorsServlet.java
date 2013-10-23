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
import java.util.Arrays;

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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

/** Test servlet that dumps our repository descriptors */ 
@SuppressWarnings("serial")
@Component(immediate = true)
@Service
@Properties({ @Property(name = "service.description", value = "Repository Descriptors Servlet"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation"),
        @Property(name = "sling.servlet.paths", value = "/testing/RepositoryDescriptors"),
        @Property(name = "sling.servlet.extensions", value = "json")})
public class RepositoryDescriptorsServlet extends SlingSafeMethodsServlet {

    @Reference
    private Repository repository;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) 
    throws ServletException,IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            final JSONWriter w = new JSONWriter(response.getWriter());
            w.setTidy(Arrays.asList(request.getRequestPathInfo().getSelectors()).contains("tidy"));
            w.object();
            w.key("descriptors");
            w.object();
            for(String key : repository.getDescriptorKeys()) {
                w.key(key).value(repository.getDescriptor(key));
            }
            w.endObject();
            w.endObject();
        } catch(JSONException je) {
            throw (IOException)new IOException("JSONException in doGet").initCause(je);
        }
        response.getWriter().flush();
    }
}