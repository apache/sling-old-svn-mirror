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

import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Simple helper class for path matching.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
public class Path implements Comparable<Path> {

    private final String path;

    private final String prefix;

    private final boolean isPattern;
    private final Pattern regexPattern;

    /**
     * <p>Create a new path object either from a concrete path or from a glob pattern.</p>
     *
     * <p>A glob pattern should start with the {@code glob:} prefix (e.g. <code>glob:**&#47;*.html</code>). The following rules are used
     * to interpret glob patterns:</p>
     * <ul>
     *     <li>The {@code *} character matches zero or more characters of a name component without crossing directory boundaries.</li>
     *     <li>The {@code **} characters match zero or more characters crossing directory boundaries.</li>
     * </ul>
     *
     * @param path the resource path or a glob pattern.
     */
    public Path(@Nonnull final String path) {
        this.path = path;
        this.prefix = path.equals("/") ? "/" : path.concat("/");
        if (path.startsWith("glob:")) {
            isPattern = true;
            regexPattern = Pattern.compile(toRegexPattern(path.substring(5)));
        } else {
            isPattern = false;
            regexPattern = null;
        }

    }

    /**
     * Check whether the provided path is equal to this path or a sub path of it.
     * @param otherPath Path to check
     * @return {@code true} If other path is within the sub tree of this path.
     */
    public boolean matches(final String otherPath) {
        if (isPattern) {
            return regexPattern.matcher(otherPath).matches();
        }
        return this.path.equals(otherPath) || otherPath.startsWith(this.prefix);
    }

    /**
     * Return the path.
     * @return The path.
     */
    public String getPath() {
        return this.path;
    }

    @Override
    public int compareTo(@Nonnull final Path o) {
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

    private static String toRegexPattern(String pattern) {
        StringBuilder stringBuilder = new StringBuilder("^");
        int index = 0;
        while (index < pattern.length()) {
            char currentChar = pattern.charAt(index++);
            switch (currentChar) {
                case '*':
                    if (getCharAtIndex(pattern, index) == '*') {
                        stringBuilder.append(".*");
                        ++index;
                    } else {
                        stringBuilder.append("[^/]*");
                    }
                    break;
                case '/':
                    stringBuilder.append(currentChar);
                    break;
                default:
                    if (isRegexMeta(currentChar)) {
                        stringBuilder.append('\\');
                    }

                    stringBuilder.append(currentChar);
            }
        }
        return stringBuilder.append('$').toString();
    }

    private static char getCharAtIndex(String string, int index) {
        return index < string.length() ? string.charAt(index) : 0;
    }

    private static boolean isRegexMeta(char character) {
        return ".^$+{[]|()".indexOf(character) != -1;
    }

}
