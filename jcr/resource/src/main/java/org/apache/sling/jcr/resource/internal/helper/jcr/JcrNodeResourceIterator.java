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

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.internal.JcrResourceResolver;
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
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** resource manager used to create resources from nodes */
    private JcrResourceResolver resourceResolver;

    /** underlying node iterator to be used for resources */
    private NodeIterator nodes;

    /** The prefetched next iterator entry, null at the end of iterating */
    private Resource nextResult;

    /**
     * Creates an instance using the given resource manager and the nodes
     * provided as a node iterator.
     */
    public JcrNodeResourceIterator(JcrNodeResource parent) {
        try {
            NodeIterator nodes = parent.getNode().getNodes();

            this.resourceResolver = parent.getResourceResolver();
            this.nodes = nodes;
            this.nextResult = seek();
        } catch (RepositoryException re) {
            log.error("<init>: Cannot get children of resource " + parent, re);
            this.resourceResolver = null;
            this.nodes = null;
            this.nextResult = null;
        }
    }

    public JcrNodeResourceIterator(JcrResourceResolver resourceResolver,
            NodeIterator nodes) {
        this.resourceResolver = resourceResolver;
        this.nodes = nodes;
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
                return new JcrNodeResource(resourceResolver, nodes.nextNode());
            } catch (Throwable t) {
                log.error(
                    "seek: Problem creating Resource for next node, skipping",
                    t);
            }
        }

        // no more results
        log.debug("seek: No more nodes, iterator exhausted");
        return null;
    }
}
