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
import java.util.Map.Entry;

import org.osgi.framework.Bundle;

public class BundleResourceCache {

    private static final URL NOT_FOUND_URL;

    private static final List<String> NOT_FOUND_CHILDREN = Collections.<String> emptyList();

    private final BundleResourceMap<String, URL> cache;

    private final BundleResourceMap<String, List<String>> listCache;

    private final Bundle bundle;

    static {
        try {
            NOT_FOUND_URL = new URL("file:///not_found");
        } catch (MalformedURLException mue) {
            throw new ExceptionInInitializerError(mue);
        }
    }

    BundleResourceCache(Bundle bundle) {
        this.bundle = bundle;
        this.cache = new BundleResourceMap<String, URL>(50);
        this.listCache = new BundleResourceMap<String, List<String>>(20);
    }

    Bundle getBundle() {
        return bundle;
    }
    
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
    
    private static class BundleResourceMap<K, V> extends LinkedHashMap<String, V> {

        /**
         * The default size of a bundle resource cache.
         */
        public static final int DEFAULT_LIMIT = 100;
        
        private final int limit;
        
        BundleResourceMap(int limit) {
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
