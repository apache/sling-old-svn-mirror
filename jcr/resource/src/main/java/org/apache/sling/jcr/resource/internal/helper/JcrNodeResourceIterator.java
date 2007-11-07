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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.internal.JcrResourceManager;

public class JcrNodeResourceIterator implements Iterator<Resource> {

    private JcrResourceManager resourceManager;
    private NodeIterator nodes;

    private Resource nextResult = seek();

    public JcrNodeResourceIterator(JcrResourceManager resourceManager, NodeIterator nodes) {
        this.resourceManager = resourceManager;
        this.nodes = nodes;
        this.nextResult = seek();
    }

    public boolean hasNext() {
        return nextResult != null;
    }

    public Resource next() {
        if (nextResult == null) {
            throw new NoSuchElementException();
        }

        Resource result = nextResult;
        nextResult = seek();
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private Resource seek() {
        while (nodes.hasNext()) {
            try {
                return new JcrNodeResource(resourceManager, nodes.nextNode());
            } catch (RepositoryException re) {
                // TODO: log this situation and continue mapping
            } catch (ObjectContentManagerException ocme) {
                // TODO: log this situation and continue mapping
            } catch (Throwable t) {
                // TODO: log this situation and continue mapping
            }
        }

        // no more results
        return null;
    }

}
