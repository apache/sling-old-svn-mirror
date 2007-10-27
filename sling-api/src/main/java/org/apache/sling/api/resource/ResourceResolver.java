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
package org.apache.sling.api.resource;

import java.util.Iterator;

import javax.servlet.ServletRequest;

import org.apache.sling.api.exceptions.SlingException;

/**
 * The <code>ResourceResolver</code> defines the service API which may be used
 * to resolve {@link Resource} objects. The resource resolver is available to
 * the request processing servlet through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()}
 * method.
 */
public interface ResourceResolver {

    /**
     * Resolves the resource from the given <code>ServletRequest</code>.
     *
     * @param request The servlet request object used to resolve the resource
     *            for.
     * @return The {@link Resource} for the request.
     * @throws ResourceNotFoundException May be thrown if no resource can be
     *             resolved for for the request.
     * @throws SlingException May be thrown if another error occurrs.
     */
    Resource resolve(ServletRequest request) throws SlingException,
            ResourceNotFoundException;

    /**
     * Returns a {@link Resource} object for data located at the given path.
     * <p>
     * This specification does not define the location for resources or the
     * semantics for resource paths. For an implementation reading content from
     * a Java Content Repository, the path could be a
     * <code>javax.jcr.Item</code> path from which the resource object is
     * loaded.
     *
     * @param path The absolute path to the resource object to be loaded. The
     *            path may contain relative path specifiers like <code>.</code>
     *            (current location) and <code>..</code> (parent location),
     *            which are resolved by this method. If the path is relative,
     *            that is the first character is not a slash, a
     *            <code>ResourceNotFoundException</code> is thrown.
     * @return The <code>Content</code> object loaded from the path.
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this resource manager
     *             has no read access to the item.
     * @throws ResourceNotFoundException If no resource can be resolved at the
     *             resolved path or if the path is not an absolute path.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(String path) throws ResourceNotFoundException,
            SlingException;

    /**
     * Returns a {@link Resource} object for data located at the given path.
     * <p>
     * This specification does not define the location for resources or the
     * semantics for resource paths. For an implementation reading content from
     * a Java Content Repository, the path could be a
     * <code>javax.jcr.Item</code> path from which the resource object is
     * loaded.
     *
     * @param base The base {@link Resource} against which a relative path
     *            argument given by <code>path</code> is resolved. This
     *            parameter may be <code>null</code> if the <code>path</code>
     *            is known to be absolute.
     * @param path The path to the resource object to be loaded. If the path is
     *            relative, i.e. does not start with a slash (<code>/</code>),
     *            the resource relative to the given <code>base</code>
     *            resource is returned. The path may contain relative path
     *            specifiers like <code>.</code> (current location) and
     *            <code>..</code> (parent location), which are resolved by
     *            this method.
     * @return The <code>Content</code> object loaded from the path.
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this resource manager
     *             has no read access to the item.
     * @throws ResourceNotFoundException If no resource can be resolved at the
     *             resolved path.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(Resource base, String path)
            throws ResourceNotFoundException, SlingException;

    /**
     * Returns an <code>Iterator</code> of {@link Content} objects loaded from
     * the children of the given <code>content</code>.
     * <p>
     * This specification does not define what the term "child" means. This is
     * left to the implementation to define. For example an implementation
     * reading content from a Java Content Repository, the children could be the
     * {@link Content} objects loaded from child items of the
     * <code>javax.jcr.Item</code> of the given <code>content</code>.
     *
     * @param content The {@link Content content object} whose children are
     *            requested. If <code>null</code> the children of this
     *            request's content (see {@link #getContent()}) are returned.
     * @return An <code>Iterator</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is
     *             <code>null</code>.
     * @throws SlingException If any error occurs acquiring the child resource
     *             iterator.
     */
    Iterator<Resource> listChildren(Resource parent) throws SlingException;

}
