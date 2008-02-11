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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HttpConstants;

public class PathSupport {

    public static String toPath(String resourceType) {
        return resourceType.replaceAll("\\:", "/");
    }

    public static String getScriptBaseName(SlingHttpServletRequest request) {
        String methodName = request.getMethod();
        String extension = request.getRequestPathInfo().getExtension();

        if (methodName == null || methodName.length() == 0) {

            throw new IllegalArgumentException(
                "HTTP Method name must not be empty");

        } else if ((HttpConstants.METHOD_GET.equalsIgnoreCase(methodName)
            || HttpConstants.METHOD_HEAD.equalsIgnoreCase(methodName))
            && extension != null && extension.length() > 0) {

            // for GET, we use the request extension
            return extension;

        } else {

            // for other methods use the method name
            return methodName;
        }
    }

    public static Iterator<String> getPathIterator(
            SlingHttpServletRequest request, String[] path) {
        return new ScriptPathIterator(request, path);
    }

    public static Iterator<String> getPathIterator(String relPath, String[] path) {
        return new ScriptPathIterator(relPath, path);
    }

    private static class ScriptPathIterator implements Iterator<String> {

        private static final String[] EMPTY_PATH = { "" };

        private final String resourceTypePath;

        private final String selectorsPath;

        private String[] path;

        private int pathIdx;

        private int minPathLength;

        private String nextPath;

        private ScriptPathIterator(SlingHttpServletRequest request,
                String[] path) {

            this.resourceTypePath = toPath(request.getResource().getResourceType());

            String selectors = request.getRequestPathInfo().getSelectorString();
            if (selectors == null) {
                this.selectorsPath = null;
            } else {
                this.selectorsPath = "/"
                    + selectors.toLowerCase().replace('.', '/');
            }

            // path prefixes
            this.path = resourceTypePath.startsWith("/") ? EMPTY_PATH : path;
            this.pathIdx = -1;

            // prepare the first entry
            this.nextPath = "";
            seek();
        }

        private ScriptPathIterator(String relPath, String[] path) {

            this.resourceTypePath = relPath;
            this.selectorsPath = null;

            // path prefixes
            this.path = resourceTypePath.startsWith("/") ? EMPTY_PATH : path;
            this.pathIdx = -1;

            // prepare the first entry
            this.nextPath = "";
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
}