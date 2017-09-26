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
package org.apache.sling.launchpad.testservices.jcr;

import java.io.IOException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

/** Servlet used to test HtmlResponse escaping */
@Component(immediate=true, metatype=false)
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name="service.description", value="FullText Query Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.paths", value={
            "/testing/fullTextQuery"
    })
})
/**
 * Outputs paths of nodes matching the specified full-text search
 * 
 * <p>The paths are written in text format, one per line.</p>
 *
 */
public class FullTextQueryServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        
        String queryText = request.getParameter("q");
        if ( queryText == null || queryText.isEmpty() ) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing mandatory 'q' parameter");
            return;
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        
        try {
            Query query = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [nt:base] AS s WHERE CONTAINS(s.*, $queryText)", Query.JCR_SQL2);
            query.bindValue("queryText", session.getValueFactory().createValue(queryText));
            QueryResult result  = query.execute();
            NodeIterator iterator = result.getNodes();
            while( iterator.hasNext() ) {
                response.getWriter().println(iterator.nextNode().getPath());
            }
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
        
    }
    
}
