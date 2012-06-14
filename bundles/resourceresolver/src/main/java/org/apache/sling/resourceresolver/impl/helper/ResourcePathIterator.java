/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resourceresolver.impl.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterate over the the HTTP request path by creating shorter segments of that
 * path using "." as a separator.
 * <p>
 * For example, if path = /some/stuff.a4.html/xyz.ext the sequence is:
 * <ol>
 * <li> /some/stuff.a4.html/xyz.ext </li>
 * <li> /some/stuff.a4.html/xyz </li>
 * <li> /some/stuff.a4</li>
 * <li> /some/stuff </li>
 * </ol>
 * <p>
 * The root path (/) is never returned.
 */
public class ResourcePathIterator implements Iterator<String> {

    // the next path to return, null if nothing more to return
    private String nextPath;

    /**
     * Creates a new instance iterating over the given path
     *
     * @param path The path to iterate over. If this is empty or
     *            <code>null</code> this iterator will not return anything.
     */
    public ResourcePathIterator(String path) {

        if (path == null || path.length() == 0) {

            // null or empty path, there is nothing to return
            nextPath = null;

        } else {

            // find last non-slash character
            int i = path.length() - 1;
            while (i >= 0 && path.charAt(i) == '/') {
                i--;
            }

            if (i < 0) {
                // only slashes, assume root node
                nextPath = "/";

            } else if (i < path.length() - 1) {
                // cut off slash
                nextPath = path.substring(0, i + 1);

            } else {
                // no trailing slash
                nextPath = path;
            }
        }
    }

    public boolean hasNext() {
        return nextPath != null;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final String result = nextPath;

        // find next path
        int lastDot = nextPath.lastIndexOf('.');
        nextPath = (lastDot > 0) ? nextPath.substring(0, lastDot) : null;

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
