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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPathExpressionException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The <tt>SaxServlet</tt> evaluates a simple XML document using a SAX handler
 * 
 */
@SlingServlet(paths = "/bin/sax", extensions = "xml")
public class SaxServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final String XML_INPUT = "<content><name>SAX</name></content>";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            SimpleHandler handler = new SimpleHandler();

            parser.parse(new InputSource(new StringReader(XML_INPUT)), handler);

            response.setContentType("text/plain");
            response.getWriter().write(handler.getValue());
        } catch (ParserConfigurationException e) {
            throw new ServletException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    static class SimpleHandler extends DefaultHandler {

        private String value;
        private boolean matched;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            matched = "name".equals(qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

            if (matched) {
                value = new String(ch, start, length);
            }
        }

        public String getValue() {
            return value;
        }
    }

}
