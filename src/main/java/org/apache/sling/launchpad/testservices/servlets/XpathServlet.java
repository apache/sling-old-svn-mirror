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
package org.apache.sling.launchpad.testservices.servlets;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.xml.sax.InputSource;

/**
 * The <tt>XpathServlet</tt> evaluates a simple XML document using an XPath expression
 * 
 */
@SlingServlet(paths = "/bin/xpath", extensions = "xml")
public class XpathServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String XML_INPUT = "<content><name>JAXP</name></content>";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();

            String result = xpath.evaluate("/content/name", new InputSource(new StringReader(XML_INPUT)));

            response.setContentType("text/plain");
            response.getWriter().write(result);
        } catch (XPathExpressionException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

}
