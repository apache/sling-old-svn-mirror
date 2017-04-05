/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.api.wrappers;

import java.util.Iterator;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Wrapper class for {@code Iterator}s, that forwards all method calls to the wrapped {@code Iterator}.
 *
 * @param <T> the type of objects this {@code Iterator} holds
 */
@ConsumerType
public class IteratorWrapper<T> implements Iterator<T> {

    private final Iterator<T> wrapped;

    /**
     * Creates a wrapping iterator, delegating all method calls to the given {@code wrappedIterator}.
     *
     * @param wrappedIterator the wrapped iterator
     */
    public IteratorWrapper(Iterator<T> wrappedIterator) {
        this.wrapped = wrappedIterator;
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public T next() {
        return wrapped.next();
    }

    @Override
    public void remove() {
        wrapped.remove();
    }
}
