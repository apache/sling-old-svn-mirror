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
package org.apache.sling.api.resource.util;

/**
 * Simple helper class for path matching.
 *
 * @since 1.0.0 (Sling API Bundle 2.10.0)
 */
public class Path implements Comparable<Path> {

    private final String path;

    private final String prefix;

    /**
     * Create a new path object.
     * @param path The resource path.
     */
    public Path(final String path) {
        this.path = path;
        this.prefix = path.equals("/") ? "/" : path.concat("/");
    }

    /**
     * Check whether the provided path is equal to this path or a sub path
     * of it.
     * @param otherPath Path to check
     * @return {@code true} If other path is within the sub tree of this path.
     */
    public boolean matches(final String otherPath) {
        if ( this.path.equals(otherPath) || otherPath.startsWith(this.prefix) ) {
            return true;
        }
        return false;
    }

    /**
     * Return the path.
     * @return The path.
     */
    public String getPath() {
        return this.path;
    }

    @Override
    public int compareTo(final Path o) {
        return this.getPath().compareTo(o.getPath());
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Path)) {
            return false;
        }
        return this.getPath().equals(((Path)obj).getPath());
    }

}