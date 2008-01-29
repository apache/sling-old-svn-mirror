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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl;
import org.apache.sling.jcr.resource.internal.helper.Descendable;
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

    protected static final String ACTION_READ = "read";

    private final JcrResourceResolverFactoryImpl factory;

    private final Session session;

    public JcrResourceProvider(JcrResourceResolverFactoryImpl factory,
            Session session) {
        this.factory = factory;
        this.session = session;
    }

    // ---------- ResourceProvider interface ----------------------------------

    public String[] getRoots() {
        return new String[] { "/" };
    }

    public Resource getResource(HttpServletRequest request, String path)
            throws SlingException {
        return getResource(path);
    }

    public Resource getResource(String path) throws SlingException {

        try {
            return createResource(path);
        } catch (RepositoryException re) {
            throw new SlingException("Problem retrieving node based resource "
                + path, re);
        }

    }

    public Iterator<Resource> listChildren(Resource parent) {
        if (parent instanceof Descendable) {
            return ((Descendable) parent).listChildren();
        }

        try {
            parent = getResource(parent.getPath());
            if (parent instanceof Descendable) {
                return ((Descendable) parent).listChildren();
            }
        } catch (SlingException se) {
            log.warn("listChildren: Error trying to resolve parent resource "
                + parent.getPath(), se);
        }

        // return an empty iterator if parent has no node
        List<Resource> empty = Collections.emptyList();
        return empty.iterator();
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
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    private Resource createResource(String path) throws RepositoryException {
        if (itemExists(path)) {
            Item item = getSession().getItem(path);
            if (item.isNode()) {
                log.debug(
                    "createResource: Found JCR Node Resource at path '{}'",
                    path);
                return new JcrNodeResource(this, (Node) item);
            }

            log.debug(
                "createResource: Found JCR Property Resource at path '{}'",
                path);
            return new JcrPropertyResource(this, path, (Property) item);
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
     * @throws RepositoryException
     * @throws AccessControlException If the item really exists but this content
     *             manager's session has no read access to it.
     */
    public boolean itemExists(String path) throws RepositoryException {
        if (factory.itemReallyExists(getSession(), path)) {
            checkPermission(path, ACTION_READ);
            return true;
        }

        return false;
    }

    /**
     * @param path
     * @param actions
     * @throws RepositoryException
     * @throws AccessControlException if this manager does not have the
     *             permission for the listed action(s).
     */
    protected void checkPermission(String path, String actions)
            throws RepositoryException {
        getSession().checkPermission(path, actions);
    }

}
