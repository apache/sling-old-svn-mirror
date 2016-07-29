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
package org.apache.sling.servlets.get.impl;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.get.impl.helpers.JsonRendererServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RedirectServlet</code> implements support for GET requests to
 * resources of type <code>sling:redirect</code>. This servlet tries to get the
 * redirect target by
 * <ul>
 * <li>first adapting the resource to a {@link ValueMap} and trying to get the
 * property <code>sling:target</code>.</li>
 * <li>The second attempt is to access the resource <code>sling:target</code>
 * below the requested resource and attapt this to a string.</li>
 * </ul>
 * <p>
 * If there is no value found for <code>sling:target</code> a 404 (NOT FOUND)
 * status is sent by this servlet. Otherwise a 302 (FOUND, temporary redirect)
 * status is sent where the target is the relative URL from the current resource
 * to the target resource. Selectors, extension, suffix and query string are
 * also appended to the redirect URL.
 */
@SuppressWarnings("serial")
@Component(immediate=true, metatype=true)
@Service(Servlet.class)
@Properties({
    @Property(name="service.description", value="Request Redirect Servlet"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="sling.servlet.resourceTypes", value="sling:redirect", propertyPrivate=true),
    @Property(name="sling.servlet.methods", value="GET", propertyPrivate=true),
    @Property(name="sling.servlet.prefix", intValue=-1, propertyPrivate=true)   
})
public class RedirectServlet extends SlingSafeMethodsServlet {

    /** The name of the target property */
    public static final String TARGET_PROP = "sling:target";

    /** The name of the redirect status property */
    public static final String STATUS_PROP = "sling:status";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Servlet jsonRendererServlet;

    /** Default value for the maximum amount of results that should be returned by the jsonResourceWriter */
    public static final int DEFAULT_JSON_RENDERER_MAXIMUM_RESULTS = 200;

    @Property(intValue=DEFAULT_JSON_RENDERER_MAXIMUM_RESULTS)
    public static final String JSON_RENDERER_MAXIMUM_RESULTS_PROPERTY = "json.maximumresults";

    private int jsonMaximumResults;

    protected void activate(ComponentContext ctx) {
      Dictionary<?, ?> props = ctx.getProperties();
      this.jsonMaximumResults = OsgiUtil.toInteger(props.get(JSON_RENDERER_MAXIMUM_RESULTS_PROPERTY),
          DEFAULT_JSON_RENDERER_MAXIMUM_RESULTS);
      // When the maximumResults get updated, we force a reset for the jsonRendererServlet.
      jsonRendererServlet = getJsonRendererServlet();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        // handle json export of the redirect node
        if (JsonRendererServlet.EXT_JSON.equals(request.getRequestPathInfo().getExtension())) {
            getJsonRendererServlet().service(request, response);
            return;
        }

        // check for redirectability
        if (response.isCommitted()) {
            // committed response cannot be redirected
            log.warn("RedirectServlet: Response is already committed, not redirecting");
            request.getRequestProgressTracker().log(
                "RedirectServlet: Response is already committed, not redirecting");
            return;
        } else if (request.getAttribute(SlingConstants.ATTR_REQUEST_SERVLET) != null) {
            // included request will not redirect
            log.warn("RedirectServlet: Servlet is included, not redirecting");
            request.getRequestProgressTracker().log(
                "RedirectServlet: Servlet is included, not redirecting");
            return;
        }

        String targetPath = null;

        // convert resource to a value map
        final Resource rsrc = request.getResource();
        final ValueMap valueMap = rsrc.adaptTo(ValueMap.class);
        if (valueMap != null) {
            targetPath = valueMap.get(TARGET_PROP, String.class);
        }
        if (targetPath == null) {
            // old behaviour
            final Resource targetResource = request.getResourceResolver().getResource(
                rsrc, TARGET_PROP);
            if (targetResource == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Missing target for redirection");
                return;
            }

            // if the target resource is a path (string), redirect there
            targetPath = targetResource.adaptTo(String.class);
        }

        // if we got a target path, make it external and redirect to it
        if (targetPath != null) {
            if (!isUrl(targetPath)) {
                // make path relative and append selectors, extension etc.
                // this is an absolute URI suitable for the Location header
                targetPath = toRedirectPath(targetPath, request);
            } else {
                // just append any selectors, extension, suffix and query string
                targetPath = appendSelectorsExtensionSuffixQuery(request,
                    new StringBuilder(targetPath)).toString();
            }

            final int status = getStatus(valueMap);

            // redirect the client, use our own setup since we might have a
            // custom response status and we already have converted the target
            // into an absolute URI.
            response.reset();
            response.setStatus(status);
            response.sendRedirect(targetPath);
            response.flushBuffer();

            return;
        }

        // no way of finding the target, just fail
        response.sendError(HttpServletResponse.SC_NOT_FOUND,
            "Cannot redirect to target resource " + targetPath);
    }

    /**
     * Returns the response status from the {@link #STATUS_PROP} property in the
     * value map. If <code>valueMap</code> is <code>null</code>, the property is
     * not contained in the map or if the value is outside of the value HTTP
     * response status range of [ 100 .. 999 ], the default status 302/FOUND is
     * returned.
     *
     * @param valueMap The <code>valueMap</code> providing the optional status
     *            property.
     * @return The status value as defined above.
     */
    static int getStatus(final ValueMap valueMap) {
        if (valueMap != null) {
            final Integer statusInt = valueMap.get(STATUS_PROP, Integer.class);
            if (statusInt != null) {
                int status = statusInt.intValue();
                if (status >= 100 && status <= 999) {
                    return status;
                }
            }
        }

        // fall back to default value
        return HttpServletResponse.SC_FOUND;

    }

    /**
     * Create an absolute URI suitable for the "Location" response header
     * including any selectors, extension, suffix and query from the current
     * request.
     */
    static String toRedirectPath(String targetPath,
            SlingHttpServletRequest request) {

        // make sure the target path is absolute
        final String rawAbsPath;
        if (targetPath.startsWith("/")) {
            rawAbsPath = targetPath;
        } else {
            rawAbsPath = request.getResource().getPath() + "/" + targetPath;
        }

        final StringBuilder target = new StringBuilder();

        // and ensure the path is normalized, us unnormalized if not possible
        final String absPath = ResourceUtil.normalize(rawAbsPath);
        if (absPath == null) {
            target.append(rawAbsPath);
        } else {
            target.append(absPath);
        }

        appendSelectorsExtensionSuffixQuery(request, target);

        // return the mapped full path
        return request.getResourceResolver().map(request, target.toString());
    }

    /**
     * Appends optional request selectors, extension, suffix and query string to
     * the URL to be prepared in the target string builder and returns the
     * string builder.
     *
     * @param request The Sling HTTP Servlet Request providing access to the
     *            data to be appended
     * @param target The String builder to append the data to. This must not be
     *            null.
     * @return The <code>target</code> string builder.
     * @throws NullPointerException if request or target is <code>null</code>.
     */
    private static StringBuilder appendSelectorsExtensionSuffixQuery(
            SlingHttpServletRequest request, StringBuilder target) {
        // append current selectors, extension and suffix
        final RequestPathInfo rpi = request.getRequestPathInfo();
        if (rpi.getExtension() != null) {

            if (rpi.getSelectorString() != null) {
                target.append('.').append(rpi.getSelectorString());
            }

            target.append('.').append(rpi.getExtension());

            if (rpi.getSuffix() != null) {
                target.append(rpi.getSuffix());
            }
        }

        // append current querystring
        if (request.getQueryString() != null) {
            target.append('?').append(request.getQueryString());
        }

        return target;
    }

    /**
     * Returns an absolute URI built from the given parameters.
     *
     * @param scheme The scheme for the URI to be built.
     * @param host The name of the host.
     * @param port The port or -1 to not add a port number to the URI. For
     *            <code>http</code> and <code>https</code> schemes the port is
     *            not added if it is the default port.
     * @param targetPath The path of the resulting URI. This path is expected to
     *            not be an absolute URI.
     * @return The absolute URI built from the components.
     */
    static String toAbsoluteUri(final String scheme, final String host,
            final int port, final String targetPath) {

        // 1. scheme and host
        final StringBuilder absUriBuilder = new StringBuilder();
        absUriBuilder.append(scheme).append("://").append(host);

        // 2. append the port depending on the scheme and whether the port is
        // the default or not
        if (port > 0) {
            if (!(("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443))) {
                absUriBuilder.append(':').append(port);
            }
        }

        // 3. the actual target path
        absUriBuilder.append(targetPath);
        return absUriBuilder.toString();
    }

    private Servlet getJsonRendererServlet() {
        if (jsonRendererServlet == null) {
            Servlet jrs = new JsonRendererServlet(jsonMaximumResults);
            try {
                jrs.init(getServletConfig());
            } catch (Exception e) {
                // don't care too much here
            }
            jsonRendererServlet = jrs;
        }
        return jsonRendererServlet;
    }

    /**
     * Returns <code>true</code> if the path is potentially an URL. This
     * checks whether the path starts with a scheme followed by a colon
     * according to <a href="http://www.faqs.org/rfcs/rfc2396.html">RFC-2396</a>:
     * <pre>
     *     scheme = alpha *( alpha | digit | "+" | "-" | "." )
     *     alpha  = [ "A" .. "Z", "a" .. "z" ]
     *     digit  = [ "0" .. "9" ]
     * </pre>
     */
    private static boolean isUrl(final String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == ':') {
                return true;
            }
            if (!((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (i > 0
                            && ((c >= '0' && c <= '9')
                                    || c == '.'
                                    || c == '+'
                                    || c == '-')))) {
                break;
            }
        }
        return false;
    }

}
