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

    private static final String GLOB_PREFIX = "glob:";

    private final String path;
    private final String prefix;
    private final boolean isPattern;
    private final Pattern regexPattern;

    /**
     * <p>Create a new path object either from a concrete path or from a glob pattern.</p>
     *
     * <p>A glob pattern must start with the {@code glob:} prefix (e.g. <code>glob:**&#47;*.html</code>). The following rules are used
     * to interpret glob patterns:</p>
     * <ul>
     *     <li>The {@code *} character matches zero or more characters of a name component without crossing directory boundaries.</li>
     *     <li>The {@code **} characters match zero or more characters crossing directory boundaries.</li>
     * </ul>
     *
     * @param path The resource path or a glob pattern.
     * @throws NullPointerException If {@code otherPath} is {@code null}
     * @throws IllegalArgumentException If the provided path is not absolute, or if the glob pattern does not start with a slash.
     */
    public Path(@Nonnull final String path) {
        if ( path.equals("/") ) {
            this.path = "/";
        } else if ( path.endsWith("/") ) {
            this.path = path.substring(0, path.length() - 1);
        } else {
            this.path = path;
        }
        if (this.path.startsWith(GLOB_PREFIX)) {
            final String patternPath = path.substring(GLOB_PREFIX.length());
            this.isPattern = true;
            this.regexPattern = Pattern.compile(toRegexPattern(patternPath));
            int lastSlash = 0;
            int pos = 1;
            while ( patternPath.length() > pos ) {
                final char c = patternPath.charAt(pos);
                if ( c == '/') {
                    lastSlash = pos;
                } else if ( c == '*') {
                    break;
                }
                pos++;
            }

            this.prefix = (pos == patternPath.length() ? patternPath : patternPath.substring(0, lastSlash + 1));
        } else {
            this.isPattern = false;
            this.regexPattern = null;
            this.prefix = this.path.equals("/") ? "/" : this.path.concat("/");
        }
        if ( !this.prefix.startsWith("/") ) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }
    }

    /**
     * If this {@code Path} object holds a path (and not a pattern), this method
     * checks whether the provided path is equal to this path or a sub path of it.
     * If a glob pattern is provided as the argument, it performs the same check
     * and respects the provided pattern.
     * If this {@code Path} object holds a pattern, it checks whether the
     * provided path matches the pattern. If this path object holds a pattern
     * and a pattern is provided as the argument, it returns only {@code true}
     * if the pattern is the same.
     * If the provided argument is not an absolute path (e.g. if it is a relative
     * path or a pattern), this method returns {@code false}.
     *
     * @param otherPath Absolute path to check.
     * @return {@code true} If other path is within the sub tree of this path
     *         or matches the pattern.
     * @see Path#isPattern()
     * @throws NullPointerException If {@code otherPath} is {@code null}
     * @throws IllegalArgumentException If the provided path is not absolute, or if the glob pattern does not start with a slash.
     */
    public boolean matches(final String otherPath) {
        if ( otherPath.startsWith(GLOB_PREFIX) ) {
            if ( this.isPattern ) {
                // both are patterns, then they must be equal
                final Path oPath = new Path(otherPath);
                return this.regexPattern.equals(oPath.regexPattern);
            }

            // this is path, provided argument is a pattern
            // simplest case - the prefix of the glob pattern matches already
            // for example: this path = /apps
            //              glob      = /apps/**
            final Path oPath = new Path(otherPath);
            if ( this.matches(oPath.prefix) ) {
                return true;
            }
            // count slashes in path
            int count = 0;
            for (int i=0; i < this.path.length(); i++) {
                if (this.path.charAt(i) == '/') {
                     count++;
                }
            }
            // now create the substring of the glob pattern with the same amount of slashes
            int start = GLOB_PREFIX.length();
            while ( start < otherPath.length() ) {
                if ( otherPath.charAt(start) == '/') {
                    if ( count == 0 ) {
                        break;
                    }
                    count--;
                }
                start++;
            }
            if ( count > 0 ) {
                return false;
            }
            final String globPattern = otherPath.substring(0, start);
            final Path globPatternPath = new Path(globPattern);
            return globPatternPath.matches(this.path);
        }

        // provided argument is a path
        if ( !otherPath.startsWith("/") ) {
            throw new IllegalArgumentException("Path must be absolute: " + otherPath);
        }
        if (isPattern) {
            return this.regexPattern.matcher(otherPath).matches();
        }
        return this.path.equals(otherPath) || otherPath.startsWith(this.prefix);
    }

    /**
     * Return the path if this {@code Path} object holds a path,
     * returns the pattern otherwise.
     * @return The path or pattern.
     * @see #isPattern()
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Returns {code true} if this {@code Path} object is holding a pattern
     * @return {code true} for a pattern, {@code false} for a path.
     * @since 1.2.0 (Sling API Bundle 2.15.0)
     */
    public boolean isPattern() {
        return this.isPattern;
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
                        stringBuilder.append(Pattern.quote(Character.toString(currentChar)));
                    } else {
                        stringBuilder.append(currentChar);
                    }
            }
        }
        return stringBuilder.append('$').toString();
    }

    private static char getCharAtIndex(String string, int index) {
        return index < string.length() ? string.charAt(index) : 0;
    }

    private static boolean isRegexMeta(char character) {
        return "<([{\\^-=$!|]})?*+.>".indexOf(character) != -1;
    }

}
