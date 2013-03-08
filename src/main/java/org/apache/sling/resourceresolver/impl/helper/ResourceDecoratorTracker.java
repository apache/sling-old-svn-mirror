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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.commons.osgi.ServiceUtil;

/**
 * Helper class to track the resource decorators and keep them sorted by their
 * service ranking.
 */
public class ResourceDecoratorTracker {

    private static final ResourceDecorator[] EMPTY_ARRAY = new ResourceDecorator[0];

    /**
     * The (optional) resource decorators, working copy.
     */
    protected final List<ResourceDecoratorEntry> resourceDecorators = new ArrayList<ResourceDecoratorEntry>();

    /**
     * An array of the above, updates when changes are created.
     */
    private volatile ResourceDecorator[] resourceDecoratorsArray = EMPTY_ARRAY;

    public void close() {
        synchronized (this.resourceDecorators) {
            this.resourceDecorators.clear();
            this.resourceDecoratorsArray = EMPTY_ARRAY;
        }
    }

    /**
     * Decorate a resource.
     */
    public Resource decorate(final Resource resource) {
        Resource result = resource;
        final ResourceDecorator[] decorators = this.resourceDecoratorsArray;
        for (final ResourceDecorator decorator : decorators) {
            final Resource original = result;
            result = decorator.decorate(original);
            if (result == null) {
                result = original;
            }
        }

        // make resource metadata read-only
        result.getResourceMetadata().lock();

        return result;
    }

    /**
     * Bind a resource decorator.
     */
    public void bindResourceDecorator(final ResourceDecorator decorator,
            final Map<String, Object> props) {
        synchronized (this.resourceDecorators) {
            this.resourceDecorators.add(new ResourceDecoratorEntry(decorator,
                    ServiceUtil.getComparableForServiceRanking(props)));
            Collections.sort(this.resourceDecorators);
            updateResourceDecoratorsArray();
        }
    }

    /**
     * Unbind a resouce decorator.
     */
    public void unbindResourceDecorator(final ResourceDecorator decorator,
            final Map<String, Object> props) {
        synchronized (this.resourceDecorators) {
            final Iterator<ResourceDecoratorEntry> i = this.resourceDecorators
                    .iterator();
            while (i.hasNext()) {
                final ResourceDecoratorEntry current = i.next();
                if (current.decorator == decorator) {
                    i.remove();
                    break;
                }
            }
            updateResourceDecoratorsArray();
        }
    }

    /**
     * Updates the ResourceDecorators array, this method is not thread safe and
     * should only be called from a synchronized block.
     */
    private void updateResourceDecoratorsArray() {
        final ResourceDecorator[] decorators;
        if (this.resourceDecorators.size() > 0) {
            decorators = new ResourceDecorator[this.resourceDecorators.size()];
            int index = 0;
            final Iterator<ResourceDecoratorEntry> i = this.resourceDecorators
                    .iterator();
            while (i.hasNext()) {
                decorators[index] = i.next().decorator;
                index++;
            }
        } else {
            decorators = EMPTY_ARRAY;
        }
        this.resourceDecoratorsArray = decorators;
    }

    /**
     * Internal class to keep track of the resource decorators.
     */
    private static final class ResourceDecoratorEntry implements
            Comparable<ResourceDecoratorEntry> {

        final Comparable<Object> comparable;

        final ResourceDecorator decorator;

        public ResourceDecoratorEntry(final ResourceDecorator d,
                final Comparable<Object> comparable) {
            this.comparable = comparable;
            this.decorator = d;
        }

        public int compareTo(final ResourceDecoratorEntry o) {
            return comparable.compareTo(o.comparable);
        }
    }
}
