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
package org.apache.sling.apt.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.apt.parser.SlingAptParser;

/**
* Parses APT structured text files and renders them in HTML
*
* Use this as the default GET servlet for apt requests
*
* TODO for now we have to use this weird extension, added after the
* full filename. We should add a sling.servlet.contentExtension parameter
* to the servlet selection mechanism, and use that to tell sling to map
* an html request to an apt file using this servlet.
*
*/
@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="Sling APT Servlet"),
    @Property(name="sling.servlet.resourceTypes", value="sling/servlet/default"),
    @Property(name="sling.servlet.extensions", value="aptml")
})
public class SlingAptServlet extends SlingSafeMethodsServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 39312431611309023L;
    @Reference
    protected SlingAptParser parser;

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final InputStream stream = request.getResource().adaptTo(InputStream.class);
        if(stream == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Resource does not adapt to an InputStream: " + request.getResource()
            );
            return;
        }

        // TODO which encoding to use for input??
        // Should find out from the JCR resource node
        final String encoding = "UTF-8";
        final Reader r = new InputStreamReader(stream, encoding);
        final Writer w = new StringWriter();
        try {
            parser.parse(r, w);
        } catch(Exception e) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Exception while parsing APT content: " + e
            );
        }

        final byte [] bytes = w.toString().getBytes(encoding);
        response.setContentType("text/html");
        response.setCharacterEncoding(encoding);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}