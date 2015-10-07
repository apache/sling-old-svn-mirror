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
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.internal.HelperData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrNodeResourceIterator</code> class is a resource iterator,
 * which returns resources for each node of an underlying
 * <code>NodeIterator</code>. Nodes in the node iterator which cannot be
 * accessed or for which a resource cannot be created are skipped.
 */
public class JcrNodeResourceIterator implements Iterator<Resource> {

    /** default log */
    private static final Logger LOGGER = LoggerFactory.getLogger(JcrNodeResourceIterator.class);

    /** resource resolver used to create resources from nodes */
    private final ResourceResolver resourceResolver;

    /** underlying node iterator to be used for resources */
    private final NodeIterator nodes;

    /** The prefetched next iterator entry, null at the end of iterating */
    private Resource nextResult;

    private final HelperData helper;

    private final String parentPath;

    private final String parentVersion;

    /**
     * Creates an instance using the given resource manager and the nodes
     * provided as a node iterator.
     *
     * @param resourceResolver the resolver
     * @param nodes the node iterator
     * @param helper the helper
     */
    public JcrNodeResourceIterator(final ResourceResolver resourceResolver,
                                   final NodeIterator nodes,
                                   final HelperData helper) {
        this(resourceResolver, null, null, nodes, helper);
    }

    /**
     * Creates an instance using the given resource manager and the nodes
     * provided as a node iterator. Paths of the iterated resources will be
     * concatenated from the parent path, node name and the version number.
     *
     * @param resourceResolver the resolver
     * @param parentPath the parent path
     * @param parentVersion the parent version
     * @param nodes the node iterator
     * @param helper the helper
     */
    public JcrNodeResourceIterator(final ResourceResolver resourceResolver,
                                   final String parentPath,
                                   final String parentVersion,
                                   final NodeIterator nodes,
                                   final HelperData helper) {
        this.resourceResolver = resourceResolver;
        this.parentPath = parentPath;
        this.parentVersion = parentVersion;
        this.nodes = nodes;
        this.helper = helper;
        this.nextResult = seek();
    }

    public boolean hasNext() {
        return nextResult != null;
    }

    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Resource result = nextResult;
        nextResult = seek();
        return result;
    }

    /**
     * Throws <code>UnsupportedOperationException</code> as this method is not
     * supported by this implementation.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private Resource seek() {
        while (nodes.hasNext()) {
            try {
                final Node n = nodes.nextNode();
                final String path = getPath(n);
                if ( path != null ) {
                    final Resource resource = new JcrNodeResource(resourceResolver,
                        path, parentVersion, n, helper);
                    LOGGER.debug("seek: Returning Resource {}", resource);
                    return resource;
                }
            } catch (final Throwable t) {
                LOGGER.error(
                    "seek: Problem creating Resource for next node, skipping",
                    t);
            }
        }

        // no more results
        LOGGER.debug("seek: No more nodes, iterator exhausted");
        return null;
    }

    private String getPath(final Node node) throws RepositoryException {
        final String path;
        if (parentPath == null) {
            path = node.getPath();
        } else {
            path = "/".equals(parentPath) ? '/' + node.getName() : parentPath + '/' + node.getName();
        }
        return helper.pathMapper.mapJCRPathToResourcePath(path);
    }
}
