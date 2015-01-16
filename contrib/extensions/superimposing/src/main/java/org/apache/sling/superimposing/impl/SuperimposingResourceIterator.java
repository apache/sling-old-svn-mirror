/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.superimposing.impl;

import org.apache.sling.api.resource.Resource;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over children of a {@link SuperimposingResource} by iterating over the children of the source resource
 * and remapping and wrapping all children in SuperimposingResource instances.
 */
public class SuperimposingResourceIterator implements Iterator<Resource> {

    private final SuperimposingResourceProviderImpl superimposingProvider;
    private final Iterator<Resource> decoratee;

    private Resource next;

    SuperimposingResourceIterator(SuperimposingResourceProviderImpl superimposingProvider, Iterator<Resource> decoratee) {
        this.superimposingProvider = superimposingProvider;
        this.decoratee = decoratee;
        seek();
    }

    private void seek() {
        next = null;
        while (next == null && decoratee.hasNext()) {
            final Resource resource = decoratee.next();
            final String superimposingPath = SuperimposingResourceProviderImpl.reverseMapPath(superimposingProvider, resource.getPath());
            if (null != superimposingPath) {
                next = new SuperimposingResource(resource, superimposingPath);
            }
        }
    }

    public boolean hasNext() {
        return null != next;
    }

    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final Resource current = next;
        seek();
        return current;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
