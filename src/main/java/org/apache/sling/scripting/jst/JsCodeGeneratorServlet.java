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
package org.apache.sling.scripting.jst;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/**
 * A SlingSafeMethodsServlet that renders JST templates as javascript code
 *
 */
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="Apache Sling JST code generator servlet"),
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.extensions", value="js"),
    @Property(name="sling.servlet.selectors", value="jst")
})
@SuppressWarnings("serial")
public class JsCodeGeneratorServlet extends SlingSafeMethodsServlet {

    private final JsCodeGenerator codeGenerator = new JsCodeGenerator();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
    throws ServletException, IOException {
        final Reader scriptReader = getReader(request.getResource());
        final PrintWriter output = new PrintWriter(response.getOutputStream());
        response.setContentType(" application/x-javascript");
        codeGenerator.generateCode(scriptReader, output);
        output.flush();
    }

    /** Return a Reader for the given Resource */
    static Reader getReader(Resource resource) throws IOException {
        if(resource == null) {
            throw new IllegalArgumentException("Resource is null");
        }
        final InputStream ins = resource.adaptTo(InputStream.class);
        if (ins == null) {
            throw new IOException("Resource " + resource.getPath() + " cannot be adapted to an InputStream");
        }

        String enc = (String) resource.getResourceMetadata().get(ResourceMetadata.CHARACTER_ENCODING);
        if (enc == null) {
            enc = "UTF-8";
        }
        return new InputStreamReader(ins, enc);
    }
}
