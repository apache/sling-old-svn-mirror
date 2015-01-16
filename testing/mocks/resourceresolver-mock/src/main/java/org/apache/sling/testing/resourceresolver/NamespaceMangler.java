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
package org.apache.sling.testing.resourceresolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NamespaceMangler {

    private static final String MANGLED_NAMESPACE_PREFIX = "_";
    private static final String MANGLED_NAMESPACE_SUFFIX = "_";
    private static final char NAMESPACE_SEPARATOR = ':';
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("/([^:/]+):");
    private static final Pattern MANGLED_NAMESPACE_PATTERN = Pattern.compile("/_([^_/]+)_");

    private NamespaceMangler() {
        // static methods only
    }

    /**
     * Mangle the namespaces in the given path for usage in sling-based URLs.
     * <p>
     * Example: /path/jcr:content to /path/_jcr_content
     * </p>
     * @param path Path to mangle
     * @return Mangled path
     */
    public static String mangleNamespaces(String path) {
        if (path == null) {
            return null;
        }
        Matcher matcher = NAMESPACE_PATTERN.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = "/" + MANGLED_NAMESPACE_PREFIX + matcher.group(1) + MANGLED_NAMESPACE_SUFFIX;
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Unmangle the namespaces in the given path for usage in sling-based URLs.
     * <p>
     * Example: /path/_jcr_content to /path/jcr:content
     * </p>
     * @param path Path to unmangle
     * @return Unmangled path
     */
    public static String unmangleNamespaces(String path) {
        if (path == null) {
            return null;
        }
        Matcher matcher = MANGLED_NAMESPACE_PATTERN.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String replacement = "/" + matcher.group(1) + NAMESPACE_SEPARATOR;
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
