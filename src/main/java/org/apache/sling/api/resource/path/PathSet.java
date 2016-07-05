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
package org.apache.sling.api.resource.path;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Simple helper class for path matching against a set of paths.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
public class PathSet implements Iterable<Path> {

    /** Empty path set. */
    public static final PathSet EMPTY_SET = new PathSet(Collections.<Path> emptySet());

    /**
     * Create a path set from a collection of path objects
     * @param paths The collection of path objects
     * @return The path set
     */
    public static PathSet fromPathCollection(final Collection<Path> paths) {
        final Set<Path> set = new HashSet<Path>();
        for(final Path p : paths) {
            set.add(p);
        }
        optimize(set);
        return new PathSet(set);
    }

    /**
     * Create a path set from a collection of path objects
     * @param paths The collection of path objects
     * @return The path set
     */
    public static PathSet fromPaths(final Path...paths) {
        final Set<Path> set = new HashSet<Path>();
        for(final Path p : paths) {
            set.add(p);
        }
        optimize(set);
        return new PathSet(set);
    }

    /**
     * Create a path set from a collection of strings
     * @param paths The collection of strings
     * @return The path set
     */
    public static PathSet fromStringCollection(final Collection<String> paths) {
        final Set<Path> set = new HashSet<Path>();
        for(final String p : paths) {
            set.add(new Path(p));
        }
        optimize(set);
        return new PathSet(set);
    }

    /**
     * Create a path set from a collection of strings
     * @param strings The array of strings
     * @return The path set
     */
    public static PathSet fromStrings(final String...strings) {
        final Set<Path> set = new HashSet<Path>();
        for(final String p : strings) {
            set.add(new Path(p));
        }
        optimize(set);
        return new PathSet(set);
    }

    /**
     * Optimize the set by filtering out paths which are a sub path
     * of another path in the set.
     * @param set The path set
     */
    private static void optimize(final Set<Path> set) {
        final Iterator<Path> i = set.iterator();
        while ( i.hasNext() ) {
            final Path next = i.next();
            boolean found = false;
            for(final Path p : set) {
                if ( p != next && p.matches(next.getPath()) ) {
                    found = true;
                    break;
                }
            }
            if ( found ) {
                i.remove();
            }
        }
    }

    private final Set<Path> paths;

    /**
     * Create a path set from a set of paths
     * @param paths A set of paths
     */
    private PathSet(final Set<Path> paths) {
        this.paths = paths;
    }

    /**
     * Check whether the provided path is in the sub tree of any
     * of the paths in this set.
     * @param otherPath
     * @return The path which matches the provided path, {@code null} otherwise.
     * @see Path#matches(String)
     */
    public Path matches(final String otherPath) {
         for(final Path p : this.paths) {
             if ( p.matches(otherPath) ) {
                 return p;
             }
         }
         return null;
    }

    /**
     * Generate a path set of paths from this set which
     * are in the sub tree of the provided path
     * @param path The base path
     * @return Path set
     */
    public PathSet getSubset(final String path) {
        return getSubset(new Path(path));
    }

    /**
     * Generate a path set of paths from this set which
     * are in the sub tree of the provided path
     * @param path The base path
     * @return Path set
     */
    public PathSet getSubset(final Path path) {
        final Set<Path> result = new HashSet<Path>();
        for(final Path p : this.paths) {
            if ( path.matches(p.getPath()) ) {
                result.add(p);
            }
        }
        return new PathSet(result);
    }

    /**
     * Create a unmodifiable set of strings
     */
    public Set<String> toStringSet() {
        final Set<String> set = new HashSet<String>();
        for(final Path p : this) {
            set.add(p.getPath());
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Return an unmodifiable iterator for the paths.
     */
    @Override
    public Iterator<Path> iterator() {
        return Collections.unmodifiableSet(this.paths).iterator();
    }

    @Override
    public int hashCode() {
        return paths.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof PathSet)) {
            return false;
        }
        return this.paths.equals(((PathSet)obj).paths);
    }
}