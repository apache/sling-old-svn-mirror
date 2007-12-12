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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterate over the the HTTP request path by creating shorter segments of that
 * path using "." and "/" as separators.
 * <p>
 * For example, if path = /some/stuff.a4.html/xyz the sequence is:
 * <ol>
 * <li> /some/stuff.a4.html/xyz </li>
 * <li> /some/stuff.a4.html </li>
 * <li> /some/stuff.a4 </li>
 * <li> /some/stuff </li>
 * <li> /some </li>
 * </ol>
 * <p>
 * The root path (/) is never returned. Creating a resource path iterator with a
 * null or empty path or a root path will not return anything.
 */
public class ResourcePathIterator implements Iterator<String> {

    private String nextPath;

    public ResourcePathIterator(String path) {
        if (path != null) {
            int i = path.length() - 1;
            while (i >= 0 && path.charAt(i) == '/') {
                i--;
            }
            if (i < 0) {
                nextPath = null;
            } else if (i < path.length() - 1) {
                nextPath = path.substring(0, i + 1);
            } else {
                nextPath = path;
            }
        } else {
            nextPath = null;
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
        int pos = result.length() - 1;
        while (pos >= 0) {
            final char c = result.charAt(pos);
            if (c == '.' || c == '/') {
                break;
            }
            pos--;
        }

        nextPath = (pos <= 0) ? null : result.substring(0, pos);

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException(
            "Cannot remove() on ResourcePathIterator");
    }

}
