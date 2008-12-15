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
package org.apache.sling.api.resource;

import java.util.Iterator;


/**
 * The <code>ResourceUtil</code> class provides helper methods dealing
 * with resources.
 */
public class ResourceUtil {

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
     * @return <code>null</code> if <code>path</code> is the root path (<code>/</code>)
     *         or if <code>path</code> is a single name containing no slash (<code>/</code>)
     *         characters.
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

        // find the last slash, after which to cut off
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            // no slash in the path
            return null;
        } else if (lastSlash == 0) {
            // parent is root
            return "/";
        }

        return path.substring(0, lastSlash);
    }

    /**
     * Utility method returns the parent resource of the resource.
     * @throws NullPointerException If <code>rsrc</code> is <code>null</code>.
     * @return The parent resource or null if the rsrc is the root.
     */
    public static Resource getParent(Resource rsrc) {
        final String parentPath = getParent(rsrc.getPath());
        if ( parentPath == null ) {
            return null;
        }
        return rsrc.getResourceResolver().getResource(parentPath);
    }

    /**
     * Utility method returns the parent resource of the resource.
     * @throws NullPointerException If <code>rsrc</code> is <code>null</code>.
     */
    public static String getName(Resource rsrc) {
        final String name = getName(rsrc.getPath());
        return name;
    }

    /**
     * Utility method returns the name of the given <code>path</code>, which
     * is normalized by {@link #normalize(String)} before resolving the name.
     *
     * @param path The path whose name (the last path element) is to be
     *            returned.
     * @return The empty string if <code>path</code> is the root path (<code>/</code>)
     *         or if <code>path</code> is a single name containing no slash (<code>/</code>)
     *         characters.
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

    /**
     * Returns <code>true</code> if the resource <code>res</code> is a
     * synthetic resource.
     * <p>
     * This method checks whether the resource is an instance of the
     * <code>org.apache.sling.resource.SyntheticResource</code> class.
     *
     * @param res The <code>Resource</code> to check whether it is a synthetic
     *            resource.
     * @return <code>true</code> if <code>res</code> is a synthetic
     *         resource. <code>false</code> is returned if <code>res</code>
     *         is <code>null</code> or not an instance of the
     *         <code>org.apache.sling.resource.SyntheticResource</code> class.
     */
    public static boolean isSyntheticResource(Resource res) {
        return res instanceof SyntheticResource;
    }

    /**
     * Returns <code>true</code> if the resource <code>res</code> is a "star
     * resource". A <i>star resource</i> is a resource returned from the
     * <code>ResourceResolver.resolve(HttpServletRequest)</code> whose path
     * terminates in a <code>/*</code>. Generally such resource result from
     * requests to something like <code>/some/path/*</code> or
     * <code>/some/path/*.html</code> which may be used web applications to
     * uniformly handle resources to be created.
     * <p>
     * This method checks whether the resource path ends with a <code>/*</code>
     * indicating such a star resource.
     *
     * @param res The <code>Resource</code> to check whether it is a star
     *            resource.
     * @return <code>true</code> if <code>res</code> is to be considered a
     *         star resource.
     * @throws NullPointerException if <code>res</code> is <code>null</code>.
     */
    public static boolean isStarResource(Resource res) {
        return res.getPath().endsWith("/*");
    }

    /**
     * Returns <code>true</code> if the resource <code>res</code> is a
     * non-existing resource.
     * <p>
     * This method checks the resource type of the resource to match the
     * well-known resource type <code>sling:nonexisting</code> of the
     * <code>NonExistingResource</code> class defined in the Sling API.
     *
     * @param res The <code>Resource</code> to check whether it is a
     *            non-existing resource.
     * @return <code>true</code> if <code>res</code> is to be considered a
     *         non-existing resource.
     * @throws NullPointerException if <code>res</code> is <code>null</code>.
     */
    public static boolean isNonExistingResource(Resource res) {
        return NonExistingResource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    /**
     * Returns an <code>Iterator</code> of {@link Resource} objects loaded
     * from the children of the given <code>Resource</code>.
     * <p>
     * This is a convenience method for {@link ResourceResolver#listChildren(Resource)}.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterator</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is
     *             <code>null</code>.
     * @throws org.apache.sling.api.SlingException If any error occurs acquiring
     *             the child resource iterator.
     * @see ResourceResolver#listChildren(Resource)
     */
    public static Iterator<Resource> listChildren(Resource parent) {
        return parent.getResourceResolver().listChildren(parent);
    }

    /**
     * Returns an <code>ValueMap</code> object for the given
     * <code>Resource</code>.
     * This method calls {@link Resource#adaptTo(Class)} with the
     * {@link ValueMap} class as an argument. If the <code>adaptTo</code>
     * method returns a map, this map is returned. If the resource is not
     * adaptable to a value map, an empty value map is returned.
     * If <code>null</code> is provided as the resource an empty map is
     * returned as well.
     * @param res The <code>Resource</code> to adapt to the value map.
     * @return A value map.
     */
    public static ValueMap getValueMap(final Resource res) {
        ValueMap map = (res == null ? null : res.adaptTo(ValueMap.class));
        if ( map == null ) {
            map = ValueMap.EMPTY;
        }
        return map;
    }
}
