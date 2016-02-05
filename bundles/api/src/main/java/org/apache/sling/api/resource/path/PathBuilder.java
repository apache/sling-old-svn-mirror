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

/**
 * The <tt>PathBuilder</tt> offers a convenient way of creating a valid path from multiple fragments
 *
 */
public final class PathBuilder {
    
    private StringBuilder sb = new StringBuilder();
    
    /**
     * Creates a new <tt>PathBuilder</tt> instance
     * 
     * @param path the initial path
     */
    public PathBuilder(String path) {
        
        if ( path == null || path.isEmpty() || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path '" + path + "' is not absolute");
        }
        
        sb.append(path);
    }
    
    /**
     * Appends a new path fragment
     * 
     * @param path the path fragment to append
     * @return this instance
     */
    public PathBuilder append(String path) {
        
        if ( path == null || path.isEmpty() ) {
            throw new IllegalArgumentException("Path '" + path + "' is null or empty");
        }
        
        boolean trailingSlash = sb.charAt(sb.length() - 1) == '/';
        boolean leadingSlash = path.charAt(0) == '/';
        
        if ( trailingSlash && leadingSlash) {
            sb.append(path.substring(1));
        } else if ( !trailingSlash && !leadingSlash ) {
            sb.append('/').append(path);
        } else {
            sb.append(path);
        }
        
        return this;
    }
    
    /**
     * Returns the path
     * 
     * @return the path
     */
    public String toString() {
        return sb.toString();
    }
}