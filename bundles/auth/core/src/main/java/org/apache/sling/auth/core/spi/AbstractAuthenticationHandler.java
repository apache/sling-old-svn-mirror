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
package org.apache.sling.auth.core.spi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.AuthUtil;
import org.slf4j.LoggerFactory;

/**
 * The <code>AbstractAuthenticationHandler</code> implements the
 * <code>AuthenticationHandler</code> interface and extends the
 * {@link DefaultAuthenticationFeedbackHandler} providing some helper methods
 * which may be used by authentication handlers.
 */
public abstract class AbstractAuthenticationHandler extends
        DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    /**
     * The name of the request parameter indicating that the submitted username
     * and password should just be checked and a status code be set for success
     * (200/OK) or failure (403/FORBIDDEN).
     *
     * @see #isValidateRequest(HttpServletRequest)
     * @see #sendValid(HttpServletResponse)
     * @see #sendInvalid(HttpServletRequest, HttpServletResponse)
     * @since 1.0.2 (Bundle version 1.0.4)
     */
    private static final String PAR_J_VALIDATE = "j_validate";

    /**
     * The name of the request header set by the
     * {@link #sendInvalid(HttpServletRequest, HttpServletResponse)} method if the provided
     * credentials cannot be used for login.
     * <p>
     * This header may be inspected by clients for a reason why the request
     * failed.
     *
     * @see #sendInvalid(HttpServletRequest, HttpServletResponse)
     * @since 1.0.2 (Bundle version 1.0.4)
     */
    private static final String X_REASON = "X-Reason";

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
     * @since 1.0.2 (Bundle version 1.0.4)
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
     * <li>The target path is prefixed with the request's context path to ensure
     * proper redirection into the same web application. Therefore the
     * <code>target</code> path parameter must not be prefixed with the context
     * path.</li>
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
     *
     * @param request The request object used to get the current request URI and
     *            request query string if the <code>params</code> map does not
     *            have the {@link Authenticator#LOGIN_RESOURCE resource}
     *            parameter set.
     * @param response The response used to send the redirect to the client.
     * @param target The target path to redirect the client to. This parameter
     *            must not be prefixed with the request's context path because
     *            this will be added by this method. If this parameter is not
     *            a valid target request as per the
     *            {@link #isRedirectValid(HttpServletRequest, String)} method
     *            the target is modified to be the root of the request's context.
     * @param params The map of parameters to be added to the target path. This
     *            may be <code>null</code>.
     * @throws IOException If an error occurs sending the redirect request
     * @throws IllegalStateException If the response was committed or if a
     *             partial URL is given and cannot be converted into a valid URL
     * @throws InternalError If the UTF-8 character encoding is not supported by
     *             the platform. This should not be caught, because it is a real
     *             problem if the encoding required by the specification is
     *             missing.
     * @since 1.0.2 (Bundle version 1.0.4)
     * @since 1.0.4 (bundle version 1.0.8) the target is validated with the
     *      {@link AuthUtil#isRedirectValid(HttpServletRequest, String)} method.
     */
    public static void sendRedirect(final HttpServletRequest request,
            final HttpServletResponse response, final String target,
            Map<String, String> params) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append(request.getContextPath());

        if (AuthUtil.isRedirectValid(request, target)) {
            b.append(target);
        } else {
            b.append("/");
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
     * Returns <code>true</code> if the given redirect <code>target</code> is
     * valid according to the following list of requirements:
     * <ul>
     * <li>The <code>target</code> is neither <code>null</code> nor an empty
     *   string</li>
     * <li>The <code>target</code> is not an URL which is identified by the
     *   character sequence <code>://</code> separating the scheme from the
     *   host</li>
     * <li>If a <code>ResourceResolver</code> is available as a request
     *   attribute the <code>target</code> must resolve to an existing resource
     *   </li>
     * <li>If a <code>ResourceResolver</code> is <i>not</i> available as a
     *   request attribute the <code>target</code> must be an absolute path
     *   starting with a slash character</li>
     * </ul>
     * <p>
     * If any of the conditions does not hold, the method returns
     * <code>false</code> and logs a <i>warning</i> level message with the
     * <i>org.apache.sling.auth.core.spi.AbstractAuthenticationHandler</i>
     * logger.
     *
     *
     * @param request Providing the <code>ResourceResolver</code> attribute
     *   and the context to resolve the resource from the <code>target</code>.
     *   This may be <code>null</code> which cause the target to not be
     *   validated with a <code>ResoureResolver</code>
     * @param target The redirect target to validate
     * @return <code>true</code> if the redirect target can be considered
     *  valid
     *
     * @since 1.0.4 (bundle version 1.0.8)
     */
    @Deprecated
    public static boolean isRedirectValid(final HttpServletRequest request,
            final String target) {
        return AuthUtil.isRedirectValid(request, target);
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
     * @since 1.0.2 (Bundle version 1.0.4)
     */
    public static boolean isValidateRequest(final HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter(PAR_J_VALIDATE));
    }

    /**
     * Sends a 200/OK response to a credential validation request.
     *
     * @param response The response object
     * @since 1.0.2 (Bundle version 1.0.4)
     */
    public static void sendValid(final HttpServletResponse response) {
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
            // TODO: log.error("Failed to send 200/OK response", ioe);
        }
    }

    /**
     * Sends a 403/FORBIDDEN response optionally stating the reason for
     * this response code in the {@link #X_REASON} header. The value for
     * the {@link #X_REASON} header is taken from
     * {@link AuthenticationHandler#FAILURE_REASON} request attribute if
     * set.
     *
     * @param request The request object
     * @param response The response object
     * @since 1.0.2 (Bundle version 1.0.4)
     */
    public static void sendInvalid(final HttpServletRequest request,
            final HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            Object reason = request.getAttribute(AuthenticationHandler.FAILURE_REASON);
            if (reason != null) {
                response.setHeader(X_REASON, reason.toString());
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().println(reason);
            }

            response.flushBuffer();
        } catch (IOException ioe) {
            // TODO: log.error("Failed to send 403/Forbidden response", ioe);
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
        		LoggerFactory.getLogger(AbstractAuthenticationHandler.class)
        			.debug("Failed to parse the referer value for the login form " + loginForm, e);
        	}
        }
        return true;
	}
}
