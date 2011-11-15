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
package org.apache.sling.auth.core;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AuthUtil</code> provides utility functions for implementations of
 * {@link org.apache.sling.auth.core.spi.AuthenticationHandler} services and
 * users of the Sling authentication infrastructure.
 * <p>
 * This utility class can neither be extended from nor can it be instantiated.
 *
 * @since 1.1 (bundle version 1.0.8)
 */
public final class AuthUtil {

    // no instantiation
    private AuthUtil() {
    }

    /**
     * Returns <code>true</code> if the given redirect <code>target</code> is
     * valid according to the following list of requirements:
     * <ul>
     * <li>The <code>target</code> is neither <code>null</code> nor an empty
     * string</li>
     * <li>The <code>target</code> is not an URL which is identified by the
     * character sequence <code>://</code> separating the scheme from the host</li>
     * <li>The <code>target</code> is not normalized; that is it either contains
     * single or double dots in segments or consecutive slashes</li>
     * <li>If a <code>ResourceResolver</code> is available as a request
     * attribute the <code>target</code> must resolve to an existing resource</li>
     * <li>If a <code>ResourceResolver</code> is <i>not</i> available as a
     * request attribute the <code>target</code> must be an absolute path
     * starting with a slash character</li>
     * </ul>
     * <p>
     * If any of the conditions does not hold, the method returns
     * <code>false</code> and logs a <i>warning</i> level message with the
     * <i>org.apache.sling.auth.core.AuthUtil</i> logger.
     *
     * @param request Providing the <code>ResourceResolver</code> attribute and
     *            the context to resolve the resource from the
     *            <code>target</code>. This may be <code>null</code> which
     *            causes the target to not be validated with a
     *            <code>ResoureResolver</code>
     * @param target The redirect target to validate
     * @return <code>true</code> if the redirect target can be considered valid
     * @since 1.1 (bundle version 1.0.8)
     */
    public static boolean isRedirectValid(final HttpServletRequest request, final String target) {
        if (target == null || target.length() == 0) {
            getLog().warn("isRedirectValid: Redirect target must not be empty or null");
            return false;
        }

        if (target.contains("://")) {
            getLog().warn("isRedirectValid: Redirect target '{}' must not be an URL", target);
            return false;
        }

        if (target.contains("//") || target.contains("/../") || target.contains("/./") || target.endsWith("/.")
            || target.endsWith("/..")) {
            getLog().warn("isRedirectValid: Redirect target '{}' is not normalized", target);
            return false;
        }

        final int query = target.indexOf('?');
        final String path = (query > 0) ? target.substring(0, query) : target;

        if (request != null) {
            ResourceResolver resolver = (ResourceResolver) request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
            if (resolver != null) {
                final boolean isValid = !ResourceUtil.isNonExistingResource(resolver.resolve(request, path));
                if (!isValid) {
                    getLog().warn("isRedirectValid: Redirect target '{}' does not resolve to an existing resource",
                        target);
                }
                return isValid;
            }
        }

        final boolean isValid = target.startsWith("/");
        if (!isValid) {
            getLog().warn("isRedirectValid: Redirect target '{}' must be an absolute path", target);
        }
        return isValid;
    }

    /**
     * Returns <code>true</code> if the given request can be assumed to be sent
     * by a client browser such as Firefix, Internet Explorer, etc.
     * <p>
     * This method inspects the <code>User-Agent</code> header and returns
     * <code>true</code> if the header contains the string <i>Mozilla</i> (known
     * to be contained in Firefox, Internet Explorer, WebKit-based browsers
     * User-Agent) or <i>Opera</i> (known to be contained in the Opera
     * User-Agent).
     *
     * @param request The request to inspect
     * @return <code>true</code> if the request is assumed to be sent by a
     *         browser.
     */
    public static boolean isBrowserRequest(final HttpServletRequest request) {
        final String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.contains("Mozilla") || userAgent.contains("Opera"))) {
            return true;
        }
        return false;
    }

    /**
     * Helper method returning a <i>org.apache.sling.auth.core.AuthUtil</i> logger.
     */
    private static Logger getLog() {
        return LoggerFactory.getLogger("org.apache.sling.auth.core.AuthUtil");
    }
}
