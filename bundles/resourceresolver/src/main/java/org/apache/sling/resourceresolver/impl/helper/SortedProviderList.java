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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.resourceresolver.impl.tree.ProviderHandler;
import org.apache.sling.resourceresolver.impl.tree.ResourceProviderFactoryHandler;
import org.apache.sling.resourceresolver.impl.tree.ResourceProviderHandler;

/**
 * Helper class to get a sorted list of resource providers which implement
 * a specific feature interface.
 */
public class SortedProviderList<T> {

    /** The feature interface class. */
    private final Class<T> genericClass;

    /** Sorted list of providers and factories. */
    private Entry[] sortedList = new Entry[0];

    /**
     * We need the class to do the instanceof test for providers
     * returned by the factory.
     */
    public SortedProviderList(final Class<T> genericClass) {
        this.genericClass = genericClass;
    }

    public interface Filter<T> {

        boolean select(ProviderHandler handler, T provider);
    }

    /**
     * Return an iterator for the current sorted list of providers
     * implementing the feature interface.
     */
    public Iterator<T> getProviders(final ResourceResolverContext ctx, final Filter<T> filter) {
        return new Iterator<T>() {

            private final Entry[] list = sortedList;

            private int index = 0;

            private Object nextObject = seek();

            private Object seek() {
                Object result;
                if ( this.index < list.length ) {
                    final Entry entry = list[this.index];
                    result = entry.object;
                    this.index++;
                    if ( result instanceof ResourceProviderFactoryHandler ) {
                        result = ((ResourceProviderFactoryHandler)result).getResourceProvider(ctx);
                        if ( !genericClass.isAssignableFrom(result.getClass())) {
                            result = null;
                        }
                    }
                    if ( result != null && filter != null && !filter.select(entry.handler, (T)result)) {
                        result = null;
                    }
                    if ( result == null ) {
                        result = seek();
                    }
                } else {
                    result = null;
                }
                return result;
            }

            /**
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return this.nextObject != null;
            }

            /**
             * @see java.util.Iterator#next()
             */
            @SuppressWarnings("unchecked")
            public T next() {
                if ( this.nextObject == null ) {
                    throw new NoSuchElementException();
                }
                final Object result = this.nextObject;
                this.nextObject = seek();
                return (T)result;
            }

            /**
             * @see java.util.Iterator#remove()
             */
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Add an object to the list
     */
    private synchronized void addToList(final Object obj,
                    final ProviderHandler handler) {
        final List<Entry> list = new ArrayList<Entry>();
        list.addAll(Arrays.asList(this.sortedList));
        list.add(new Entry(obj, handler));
        Collections.sort(list);
        this.sortedList = list.toArray(new Entry[list.size()]);
    }

    /**
     * Remove an object from the list
     */
    private synchronized void removeFromList(final Object obj) {
        final List<Entry> list = new ArrayList<Entry>();
        list.addAll(Arrays.asList(this.sortedList));
        final Iterator<Entry> i = list.iterator();
        while ( i.hasNext() ) {
            final Entry entry = i.next();
            if ( entry.object.equals(obj) ) {
                i.remove();
                break;
            }
        }
        this.sortedList = list.toArray(new Entry[list.size()]);
    }

    /**
     * Add a resource provider
     * This first checks if it implements the feature interface.
     */
    public void add(final ResourceProviderHandler rpHandler) {
        if ( genericClass.isAssignableFrom(rpHandler.getResourceProvider().getClass())) {
            this.addToList(rpHandler.getResourceProvider(), rpHandler);
        }
    }

    /**
     * Add a resource provider factory. We don't know yet if the
     * resource provider implements the feature interface.
     * This will be checked on demand.
     */
    public void add(final ResourceProviderFactoryHandler factory) {
        this.addToList(factory, factory);
    }

    /**
     * Remove a resource provider.
     */
    public void remove(final ResourceProviderHandler rpHandler) {
        if ( genericClass.isAssignableFrom(rpHandler.getResourceProvider().getClass())) {
            this.removeFromList(rpHandler.getResourceProvider());
        }
    }

    /**
     * Remove a resource provider factory.
     */
    public void remove(final ResourceProviderFactoryHandler factory) {
        this.removeFromList(factory);
    }
    
    /**
     * returns the ProviderHandler for a specific resource provider
     */
    public ProviderHandler getProviderHandler ( ResourceResolverContext ctx, ResourceProvider resourceProvider )
    {
        ProviderHandler returnValue = null;
        final List<Entry> list = new ArrayList<Entry>();
        list.addAll(Arrays.asList(this.sortedList));
        final Iterator<Entry> i = list.iterator();
        while ( i.hasNext() ) {
            final Entry entry = i.next();
            if ( entry.handler.getResourceProvider(ctx).equals(resourceProvider) ) {
                returnValue = entry.handler;
                break;
            }
        }
        return returnValue;
    }

    private static final class Entry implements Comparable<Entry> {

        private final String path;

        public final ProviderHandler handler;

        public final Object object;

        public Entry(final Object object,
                     final ProviderHandler handler) {
            this.object = object;
            if ( handler.getRoots() != null ) {
                this.path = handler.getRoots()[0];
            } else {
                this.path = "";
            }
            this.handler = handler;
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final Entry other) {
            int result = this.path.compareTo(other.path);
            if ( result == 0 ) {
                result = this.handler.getServiceId().compareTo(other.handler.getServiceId());
            }
            return result;
        }
    }
}
