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
package org.apache.sling.api.wrappers;

import javax.servlet.http.HttpServletRequest;

/**
 * This class is not a "wrapper" per se, but computes the correct path info,
 * request URI, etc. for included requests. When including a request via
 * {@link javax.servlet.RequestDispatcher}, the Servlet API specifies that
 * target paths of the included request are available as request attributes.
 * {@code Request.getPathInfo()}, for example will return the value for the
 * including request, *not* for the included one.
 * <p>
 * This class is not intended to be extended or instantiated because it just
 * provides constants and static utility methods not intended to be overwritten.
 */
public class SlingRequestPaths {

    /**
     * Attribute name used by the RequestDispatcher to indicate the context path
     * of the included request, as a String.
     */
    public static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";

    /**
     * Attribute name used by the RequestDispatcher to indicate the path info of
     * the included request, as a String.
     */
    public static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * Attribute name used by the RequestDispatcher to indicate the query string
     * of the included request, as a String.
     */
    public static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

    /**
     * Attribute name used by the RequestDispatcher to indicate the request URI
     * of the included request, as a String.
     */
    public static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";

    /**
     * Attribute name used by the RequestDispatcher to indicate the servlet path
     * of the included request, as a String.
     */
    public static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    /**
     * Return the context path for r, using the appropriate request attribute if
     * the request is an included one.
     */
    public static String getContextPath(HttpServletRequest r) {
        final String attr = (String) r.getAttribute(INCLUDE_CONTEXT_PATH);
        return attr != null ? attr : r.getContextPath();
    }

    /**
     * Return the context path for r, using the appropriate request attribute if
     * the request is an included one.
     */
    public static String getPathInfo(HttpServletRequest r) {
        final String attr = (String) r.getAttribute(INCLUDE_PATH_INFO);
        return attr != null ? attr : r.getPathInfo();
    }

    /**
     * Return the query string for r, using the appropriate request attribute if
     * the request is an included one.
     */
    public static String getQueryString(HttpServletRequest r) {
        final String attr = (String) r.getAttribute(INCLUDE_QUERY_STRING);
        return attr != null ? attr : r.getQueryString();
    }

    /**
     * Return the request URI for r, using the appropriate request attribute if
     * the request is an included one.
     */
    public static String getRequestURI(HttpServletRequest r) {
        final String attr = (String) r.getAttribute(INCLUDE_REQUEST_URI);
        return attr != null ? attr : r.getRequestURI();
    }

    /**
     * Return the servlet path for r, using the appropriate request attribute if
     * the request is an included one.
     */
    public static String getServletPath(HttpServletRequest r) {
        final String attr = (String) r.getAttribute(INCLUDE_SERVLET_PATH);
        return attr != null ? attr : r.getServletPath();
    }

    /**
     * True if r is an included request, in which case it has the
     * INCLUDE_REQUEST_URI attribute
     */
    public static boolean isIncluded(HttpServletRequest r) {
        return r.getAttribute(INCLUDE_REQUEST_URI) != null;
    }
}
