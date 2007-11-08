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
package org.apache.sling.jcr.resource;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.sling.jcr.resource.internal.helper.LazyInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>JcrResourceUtil</code> class provides helper methods used throughout
 * this bundle.
 */
public class JcrResourceUtil {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(JcrResourceUtil.class);

    /** Helper method to execute a JCR query */
    public static QueryResult query(Session session, String query,
            String language) throws RepositoryException {
        QueryManager qManager = session.getWorkspace().getQueryManager();
        Query q = qManager.createQuery(query, language);
        return q.execute();
    }

    /** Converts a JCR Value to a corresponding Java Object */
    public static Object toJavaObject(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return new LazyInputStream(value);
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME: // fall through
            case PropertyType.PATH: // fall through
            case PropertyType.REFERENCE: // fall through
            case PropertyType.STRING: // fall through
            case PropertyType.UNDEFINED: // not actually expected
            default: // not actually expected
                return value.getString();
        }
    }

    /**
     * Resolves relative path segments '.' and '..' in the absolute path.
     * Returns null if not possible (.. points above root) or if path is not
     * absolute.
     */
    public static String normalize(String path) {

        // don't care for empty paths
        if (path.length() == 0) {
            log.error("normalize: Not modifying empty path");
            return path;
        }

        // prepare the path buffer with trailing slash (simplifies impl)
        int absOffset = (path.charAt(0) == '/') ? 0 : 1;
        char[] buf= new char[path.length() + 1 + absOffset];
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
                        log.error("normalize: Path '{}' cannot be resolved", path);
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
            resolved = new String(buf, absOffset, bufPos-absOffset);
        }

        log.debug("normalize: Resolving '{}' to '{}'", path, resolved);
        return resolved;
    }

    /**
     * Utility method returns the parent path of the given <code>path</code>,
     * which is normalized by {@link #normalize(String)} before resolving the
     * parent.
     *
     * @param path The path whose parent is to be returned.
     *
     * @return <code>null</code> if <code>path</code> is the root path (<code>/</code>)
     *      or if <code>path</code> is a single name containing no slash (<code>/</code>)
     *      characters.
     *
     * @throws IllegalArgumentException If the path cannot be normalized by
     *      the {@link #normalize(String)} method.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    public static String getParent(String path) {
        if ("/".equals(path)) {
            return null;
        }

        // normalize path (remove . and ..)
        path = normalize(path);

        // if normalized to root, there is no parent
        if ("/".equals(path)) {
            return null;
        }

        // find the last slash, after which to cut off
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            // no slash in the path
            return null;
        }

        return path.substring(0, lastSlash);
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
        int lastSlash = path.lastIndexOf('/');
        return (lastSlash >= 0) ? path.substring(lastSlash) : path;
    }

}
