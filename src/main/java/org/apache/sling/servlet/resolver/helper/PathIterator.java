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
package org.apache.sling.servlet.resolver.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.jcr.resource.JcrResourceUtil;

public class PathIterator implements Iterator<String> {

    private static final String[] EMPTY_PATH = { "" };

    private final String[] initPath;

    private final String selectorsPath;

    private String resourceTypePath;

    private String[] path;

    private int pathIdx;

    private int minPathLength;

    private String nextPath;

    public PathIterator(String resourceType, String selectorString,
            String[] path) {

        selectorsPath = (selectorString != null) ? "/"
            + selectorString.replace('.', '/') : null;
        initPath = path;

        reset(resourceType);
    }

    public PathIterator(String relPath, String[] path) {
        this(relPath, null, path);
    }

    public void reset(String resourceType) {
        resourceTypePath = JcrResourceUtil.resourceTypeToPath(resourceType);
        path = resourceTypePath.startsWith("/") ? EMPTY_PATH : initPath;

        reset();
    }

    public void reset() {
        minPathLength = 0;
        pathIdx = -1;
        nextPath = "";
        seek();
    }

    public boolean hasNext() {
        return nextPath != null;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String result = nextPath;
        seek();
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    private void seek() {

        int lastSlash = nextPath.lastIndexOf('/');
        if (lastSlash >= minPathLength) {

            nextPath = nextPath.substring(0, lastSlash);

        } else {

            pathIdx++;
            if (pathIdx < path.length) {
                final StringBuffer sb = new StringBuffer();

                // the default path root
                sb.append(path[pathIdx]);

                // append the resource type derived path
                sb.append(resourceTypePath);

                minPathLength = sb.length();

                // add selectors in front of the filename if any, replacing
                // dots
                // in them
                // by slashes so that print.a4 becomes print/a4/
                if (selectorsPath != null) {
                    sb.append(selectorsPath);
                }

                nextPath = sb.toString();
            } else {
                nextPath = null;
            }
        }
    }
}