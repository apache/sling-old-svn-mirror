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
package org.apache.sling.rewriter.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This service tracker stores all services into a hash map.
 */
class HashingServiceTrackerCustomizer<T> extends ServiceTracker {

    public static final class Pair<T> {

        public final ServiceReference reference;
        public final T service;

        public Pair(final ServiceReference r, final T s) {
            this.reference = r;
            this.service = s;
        }
    }

    public static final class Entry<T> {

        public volatile T service;

        public final List<Pair<T>> references = new ArrayList<Pair<T>>();

        public void add(final ServiceReference ref, final T service) {
            references.add(new Pair<T>(ref, service));
            Collections.sort(references, new Comparator<Pair<T>>() {

                @Override
                public int compare(final Pair<T> o1, final Pair<T> o2) {
                    return o2.reference.compareTo(o1.reference);
                }
            });
            if ( references.get(0).reference == ref ) {
                this.service = service;
            }
        }

        public void remove(final ServiceReference ref) {
            if ( !references.isEmpty() ) {
                boolean update = references.get(0).reference == ref;
                final Iterator<Pair<T>> i = references.iterator();
                while ( i.hasNext() ) {
                    final Pair<T> pair = i.next();
                    if ( pair.reference == ref ) {
                        i.remove();
                    }
                }
                if ( update ) {
                    if ( references.isEmpty() ) {
                        this.service = null;
                    } else {
                        this.service = references.get(0).service;
                    }
                }
            }
        }
    }

    /** The services hashed by their name property. */
    private final Map<String, Entry<T>> services = new ConcurrentHashMap<String, Entry<T>>();

    /** The bundle context. */
    protected final BundleContext context;

    public HashingServiceTrackerCustomizer(final BundleContext bc, final String serviceClassName) {
        super(bc, serviceClassName, null);
        this.context = bc;
    }

    public T getFactory(final String type) {
        final Entry<T> entry = services.get(type);
        return entry == null ? null : entry.service;
    }

    private String getType(final ServiceReference ref) {
        final String type = (String) ref.getProperty(FactoryCache.PROPERTY_TYPE);
        return type;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(final ServiceReference reference) {
        final String type = this.getType(reference);
        @SuppressWarnings("unchecked")
        final T factory = (type == null ? null : (T) this.context.getService(reference));
        if ( factory != null ) {
            if ( FactoryCache.LOGGER.isDebugEnabled() ) {
                FactoryCache.LOGGER.debug("Found service {}, type={}.", factory, type);
            }
            synchronized ( this ) {
                Entry<T> entry = this.services.get(type);
                if ( entry == null ) {
                    entry = new Entry<T>();
                    this.services.put(type, entry);
                }
                entry.add(reference, factory);
            }
        }
        return factory;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        final String type = this.getType(reference);
        if ( type != null ) {
            synchronized ( this ) {
                final Entry<T> entry = this.services.get(type);
                if ( entry != null ) {
                    entry.remove(reference);
                }
            }
            this.context.ungetService(reference);
        }
    }
}