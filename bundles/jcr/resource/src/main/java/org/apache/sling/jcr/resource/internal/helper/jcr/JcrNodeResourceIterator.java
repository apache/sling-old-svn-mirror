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

import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
    private ResourceResolver resourceResolver;

    /** underlying node iterator to be used for resources */
    private NodeIterator nodes;

    /** The prefetched next iterator entry, null at the end of iterating */
    private Resource nextResult;

    private final ClassLoader dynamicClassLoader;

    /**
     * Creates an instance using the given resource manager and the nodes
     * provided as a node iterator.
     */
    public JcrNodeResourceIterator(final ResourceResolver resourceResolver,
                                   final NodeIterator nodes,
                                   final ClassLoader dynamicClassLoader) {
        this.resourceResolver = resourceResolver;
        this.nodes = nodes;
        this.dynamicClassLoader = dynamicClassLoader;
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
                Resource resource = new JcrNodeResource(resourceResolver,
                    nodes.nextNode(), dynamicClassLoader);
                LOGGER.debug("seek: Returning Resource {}", resource);
                return resource;
            } catch (final ItemNotFoundException infe) {
                LOGGER.debug(
                                "seek: Problem creating Resource for next node, skipping",
                                infe);
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
}
