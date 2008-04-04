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

class MappedPath {

    private static final char prefixSeparatorChar = '!';
    private final String resourceRoot;
    private final String resourceRootPrefix;
    private final String entryRoot;
    private final String entryRootPrefix;

    static MappedPath create(String configPath) {
        String resourceRoot;
        String entryRoot;
        int prefixSep = configPath.indexOf(prefixSeparatorChar);
        if (prefixSep >= 0) {
            entryRoot = configPath.substring(prefixSep + 1);
            resourceRoot = configPath.substring(0, prefixSep).concat(entryRoot);
        } else {
            resourceRoot = configPath;
            entryRoot = null;
        }
        return new MappedPath(resourceRoot, entryRoot);
    }
    
    MappedPath(String resourceRoot, String entryRoot) {
        this.resourceRoot = resourceRoot;
        this.resourceRootPrefix = ensureTrailingSlash(resourceRoot);
        this.entryRoot = entryRoot;
        this.entryRootPrefix = ensureTrailingSlash(entryRoot);
    }
    
    boolean isChild(String resourcePath) {
        return resourcePath.startsWith(resourceRootPrefix)
            || resourcePath.equals(resourceRoot);
    }
    
    String getEntryPath(String resourcePath) {
        if (entryRootPrefix == null) {
            return resourcePath;
        }
        
        if (resourcePath.startsWith(resourceRootPrefix)) {
            return entryRootPrefix.concat(resourcePath.substring(resourceRootPrefix.length()));
        } else if (resourcePath.equals(resourceRoot)) {
            return entryRoot;
        }
        
        return null;
    }
    
    String getResourceRoot() {
        return resourceRoot;
    }
    
    String getResourceRootPrefix() {
        return resourceRootPrefix;
    }
    
    String getEntryRoot() {
        return entryRoot;
    }
    
    String getEntryRootPrefix() {
        return entryRootPrefix;
    }
    
    private static String ensureTrailingSlash(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        
        if (!path.endsWith("/")) {
            return path.concat("/");
        }
        
        return path;
    }
    
    @Override
    public String toString() {
        return "MappedPath: " + getResourceRoot() + " -> " + getEntryRoot();
    }
}
