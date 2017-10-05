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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.commons.osgi.ManifestHeader;

class PathMapping {

    public static final String DIR_PATH = "path";
    public static final String DIR_JSON = "propsJSON";

    private static final char prefixSeparatorChar = '!';
    private final String resourceRoot;
    private final String resourceRootPrefix;
    private final String entryRoot;
    private final String entryRootPrefix;

    private final String jsonExpandExtension;

    public static PathMapping[] getRoots(final String rootList) {
        List<PathMapping> prefixList = new ArrayList<>();

        final ManifestHeader header = ManifestHeader.parse(rootList);
        for (final ManifestHeader.Entry entry : header.getEntries()) {
            final String resourceRoot = entry.getValue();
            final String pathDirective = entry.getDirectiveValue(DIR_PATH);
            final String expandDirective = entry.getDirectiveValue(DIR_JSON);
            if (pathDirective != null) {
                prefixList.add(new PathMapping(resourceRoot, pathDirective, expandDirective));
            } else {
                prefixList.add(PathMapping.create(resourceRoot, expandDirective));
            }
        }
        return prefixList.toArray(new PathMapping[prefixList.size()]);
    }


    static PathMapping create(final String configPath,
            final String expandDirective) {
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
        return new PathMapping(resourceRoot, entryRoot, expandDirective);
    }

    PathMapping(final String resourceRoot,
            final String entryRoot,
            final String expandDirective) {
        this.resourceRoot = ensureNoTrailingSlash(resourceRoot);
        this.resourceRootPrefix = ensureTrailingSlash(resourceRoot);
        this.entryRoot = ensureLeadingSlash(ensureNoTrailingSlash(entryRoot));
        this.entryRootPrefix = ensureLeadingSlash(ensureTrailingSlash(entryRoot));
        this.jsonExpandExtension = ensureLeadingDot(expandDirective);
    }

    String getJSONPropertiesExtension() {
        return this.jsonExpandExtension;
    }

    boolean isChild(final String resourcePath) {
        return resourcePath.startsWith(resourceRootPrefix)
            || resourcePath.equals(resourceRoot);
    }

    String getEntryPath(final String resourcePath) {
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

    String getResourcePath(final String entryPath) {
        if ( entryRootPrefix == null ) {
            return entryPath;
        }
        if ( entryPath.startsWith(entryRootPrefix) ) {
            return resourceRootPrefix.concat(entryPath.substring(entryRootPrefix.length()));
        } else if ( entryPath.equals(entryRoot)) {
            return resourceRoot;
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

    private static String ensureLeadingDot(final String path) {
        if (path == null || path.length() == 0) {
            return null;
        }

        if (!path.startsWith(".")) {
            return ".".concat(path);
        }

        return path;
    }

    private static String ensureNoTrailingSlash(final String path) {
        if (path == null || path.length() == 0) {
            return null;
        }

        if (path.endsWith("/")) {
            return ensureNoTrailingSlash(path.substring(0, path.length() - 1));
        }

        return path;
    }

    private static String ensureTrailingSlash(final String path) {
        if (path == null || path.length() == 0) {
            return null;
        }

        if (!path.endsWith("/")) {
            return path.concat("/");
        }

        return path;
    }

    private static String ensureLeadingSlash(final String path) {
        if (path == null || path.length() == 0) {
            return null;
        }

        if (!path.startsWith("/")) {
            return "/".concat(path);
        }

        return path;
    }

    @Override
    public String toString() {
        return "MappedPath: " + getResourceRoot() + " -> " + getEntryRoot();
    }
}
