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
package org.apache.sling.bundleresource.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;

/**
 * The <code>BundleResourceCache</code> implements a simple caching for
 * resources provided from a bundle. Each {@link BundleResourceProvider}
 * instance uses an instance of this class to access the bundle resources (or
 * bundle entries) through the cache.
 * <p>
 * The cache on the one hand caches single entries as URLs. The other part of
 * the cache is for the child entries of a given bundle entry path. This caches
 * lists of strings (entry path).
 * <p>
 * Currently the cache limits are fixed at {@value #CACHE_SIZE} for the entries
 * cache and at {@value #LIST_CACHE_SIZE} for the child entries cache.
 */
class BundleResourceCache {

    /**
     * The maximum size of the single entry cache (value is 50).
     */
    private static final int CACHE_SIZE = 50;

    /**
     * The maximum size of the child entry cache (value is 20).
     */
    private static final int LIST_CACHE_SIZE = 20;

    /**
     * Sentinel for the single entry cache representing a missing entry to
     * prevent looking for non-existing bundle entries multiple times (value is
     * "file:///not_found").
     */
    private static final URL NOT_FOUND_URL;

    /**
     * Sentinel for the child entry cache representing a missing child list for
     * a given path to prevent looking for non-existing bundle entries multiple
     * times (value is an empty list).
     */
    private static final List<String> NOT_FOUND_CHILDREN = Collections.<String> emptyList();

    /**
     * Single entry cache. This is a synchronized map with a size limit.
     */
    private final Map<String, URL> cache;

    /**
     * The child entry cache. This is a synchronized map with a size limit.
     */
    private final Map<String, List<String>> listCache;

    /**
     * The Bundle providing the resource entries.
     */
    private final Bundle bundle;

    // static initializer setting the NOT_FOUND_URL. Because the
    // constructor may throw an exception we use a static initializer
    // which fails the class initialization in the unlikely case
    // of the URL constructor failing.
    static {
        try {
            NOT_FOUND_URL = new URL("file:/not_found");
        } catch (MalformedURLException mue) {
            throw new ExceptionInInitializerError(mue);
        }
    }

    /**
     * Creates a new instance of this class providing access to the entries in
     * the given <code>bundle</code>.
     *
     * @param bundle
     */
    BundleResourceCache(Bundle bundle) {
        this.bundle = bundle;

        // create the limited maps wrapping in synchronized maps
        this.cache = Collections.synchronizedMap(new BundleResourceMap<String, URL>(
            CACHE_SIZE));
        this.listCache = Collections.synchronizedMap(new BundleResourceMap<String, List<String>>(
            LIST_CACHE_SIZE));
    }

    /**
     * Returns the <code>Bundle</code> to which this instance provides access.
     */
    Bundle getBundle() {
        return bundle;
    }

    /**
     * Returns the entry in the underlying bundle at the given path. This path
     * is assumed to be an absolute path. If relative it is resolved relative to
     * the bundle root.
     * <p>
     * This method is backed by the <code>Bundle.getEntry(String)</code>
     * method.
     *
     * @param path The path to the bundle entry to return
     * @return The URL to access the bundle entry or <code>null</code> if the
     *         bundle does not contain the request entry.
     */
    URL getEntry(String path) {
        URL url = cache.get(path);
        if (url == null) {
            url = bundle.getEntry(path);

            if (url == null) {
                url = NOT_FOUND_URL;
            }

            cache.put(path, url);
        }

        return (url == NOT_FOUND_URL) ? null : url;
    }

    /**
     * Returns a list of bundle entry paths considered children of the given
     * <code>parentPath</code>. This parent path is assumed to be an absolute
     * path. If relative it is resolved relative to the bundle root.
     * <p>
     * This method is backed by the <code>Bundle.getEntryPaths(String)</code>
     * method but returns an <code>Iterator<String></code> instead of an
     * <code>Enumeration</code> of strings.
     *
     * @param parentPath The path to the parent entry whose child entries are to
     *            be returned.
     * @return An <code>Iterator<String></code> providing the paths of
     *         entries considered direct children of the <code>parentPath</code>
     *         or <code>null</code> if the parent entry does not exist.
     */
    Iterator<String> getEntryPaths(String path) {
        List<String> list = listCache.get(path);
        if (list == null) {

            @SuppressWarnings("unchecked")
            Enumeration<String> entries = bundle.getEntryPaths(path);
            if (entries != null && entries.hasMoreElements()) {
                list = new LinkedList<String>();
                while (entries.hasMoreElements()) {
                    list.add(entries.nextElement());
                }
            }

            if (list == null) {
                list = NOT_FOUND_CHILDREN;
            }

            listCache.put(path, list);
        }

        return (list == NOT_FOUND_CHILDREN) ? null : list.iterator();
    }

    // ---------- Management API

    /**
     * Returns the current number of entries stored in the entry cache. This
     * number includes "negative" entries, which are requested entries not found
     * in the bundle.
     */
    int getEntryCacheSize() {
        return cache.size();
    }

    /**
     * Returns the maximum number of entries to be stored in the cache. This
     * number is currently fixed at {@link #CACHE_SIZE}
     */
    int getEntryCacheMaxSize() {
        return CACHE_SIZE;
    }

    /**
     * Returns the current number of list entries stored in the list cache. This
     * number includes "negative" list entries, which are requested list entries
     * not found in the bundle.
     */
    int getListCacheSize() {
        return listCache.size();
    }

    /**
     * Returns the maximum number of list entries to be stored in the cache.
     * This number is currently fixed at {@link #LIST_CACHE_SIZE}
     */
    int getListCacheMaxSize() {
        return LIST_CACHE_SIZE;
    }

    // ---------- inner class

    /**
     * The <code>BundleResourceMap</code> class extends the
     * <code>LinkedHashMap</code> class overwriting the
     * {@link #removeEldestEntry(Entry)} method to implement the size limit,
     * which is set in the constructor.
     */
    private static class BundleResourceMap<K, V> extends
            LinkedHashMap<String, V> {

        private static final long serialVersionUID = 7455098291380945276L;

        /**
         * The default size of a bundle resource cache (value is 20).
         */
        private static final int DEFAULT_LIMIT = 20;

        /**
         * The limit configured for this map.
         */
        private final int limit;

        /**
         * Creates a new instance of this size limited map.
         *
         * @param limit The maximum number of entries in this map. If this value
         *            is less than or equal to zero, the default size of
         *            {@link #DEFAULT_LIMIT} is used.
         */
        BundleResourceMap(int limit) {
            // deliberately chosen initial size and load factor, but
            // we need the access-order to implement the LRU mechanism
            super(8, 0.75f, true);

            // normalize size to a possitive number
            if (limit <= 0) {
                limit = DEFAULT_LIMIT;
            }

            this.limit = limit;
        }

        /**
         * Returns <code>true</code> if the current number of elements in the
         * map exceeds the configured limit.
         */
        @Override
        protected boolean removeEldestEntry(Entry<String, V> eldest) {
            return size() > limit;
        }
    }

}
