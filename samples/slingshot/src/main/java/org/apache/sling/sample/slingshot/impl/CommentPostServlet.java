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
package org.apache.sling.sample.slingshot.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(methods="POST", resourceTypes=SlingshotConstants.RESOURCETYPE_COMMENTS)
public class CommentPostServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory factory;

    @Override
    protected void doPost(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response)
    throws ServletException, IOException {
        final String title = request.getParameter(SlingshotConstants.PROPERTY_TITLE);
        final String description = request.getParameter(SlingshotConstants.PROPERTY_DESCRIPTION);

        final String userId = request.getRemoteUser();

        // TODO - check values

        // save comment
        ResourceResolver resolver = null;
        try {
            final Map<String, Object> loginmap = new HashMap<String, Object>();
            loginmap.put(ResourceResolverFactory.USER_IMPERSONATION, userId);
            resolver = factory.getAdministrativeResourceResolver(loginmap);

            final Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_COMMENT);
            properties.put(SlingshotConstants.PROPERTY_TITLE, title);
            properties.put(SlingshotConstants.PROPERTY_DESCRIPTION, description);

            // we try it five times
            PersistenceException exception = null;
            Resource newResource = null;
            for(int i=0; i<5; i++) {
                try {
                    exception = null;
                    final String name = ResourceUtil.createUniqueChildName(request.getResource(), Util.filter(title));
                    newResource = resolver.create(request.getResource(), name, properties);

                    resolver.commit();
                    break;
                } catch ( final PersistenceException pe) {
                    resolver.revert();
                    resolver.refresh();
                    exception = pe;
                }
            }
            if ( exception != null ) {
                throw exception;
            }
            // order node at the top (if jcr based)
            final Node newNode = newResource.adaptTo(Node.class);
            if ( newNode != null ) {
                try {
                    final Node parent = newNode.getParent();
                    final Node firstNode = parent.getNodes().nextNode();
                    if ( !firstNode.getName().equals(newNode.getName()) ) {
                        parent.orderBefore(newNode.getName(), firstNode.getName());
                        newNode.getSession().save();
                    }
                } catch ( final RepositoryException re) {
                    logger.error("Unable to order comment to the top", re);
                }
            }
        } catch ( final LoginException le ) {
            throw new ServletException("Unable to login", le);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }

        // send redirect at the end
        final String path = request.getResource().getParent().getPath();

        response.sendRedirect(request.getContextPath() + path + ".html");
    }

}
