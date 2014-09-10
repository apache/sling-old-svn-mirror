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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
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

    /**
     * Request header commonly set by Ajax Frameworks to indicate the request is
     * posted as an Ajax request. The value set is expected to be
     * {@link #XML_HTTP_REQUEST}.
     * <p>
     * This header is known to be set by JQuery, ExtJS and Prototype. Other
     * client-side JavaScript framework most probably also set it.
     *
     * @see #isAjaxRequest(javax.servlet.http.HttpServletRequest)
     */
    private static final String X_REQUESTED_WITH = "X-Requested-With";

    /**
     * The expected value of the {@link #X_REQUESTED_WITH} request header to
     * identify a request as an Ajax request.
     *
     * @see #isAjaxRequest(javax.servlet.http.HttpServletRequest)
     */
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";

    /**
     * Request header providing the clients user agent information used
     * by {@link #isBrowserRequest(HttpServletRequest)} to decide whether
     * a request is probably sent by a browser or not.
     */
    private static final String USER_AGENT = "User-Agent";

    /**
     * String contained in a {@link #USER_AGENT} header indicating a Mozilla
     * class browser. Examples of such browsers are Firefox (generally Gecko
     * based browsers), Safari, Chrome (probably generally WebKit based
     * browsers), and Microsoft IE.
     */
    private static final String BROWSER_CLASS_MOZILLA = "Mozilla";

    /**
     * String contained in a {@link #USER_AGENT} header indicating a Opera class
     * browser. The only known browser in this class is the Opera browser.
     */
    private static final String BROWSER_CLASS_OPERA = "Opera";

    // no instantiation
    private AuthUtil() {
    }

    /**
     * Returns the value of the named request attribute or parameter as a string
     * as follows:
     * <ol>
     * <li>If there is a request attribute of that name, which is a non-empty
     * string, it is returned.</li>If there is a non-empty request parameter of
     * that name, this parameter is returned.
     * <li>Otherwise the <code>defaultValue</code> is returned.
     *
     * @param request The request from which to return the attribute or request
     *            parameter
     * @param name The name of the attribute/parameter
     * @param defaultValue The default value to use if neither a non-empty
     *            string attribute or a non-empty parameter exists in the
     *            request.
     * @return The attribute, parameter or <code>defaultValue</code> as defined
     *         above.
     */
    public static String getAttributeOrParameter(
            final HttpServletRequest request, final String name,
            final String defaultValue) {

        final String resourceAttr = getAttributeString(request, name);
        if (resourceAttr != null) {
            return resourceAttr;
        }

        final String resource = request.getParameter(name);
        if (resource != null && resource.length() > 0) {
            return resource;
        }

        return defaultValue;
    }

    /**
     * Returns any resource target to redirect to after successful
     * authentication. This method either returns a non-empty string or the
     * <code>defaultLoginResource</code> parameter. First the
     * <code>resource</code> request attribute is checked. If it is a non-empty
     * string, it is returned. Second the <code>resource</code> request
     * parameter is checked and returned if it is a non-empty string.
     *
     * @param request The request providing the attribute or parameter
     * @param defaultLoginResource The default login resource value
     * @return The non-empty redirection target or
     *         <code>defaultLoginResource</code>.
     */
    public static String getLoginResource(final HttpServletRequest request,
            String defaultLoginResource) {
        return getAttributeOrParameter(request, Authenticator.LOGIN_RESOURCE,
            defaultLoginResource);
    }

    /**
     * Ensures and returns the {@link Authenticator#LOGIN_RESOURCE} request
     * attribute is set to a non-null, non-empty string. If the attribute is not
     * currently set, this method sets it as follows:
     * <ol>
     * <li>If the {@link Authenticator#LOGIN_RESOURCE} request parameter is set
     * to a non-empty string, that parameter is set</li>
     * <li>Otherwise if the <code>defaultValue</code> is a non-empty string the
     * default value is used</li>
     * <li>Otherwise the attribute is set to "/"</li>
     * </ol>
     *
     * @param request The request to check for the resource attribute
     * @param defaultValue The default value to use if the attribute is not set
     *            and the request parameter is not set. This parameter is
     *            ignored if it is <code>null</code> or an empty string.
     * @return returns the value of resource request attribute
     */
    public static String setLoginResourceAttribute(
            final HttpServletRequest request, final String defaultValue) {
        String resourceAttr = getAttributeString(request,
            Authenticator.LOGIN_RESOURCE);
        if (resourceAttr == null) {
            final String resourcePar = request.getParameter(Authenticator.LOGIN_RESOURCE);
            if (resourcePar != null && resourcePar.length() > 0) {
                resourceAttr = resourcePar;
            } else if (defaultValue != null && defaultValue.length() > 0) {
                resourceAttr = defaultValue;
            } else {
                resourceAttr = "/";
            }
            request.setAttribute(Authenticator.LOGIN_RESOURCE, resourceAttr);
        }
        return resourceAttr;
    }

    /**
     * Redirects to the given target path appending any parameters provided in
     * the parameter map.
     * <p>
     * This method implements the following functionality:
     * <ul>
     * <li>If the <code>params</code> map does not contain a (non-
     * <code>null</code>) value for the {@link Authenticator#LOGIN_RESOURCE
     * resource} entry, such an entry is generated from the request URI and the
     * (optional) query string of the given <code>request</code>.</li>
     * <li>The parameters from the <code>params</code> map or at least a single
     * {@link Authenticator#LOGIN_RESOURCE resource} parameter are added to the
     * target path for the redirect. Each parameter value is encoded using the
     * <code>java.net.URLEncoder</code> with UTF-8 encoding to make it safe for
     * requests</li>
     * </ul>
     * <p>
     * After checking the redirect target and creating the target URL from the
     * parameter map, the response buffer is reset and the
     * <code>HttpServletResponse.sendRedirect</code> is called. Any headers
     * already set before calling this method are preserved.
     *
     * @param request The request object used to get the current request URI and
     *            request query string if the <code>params</code> map does not
     *            have the {@link Authenticator#LOGIN_RESOURCE resource}
     *            parameter set.
     * @param response The response used to send the redirect to the client.
     * @param target The redirect target to validate. This path must be prefixed
     *            with the request's servlet context path. If this parameter is
     *            not a valid target request as per the
     *            {@link #isRedirectValid(HttpServletRequest, String)} method
     *            the target is modified to be the root of the request's
     *            context.
     * @param params The map of parameters to be added to the target path. This
     *            may be <code>null</code>.
     * @throws IOException If an error occurs sending the redirect request
     * @throws IllegalStateException If the response was committed or if a
     *             partial URL is given and cannot be converted into a valid URL
     * @throws InternalError If the UTF-8 character encoding is not supported by
     *             the platform. This should not be caught, because it is a real
     *             problem if the encoding required by the specification is
     *             missing.
     */
    public static void sendRedirect(final HttpServletRequest request,
            final HttpServletResponse response, final String target,
            Map<String, String> params) throws IOException {

        checkAndReset(response);

        StringBuilder b = new StringBuilder();
        if (AuthUtil.isRedirectValid(request, target)) {
            b.append(target);
        } else if (request.getContextPath().length() == 0) {
            b.append("/");
        } else {
            b.append(request.getContextPath());
        }

        if (params == null) {
            params = new HashMap<String, String>();
        }

        // ensure the login resource is provided with the redirect
        if (params.get(Authenticator.LOGIN_RESOURCE) == null) {
            String resource = request.getRequestURI();
            if (request.getQueryString() != null) {
                resource += "?" + request.getQueryString();
            }
            params.put(Authenticator.LOGIN_RESOURCE, resource);
        }

        b.append('?');
        Iterator<Entry<String, String>> ei = params.entrySet().iterator();
        while (ei.hasNext()) {
            Entry<String, String> entry = ei.next();
            if (entry.getKey() != null && entry.getValue() != null) {
                try {
                    b.append(entry.getKey()).append('=').append(
                        URLEncoder.encode(entry.getValue(), "UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    throw new InternalError(
                        "Unexpected UnsupportedEncodingException for UTF-8");
                }

                if (ei.hasNext()) {
                    b.append('&');
                }
            }
        }

        response.sendRedirect(b.toString());
    }

    /**
     * Returns the name request attribute if it is a non-empty string value.
     *
     * @param request The request from which to retrieve the attribute
     * @param name The name of the attribute to return
     * @return The named request attribute or <code>null</code> if the attribute
     *         is not set or is not a non-empty string value.
     */
    private static String getAttributeString(final HttpServletRequest request,
            final String name) {
        Object resObj = request.getAttribute(name);
        if ((resObj instanceof String) && ((String) resObj).length() > 0) {
            return (String) resObj;
        }

        // not set or not a non-empty string
        return null;
    }

    /**
     * Returns <code>true</code> if the the client just asks for validation of
     * submitted username/password credentials.
     * <p>
     * This implementation returns <code>true</code> if the request parameter
     * {@link #PAR_J_VALIDATE} is set to <code>true</code> (case-insensitve). If
     * the request parameter is not set or to any value other than
     * <code>true</code> this method returns <code>false</code>.
     *
     * @param request The request to provide the parameter to check
     * @return <code>true</code> if the {@link #PAR_J_VALIDATE} parameter is set
     *         to <code>true</code>.
     */
    public static boolean isValidateRequest(final HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter(AuthConstants.PAR_J_VALIDATE));
    }

    /**
     * Sends a 200/OK response to a credential validation request.
     * <p>
     * This method just overwrites the response status to 200/OK, sends no
     * content (content length header set to zero) and prevents caching on
     * clients and proxies. Any other response headers set before calling this
     * methods are preserved and sent along with the response.
     *
     * @param response The response object
     * @throws IllegalStateException if the response has already been committed
     */
    public static void sendValid(final HttpServletResponse response) {
        checkAndReset(response);
        try {
            response.setStatus(HttpServletResponse.SC_OK);

            // explicitly tell we have no content but set content type
            // to prevent firefox from trying to parse the response
            // (SLING-1841)
            response.setContentType("text/plain");
            response.setContentLength(0);

            // prevent the client from aggressively caching the response
            // (SLING-1841)
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");

            response.flushBuffer();
        } catch (IOException ioe) {
            getLog().error("Failed to send 200/OK response", ioe);
        }
    }

    /**
     * Sends a 403/FORBIDDEN response optionally stating the reason for this
     * response code in the {@link #X_REASON} header. The value for the
     * {@link #X_REASON} header is taken from
     * {@link AuthenticationHandler#FAILURE_REASON} request attribute if set.
     * <p>
     * This method just overwrites the response status to 403/FORBIDDEN, adds
     * the {@link AuthConstants#X_REASON} header and sends the reason as result
     * back. Any other response headers set before calling this methods are
     * preserved and sent along with the response.
     *
     * @param request The request object
     * @param response The response object
     * @throws IllegalStateException if the response has already been committed
     */
    public static void sendInvalid(final HttpServletRequest request,
            final HttpServletResponse response) {
        checkAndReset(response);
        try {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            Object reason = request.getAttribute(AuthenticationHandler.FAILURE_REASON);
            Object reasonCode = request.getAttribute(AuthenticationHandler.FAILURE_REASON_CODE);
            if (reason != null) {
                response.setHeader(AuthConstants.X_REASON, reason.toString());
                if ( reasonCode != null ) {
                    response.setHeader(AuthConstants.X_REASON_CODE, reasonCode.toString());
                }
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().println(reason);
            }

            response.flushBuffer();
        } catch (IOException ioe) {
            getLog().error("Failed to send 403/Forbidden response", ioe);
        }
    }

    /**
     * Check if the request is for this authentication handler.
     *
     * @param request the current request
     * @return true if the referer matches this handler, or false otherwise
     */
    public static boolean checkReferer(HttpServletRequest request, String loginForm) {
        //SLING-2165: if a Referer header is supplied check if it matches the login path for this handler
    	if ("POST".equals(request.getMethod())) {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                String expectedPath = String.format("%s%s", request.getContextPath(), loginForm);
                try {
                    URL uri = new URL(referer);
                    if (!expectedPath.equals(uri.getPath())) {
                        //not for this selector, so let the next one handle it.
                        return false;
                    }
                } catch (MalformedURLException e) {
                    getLog().debug("Failed to parse the referer value for the login form " + loginForm, e);
                }
            }
    	}
        return true;
    }

    /**
     * Returns <code>true</code> if the given redirect <code>target</code> is
     * valid according to the following list of requirements:
     * <ul>
     * <li>The <code>target</code> is neither <code>null</code> nor an empty
     * string</li>
     * <li>The <code>target</code> is not an URL which is identified by the
     * character sequence <code>://</code> separating the scheme from the host</li>
     * <li>The <code>target</code> is normalized such that it contains no
     * consecutive slashes and no path segment contains a single or double dot</li>
     * <li>The <code>target</code> must be prefixed with the servlet context
     * path</li>
     * <li>If a <code>ResourceResolver</code> is available as a request
     * attribute the <code>target</code> (without the servlet context path
     * prefix) must resolve to an existing resource</li>
     * <li>If a <code>ResourceResolver</code> is <i>not</i> available as a
     * request attribute the <code>target</code> must be an absolute path
     * starting with a slash character does not contain any of the characters
     * <code>&lt;</code>, <code>&gt;</code>, <code>'</code>, or <code>"</code>
     * in plain or URL encoding</li>
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
     * @param target The redirect target to validate. This path must be
     *      prefixed with the request's servlet context path.
     * @return <code>true</code> if the redirect target can be considered valid
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

        final String ctxPath = getContextPath(request);
        if (ctxPath.length() > 0 && !target.startsWith(ctxPath)) {
            getLog().warn("isRedirectValid: Redirect target '{}' does not start with servlet context path '{}'",
                target, ctxPath);
            return false;
        }

        // special case of requesting the servlet context root path
        if (ctxPath.length() == target.length()) {
            return true;
        }

        final String localTarget = target.substring(ctxPath.length());
        if (!localTarget.startsWith("/")) {
            getLog().warn(
                "isRedirectValid: Redirect target '{}' without servlet context path '{}' must be an absolute path",
                target, ctxPath);
            return false;
        }

        final int query = localTarget.indexOf('?');
        final String path = (query > 0) ? localTarget.substring(0, query) : localTarget;

        ResourceResolver resolver = getResourceResolver(request);
        if (resolver != null) {
            // assume all is fine if the path resolves to a resource
            if (!ResourceUtil.isNonExistingResource(resolver.resolve(request, path))) {
                return true;
            }

            // not resolving to a resource, check for illegal characters
        }

        final Pattern illegal = Pattern.compile("[<>'\"]");
        if (illegal.matcher(path).find()) {
            getLog().warn("isRedirectValid: Redirect target '{}' must not contain any of <>'\"", target);
            return false;
        }

        return true;
    }

    /**
     * Returns the context path from the request or an empty string if the
     * request is <code>null</code>.
     */
    private static String getContextPath(final HttpServletRequest request) {
        if (request != null) {
            return request.getContextPath();
        }
        return "";
    }

    /**
     * Returns the resource resolver set as the
     * {@link AuthenticationSupport#REQUEST_ATTRIBUTE_RESOLVER} request
     * attribute or <code>null</code> if the request object is <code>null</code>
     * or the resource resolver is not present.
     */
    private static ResourceResolver getResourceResolver(final HttpServletRequest request) {
        if (request != null) {
            return (ResourceResolver) request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
        }
        return null;
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
        final String userAgent = request.getHeader(USER_AGENT);
        if (userAgent != null && (userAgent.contains(BROWSER_CLASS_MOZILLA) || userAgent.contains(BROWSER_CLASS_OPERA))) {
            return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the request is to be considered an AJAX
     * request placed using the <code>XMLHttpRequest</code> browser host object.
     * Currently a request is considered an AJAX request if the client sends the
     * <i>X-Requested-With</i> request header set to <code>XMLHttpRequest</code>
     * .
     *
     * @param request The current request
     * @return <code>true</code> if the request can be considered an AJAX
     *         request.
     */
    public static boolean isAjaxRequest(final HttpServletRequest request) {
        return XML_HTTP_REQUEST.equals(request.getHeader(X_REQUESTED_WITH));
    }

    /**
     * Checks whether the response has already been committed. If so an
     * <code>IllegalStateException</code> is thrown. Otherwise the response
     * buffer is cleared leaving any headers and status already set untouched.
     *
     * @param response The response to check and reset.
     * @throws IllegalStateException if the response has already been committed
     */
    private static void checkAndReset(final HttpServletResponse response) {
        if (response.isCommitted()) {
            throw new IllegalStateException("Response is already committed");
        }
        response.resetBuffer();
    }

    /**
     * Helper method returning a <i>org.apache.sling.auth.core.AuthUtil</i> logger.
     */
    private static Logger getLog() {
        return LoggerFactory.getLogger("org.apache.sling.auth.core.AuthUtil");
    }
}
