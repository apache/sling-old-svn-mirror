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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
    private final BundleResourceCache bundle;

    private final MappedPath mappedPath;

    /** Underlying bundle entry path enumeration */
    private final Iterator<String> entries;

    /** The length of the parent entry path, see seek() */
    private final int prefixLength;

    /** The prefetched next iterator entry, null at the end of iterating */
    private Resource nextResult;

    /**
     * Creates an instance using the given parent bundle resource.
     */
    BundleResourceIterator(BundleResource parent) {

        if (parent.isFile()) {

            // if the parent is a file, the iterator is empty
            this.resourceResolver = null;
            this.bundle = null;
            this.entries = null;
            this.prefixLength = 0;
            this.mappedPath = null;
            this.nextResult = null;

        } else {
            // trailing slash to enumerate children
            String parentPath = parent.getPath().concat("/");

            this.resourceResolver = parent.getResourceResolver();
            this.bundle = parent.getBundle();
            this.mappedPath = parent.getMappedPath();
            
            parentPath = mappedPath.getEntryPath(parentPath);
            
            this.entries = parent.getBundle().getEntryPaths(parentPath);
            this.prefixLength = parentPath.length();

            this.nextResult = (entries != null) ? seek() : null;
        }
    }

    BundleResourceIterator(ResourceResolver resourceResolver, BundleResourceCache bundle,
            MappedPath mappedPath, String parentPath) {

        // trailing slash to enumerate children
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath.concat("/");
        }

        this.resourceResolver = resourceResolver;
        this.bundle = bundle;
        this.mappedPath = mappedPath;
        this.entries = bundle.getEntryPaths(parentPath);
        this.prefixLength = parentPath.length();

        this.nextResult = (entries != null) ? seek() : null;
    }

    /** Returns true if there is another Resource available */
    public boolean hasNext() {
        return nextResult != null;
    }

    /** Returns the next resource in the iterator */
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
                entry = "/" + entry;
            }

            int slash = entry.indexOf('/', prefixLength);
            if (slash < 0 || slash == entry.length() - 1) {
                log.debug("seek: Using entry {}", entry);
                return new BundleResource(resourceResolver, bundle, mappedPath,
                    entry);
            }

            log.debug("seek: Ignoring entry {}", entry);
        }

        // no more results
        log.debug("seek: No more nodes, iterator exhausted");
        return null;
    }
}
