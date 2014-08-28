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

/**
 * The <code>AbstractResourceVisitor</code> helps in traversing a
 * resource tree by decoupling the actual traversal code
 * from application code. Concrete subclasses should implement
 * the {@link ResourceVisitor#visit(Resource)} method.
 * 
 * @since 2.2
 */
public abstract class AbstractResourceVisitor {

    /**
     * Visit the given resource and all its descendants.
     * @param res The resource
     */
    public void accept(final Resource res) {
        if (res != null) {
            this.visit(res);
            this.traverseChildren(res.listChildren());
        }
    }

    /**
     * Visit the given resources.
     * @param children The list of resources
     */
    protected void traverseChildren(final Iterator<Resource> children) {
        while (children.hasNext()) {
            final Resource child = children.next();

            accept(child);
        }
    }

    /**
     * Implement this method to do actual work on the resources.
     * @param res The resource
     */
    protected abstract void visit(final Resource res);
}
