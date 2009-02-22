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
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.Iterator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceProvider</code> is the main resource provider of this
 * bundle providing access to JCR resources. This resoure provider is created
 * for each <code>JcrResourceResolver</code> instance and is bound to the JCR
 * session for a single request.
 */
public class JcrResourceProvider implements ResourceProvider {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Session session;
    private final JcrResourceTypeProvider[] resourceTypeProviders;

    public JcrResourceProvider(Session session, JcrResourceTypeProvider[] resourceTypeProviders) {
        this.session = session;
        this.resourceTypeProviders = resourceTypeProviders;
    }

    // ---------- ResourceProvider interface ----------------------------------

    public String[] getRoots() {
        return new String[] { "/" };
    }

    public Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path) throws SlingException {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path)
            throws SlingException {

        try {
            return createResource(resourceResolver, path);
        } catch (RepositoryException re) {
            throw new SlingException("Problem retrieving node based resource "
                + path, re);
        }

    }

    public Iterator<Resource> listChildren(Resource parent) {

        JcrItemResource parentItemResource;

        // short cut for known JCR resources
        if (parent instanceof JcrItemResource) {

            parentItemResource = (JcrItemResource) parent;

        } else {

            // try to get the JcrItemResource for the parent path to list
            // children
            try {
                parentItemResource = createResource(
                    parent.getResourceResolver(), parent.getPath());
            } catch (RepositoryException re) {
                parentItemResource = null;
            }

        }

        // return children if there is a parent item resource, else null
        return (parentItemResource != null)
                ? parentItemResource.listChildren()
                : null;
    }

    public Session getSession() {
        return session;
    }

    // ---------- implementation helper ----------------------------------------

    /**
     * Creates a <code>Resource</code> instance for the item found at the
     * given path. If no item exists at that path or the item does not have
     * read-access for the session of this resolver, <code>null</code> is
     * returned.
     *
     * @param path The absolute path
     * @return The <code>Resource</code> for the item at the given path.
     * @throws RepositoryException If an error occurrs accessingor checking the
     *             item in the repository.
     */
    private JcrItemResource createResource(ResourceResolver resourceResolver,
            String path) throws RepositoryException {
        if (itemExists(path)) {
            Item item = getSession().getItem(path);
            if (item.isNode()) {
                log.debug(
                    "createResource: Found JCR Node Resource at path '{}'",
                    path);
                return new JcrNodeResource(resourceResolver, (Node) item, resourceTypeProviders);
            }

            log.debug(
                "createResource: Found JCR Property Resource at path '{}'",
                path);
            return new JcrPropertyResource(resourceResolver, path,
                (Property) item, resourceTypeProviders);
        }

        log.debug("createResource: No JCR Item exists at path '{}'", path);
        return null;
    }

    /**
     * Checks whether the item exists and this content manager's session has
     * read access to the item. If the item does not exist, access control is
     * ignored by this method and <code>false</code> is returned.
     *
     * @param path The path to the item to check
     * @return <code>true</code> if the item exists and this content manager's
     *         session has read access. If the item does not exist,
     *         <code>false</code> is returned ignoring access control.
     */
    public boolean itemExists(String path) {

        try {
            return getSession().itemExists(path);
        } catch (RepositoryException re) {
            log.debug("itemExists: Error checking for existence of {}: {}",
                path, re.toString());
            return false;
        }

    }
}
