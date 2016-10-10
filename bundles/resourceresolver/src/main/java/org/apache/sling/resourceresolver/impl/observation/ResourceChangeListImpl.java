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
package org.apache.sling.resourceresolver.impl.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeList;

public class ResourceChangeListImpl implements ResourceChangeList {

    private final String[] searchPath;

    private boolean locked = false;

    private final ArrayList<ResourceChange> list = new ArrayList<>();

    public ResourceChangeListImpl(final String[] searchPath) {
        this.searchPath = searchPath;
    }

    public void lock() {
        this.locked = true;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<ResourceChange> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(final ResourceChange e) {
        if ( this.locked ) {
            throw new UnsupportedOperationException();
        }
        return list.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends ResourceChange> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends ResourceChange> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceChange get(final int index) {
        return list.get(index);
    }

    @Override
    public ResourceChange set(final int index, final ResourceChange element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(final int index, final ResourceChange element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceChange remove(final int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(final Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<ResourceChange> listIterator() {
        return new UnmodifiableListIterator(list.listIterator());
    }

    @Override
    public ListIterator<ResourceChange> listIterator(final int index) {
        return new UnmodifiableListIterator(list.listIterator(index));
    }

    @Override
    public List<ResourceChange> subList(final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public String[] getSearchPath() {
        return this.searchPath.clone();
    }

    private static final class UnmodifiableListIterator implements ListIterator<ResourceChange> {

        private final ListIterator<ResourceChange> iterator;

        public UnmodifiableListIterator(final ListIterator<ResourceChange> i) {
            this.iterator = i;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public ResourceChange next() {
            return iterator.next();
        }

        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        @Override
        public ResourceChange previous() {
            return iterator.previous();
        }

        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(final ResourceChange e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(final ResourceChange e) {
            throw new UnsupportedOperationException();
        }
    }
}
