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
package org.apache.sling.servlets.get.impl.helpers;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The <code>XMLRendererServlet</code> renders the current resource in XML
 * on behalf of the {@link org.apache.sling.servlets.get.impl.DefaultGetServlet}.
 *
 * At the moment only JCR nodes can be rendered as XML.
 */
public class XMLRendererServlet extends SlingSafeMethodsServlet {

    public static final String EXT_XML = "xml";

    private static final String SYSVIEW = "sysview";
    private static final String DOCVIEW = "docview";

    @Override
    protected void doGet(SlingHttpServletRequest req,
                         SlingHttpServletResponse resp)
    throws ServletException, IOException {
        final Resource r = req.getResource();

        if (ResourceUtil.isNonExistingResource(r)) {
            throw new ResourceNotFoundException("No data to render.");
        }

        resp.setContentType(req.getResponseContentType());
        resp.setCharacterEncoding("UTF-8");

        // are we included?
        final boolean isIncluded = req.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null;

        final Node node = r.adaptTo(Node.class);
        if ( node != null ) {
            try {
                if ( req.getRequestPathInfo().getSelectorString() == null
                     || req.getRequestPathInfo().getSelectorString().equals(DOCVIEW) ) {
                    // check if response is adaptable to a content handler
                    final ContentHandler ch = resp.adaptTo(ContentHandler.class);
                    if ( ch == null ) {
                        node.getSession().exportDocumentView(node.getPath(), resp.getOutputStream(), false, false);
                    } else {
                        node.getSession().exportDocumentView(node.getPath(), ch, false, false);
                    }
                } else if ( req.getRequestPathInfo().getSelectorString().equals(SYSVIEW) ) {
                    // check if response is adaptable to a content handler
                    final ContentHandler ch = resp.adaptTo(ContentHandler.class);
                    if ( ch == null ) {
                        node.getSession().exportSystemView(node.getPath(), resp.getOutputStream(), false, false);
                    } else {
                        node.getSession().exportSystemView(node.getPath(), ch, false, false);
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NO_CONTENT); // NO Content
                }
            } catch (RepositoryException e) {
                throw new ServletException("Unable to export resource as xml: " + r, e);
            } catch (SAXException e) {
                throw new ServletException("Unable to export resource as xml: " + r, e);
            }
        } else {
            if ( !isIncluded ) {
                resp.sendError(HttpServletResponse.SC_NO_CONTENT); // NO Content
            }
        }
    }
}
