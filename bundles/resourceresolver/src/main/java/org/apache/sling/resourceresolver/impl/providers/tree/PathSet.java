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
package org.apache.sling.resourceresolver.impl.providers.tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Simple helper class for path matching against a set of paths.
 */
public class PathSet implements Iterable<Path> {

    private final Set<Path> paths;

    public PathSet(final Set<String> paths) {
        this.paths = new HashSet<Path>();
        for(final String p : paths) {
            this.paths.add(new Path(p));
        }
    }

    public boolean matches(final String otherPath) {
         for(final Path p : this.paths) {
             if ( p.matches(otherPath) ) {
                 return true;
             }
         }
         return false;
    }

    /**
     * Generate an unmodifiable set of exclude paths
     * @param path The base path
     * @return Set of exclude paths
     */
    public Set<String> getExcludes(final String path) {
        final Path pathObj = new Path(path);
        final Set<String> result = new HashSet<String>();
        for(final Path p : this.paths) {
            if ( pathObj.matches(p.getPath()) ) {
                result.add(p.getPath());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Iterator<Path> iterator() {
        return this.paths.iterator();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + paths.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathSet other = (PathSet) obj;
        if (!paths.equals(other.paths))
            return false;
        return true;
    }
}