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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

/**
 * Resource iterator handling the decoration of resources.
 */
public class ResourceIteratorDecorator implements Iterator<Resource> {

    private final ResourceDecoratorTracker tracker;

    private final Iterator<Resource> iterator;

    public ResourceIteratorDecorator(final ResourceDecoratorTracker tracker,
            final Iterator<Resource> iterator) {
        this.tracker = tracker;
        this.iterator = iterator;
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    @Override
    public Resource next() {
        final Resource rsrc = this.iterator.next();
        rsrc.getResourceMetadata().setResolutionPath(rsrc.getPath());

        return this.tracker.decorate(rsrc);
    }

    /**
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        this.iterator.remove();
    }
}
