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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>BundleResourceIterator</code> class is a resource iterator, which
 * returns resources for each Bundle entry of an underlying enumeration of
 * Bundle entry paths.
 */
class BundleResourceIterator implements Iterator<Resource> {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The bundle resource resolver */
    private final ResourceResolver resourceResolver;

    /** Bundle providing the entry resources */
    private final BundleResourceCache cache;

    private final MappedPath mappedPath;

    /** Underlying bundle entry path enumeration */
    private final Iterator<String> entries;

    /** The length of the parent entry path, see seek() */
    private final int prefixLength;

    /** The prefetched next iterator entry, null at the end of iterating */
    private Resource nextResult;

    private final Map<String, Map<String, Object>> subResources;

    /**
     * Creates an instance using the given parent bundle resource.
     */
    BundleResourceIterator(final BundleResource parent) {

        // trailing slash to enumerate children
        String parentPath = parent.getPath().concat("/");

        this.resourceResolver = parent.getResourceResolver();
        this.cache = parent.getBundle();
        this.subResources = parent.getSubResources();
        this.mappedPath = parent.getMappedPath();

        this.entries = getFilteredEntries(mappedPath.getEntryPath(parentPath));
        this.prefixLength = parentPath.length();

        this.nextResult = (entries != null) ? seek() : null;
    }

    BundleResourceIterator(ResourceResolver resourceResolver, BundleResourceCache bundle,
            MappedPath mappedPath, String parentPath) {

        // trailing slash to enumerate children
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath.concat("/");
        }

        this.resourceResolver = resourceResolver;
        this.cache = bundle;
        this.subResources = null;
        this.mappedPath = mappedPath;
        this.entries = getFilteredEntries(parentPath);
        this.prefixLength = parentPath.length();

        this.nextResult = (entries != null) ? seek() : null;
    }

    private Iterator<String> getFilteredEntries(final String parentPath) {
        final Set<String> bundleEntries = new TreeSet<>(cache.getEntryPaths(parentPath));
        if ( this.mappedPath.getJSONPropertiesExtension() != null ) {
            if ( subResources != null ) {
                for(final String name : subResources.keySet()) {
                    bundleEntries.add(parentPath.concat(name));
                }
            }
            final Set<String> add = new HashSet<>();
            final Iterator<String> iter = bundleEntries.iterator();
            while ( iter.hasNext() ) {
                final String path = iter.next();
                if ( path.endsWith(this.mappedPath.getJSONPropertiesExtension()) ) {
                    iter.remove();
                    add.add(path.substring(0, path.length() - this.mappedPath.getJSONPropertiesExtension().length()));
                }
            }
            bundleEntries.addAll(add);
        }
        return (bundleEntries.isEmpty() ? null : bundleEntries.iterator());
    }

    /** Returns true if there is another Resource available */
    @Override
    public boolean hasNext() {
        return nextResult != null;
    }

    /** Returns the next resource in the iterator */
    @Override
    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Resource result = nextResult;
        nextResult = seek();
        return result;
    }

    /**
     * Throws <code>UnsupportedOperationException</code> as this method is not
     * supported by this implementation.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Scans the entry path enumeration looking for the next entry being a
     * direct child of the parent resource.
     */
    private Resource seek() {
        while (entries.hasNext()) {
            String entry = entries.next();

            // require leading slash
            if (!entry.startsWith("/")) {
                entry = "/".concat(entry);
            }

            int slash = entry.indexOf('/', prefixLength);
            if (slash < 0 || slash == entry.length() - 1) {
                log.debug("seek: Using entry {}", entry);
                final boolean isFolder = entry.endsWith("/");
                String propsPath = null;
                if ( mappedPath.getJSONPropertiesExtension() != null ) {
                    propsPath = entry.concat(mappedPath.getJSONPropertiesExtension());
                }
                final String entryPath = isFolder ? entry.substring(0, entry.length()-1) : entry;
                return new BundleResource(resourceResolver, cache, mappedPath,
                        entryPath,
                        propsPath,
                        this.subResources != null ? this.subResources.get(ResourceUtil.getName(entryPath)) : null,
                        isFolder);
            }

            log.debug("seek: Ignoring entry {}", entry);
        }

        // no more results
        log.debug("seek: No more nodes, iterator exhausted");
        return null;
    }
}
