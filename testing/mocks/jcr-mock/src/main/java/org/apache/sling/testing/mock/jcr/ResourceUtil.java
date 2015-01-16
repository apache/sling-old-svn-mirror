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
package org.apache.sling.testing.mock.jcr;

/**
 * This is a stripped-down copy of org.apache.sling.api.resource.ResourceUtil
 * with some methods required by the jcr-mock implementation internally.
 */
class ResourceUtil {

    /**
     * Resolves relative path segments '.' and '..' in the absolute path.
     * Returns null if not possible (.. points above root) or if path is not
     * absolute.
     */
    public static String normalize(String path) {

        // don't care for empty paths
        if (path.length() == 0) {
            return path;
        }

        // prepare the path buffer with trailing slash (simplifies impl)
        int absOffset = (path.charAt(0) == '/') ? 0 : 1;
        char[] buf = new char[path.length() + 1 + absOffset];
        if (absOffset == 1) {
            buf[0] = '/';
        }
        path.getChars(0, path.length(), buf, absOffset);
        buf[buf.length - 1] = '/';

        int lastSlash = 0; // last slash in path
        int numDots = 0; // number of consecutive dots after last slash

        int bufPos = 0;
        for (int bufIdx = lastSlash; bufIdx < buf.length; bufIdx++) {
            char c = buf[bufIdx];
            if (c == '/') {
                if (numDots == 2) {
                    if (bufPos == 0) {
                        return null;
                    }

                    do {
                        bufPos--;
                    } while (bufPos > 0 && buf[bufPos] != '/');
                }

                lastSlash = bufIdx;
                numDots = 0;
            } else if (c == '.' && numDots < 2) {
                numDots++;
            } else {
                // find the next slash
                int nextSlash = bufIdx + 1;
                while (nextSlash < buf.length && buf[nextSlash] != '/') {
                    nextSlash++;
                }

                // append up to the next slash (or end of path)
                if (bufPos < lastSlash) {
                    int segLen = nextSlash - bufIdx + 1;
                    System.arraycopy(buf, lastSlash, buf, bufPos, segLen);
                    bufPos += segLen;
                } else {
                    bufPos = nextSlash;
                }

                numDots = 0;
                lastSlash = nextSlash;
                bufIdx = nextSlash;
            }
        }

        String resolved;
        if (bufPos == 0 && numDots == 0) {
            resolved = (absOffset == 0) ? "/" : "";
        } else if ((bufPos - absOffset) == path.length()) {
            resolved = path;
        } else {
            resolved = new String(buf, absOffset, bufPos - absOffset);
        }

        return resolved;
    }

    /**
     * Utility method returns the parent path of the given <code>path</code>,
     * which is normalized by {@link #normalize(String)} before resolving the
     * parent.
     *
     * @param path The path whose parent is to be returned.
     * @return <code>null</code> if <code>path</code> is the root path (
     *         <code>/</code>) or if <code>path</code> is a single name
     *         containing no slash (<code>/</code>) characters.
     * @throws IllegalArgumentException If the path cannot be normalized by the
     *             {@link #normalize(String)} method.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    public static String getParent(String path) {
        if ("/".equals(path)) {
            return null;
        }

        // normalize path (remove . and ..)
        path = normalize(path);

        // if normalized to root, there is no parent
        if (path == null || "/".equals(path)) {
            return null;
        }

        String workspaceName = null;

        final int wsSepPos = path.indexOf(":/");
        if (wsSepPos != -1) {
            workspaceName = path.substring(0, wsSepPos);
            path = path.substring(wsSepPos + 1);
        }

        // find the last slash, after which to cut off
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            // no slash in the path
            return null;
        } else if (lastSlash == 0) {
            // parent is root
            if (workspaceName != null) {
                return workspaceName + ":/";
            }
            return "/";
        }

        String parentPath = path.substring(0, lastSlash);
        if (workspaceName != null) {
            return workspaceName + ":" + parentPath;
        }
        return parentPath;
    }

    /**
     * Utility method returns the ancestor's path at the given <code>level</code>
     * relative to <code>path</code>, which is normalized by {@link #normalize(String)}
     * before resolving the ancestor.
     *
     * <ul>
     * <li><code>level</code> = 0 returns the <code>path</code>.</li>
     * <li><code>level</code> = 1 returns the parent of <code>path</code>, if it exists, <code>null</code> otherwise.</li>
     * <li><code>level</code> = 2 returns the grandparent of <code>path</code>, if it exists, <code>null</code> otherwise.</li>
     * </ul>
     *
     * @param path The path whose ancestor is to be returned.
     * @param level The relative level of the ancestor, relative to <code>path</code>.
     * @return <code>null</code> if <code>path</code> doesn't have an ancestor at the
     *            specified <code>level</code>.
     * @throws IllegalArgumentException If the path cannot be normalized by the
     *             {@link #normalize(String)} method or if <code>level</code> < 0.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     * @since 2.2
     */
    public static String getParent(final String path, final int level) {
        if ( level < 0 ) {
            throw new IllegalArgumentException("level must be non-negative");
        }
        String result = path;
        for(int i=0; i<level; i++) {
            result = getParent(result);
            if ( result == null ) {
                break;
            }
        }
        return result;
    }

    /**
     * Utility method returns the name of the given <code>path</code>, which is
     * normalized by {@link #normalize(String)} before resolving the name.
     *
     * @param path The path whose name (the last path element) is to be
     *            returned.
     * @return The empty string if <code>path</code> is the root path (
     *         <code>/</code>) or if <code>path</code> is a single name
     *         containing no slash (<code>/</code>) characters.
     * @throws IllegalArgumentException If the path cannot be normalized by the
     *             {@link #normalize(String)} method.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    public static String getName(String path) {
        if ("/".equals(path)) {
            return "";
        }

        // normalize path (remove . and ..)
        path = normalize(path);
        if ("/".equals(path)) {
            return "";
        }

        // find the last slash
        return path.substring(path.lastIndexOf('/') + 1);
    }

}
