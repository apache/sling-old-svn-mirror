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
package org.apache.sling.api.servlets;

/**
 * Service registration properties and common values used for registering a Sling Servlet.
 *
 * @see <a href="http://sling.apache.org/documentation/the-sling-engine/servlets.html">Servlets and Scripts</a>
 * @since 2.2.0
 */
public final class ServletResolverConstants {

    /**
     * The name of the service registration property of a servlet registered as
     * a service providing the absolute paths under which the servlet is
     * accessible as a resource (value is "sling.servlet.paths").
     * A relative path is made absolute by prefixing it with the value set through the
     * {@link #SLING_SERVLET_PREFIX} property.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the path(s) under which the servlet is registered in the
     * resource tree.
     * <p>
     * Either this property or the {@link #SLING_SERVLET_RESOURCE_TYPES}
     * property must be set or the servlet is ignored.
     * If both are set the servlet is registered using both ways.
     * <p>
     * A servlet using this property might be ignored unless its path is included
     * in the Execution Paths {@code servletresolver.paths} configuration setting of the
     * {@code org.apache.sling.servlets.resolver.internal.SlingServletResolver} service.
     */
    public static final String SLING_SERVLET_PATHS = "sling.servlet.paths";

    /**
     * The name of the service registration property of a servlet registered as
     * a service containing the resource type(s) supported by the servlet (value
     * is "sling.servlet.resourceTypes").
     * A relative resource type is made absolute by prefixing it with the value set through the
     * {@link #SLING_SERVLET_PREFIX} property.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types.
     * <p>
     * Either this property or the {@link #SLING_SERVLET_PATHS} property must be
     * set, or the servlet is ignored.
     * If both are set, the servlet is registered using both ways.
     */
    public static final String SLING_SERVLET_RESOURCE_TYPES = "sling.servlet.resourceTypes";

    /**
     * <p>
     * The name of the service registration property of a servlet registered as
     * a service providing the prefix/index to be used to register this servlet.
     * It only is applied as prefix to {@link #SLING_SERVLET_PATHS} and
     * {@link #SLING_SERVLET_RESOURCE_TYPES} in case they do not start with a "/".
     * </p>
     * <ul>
     * <li>If the value of this property is a number, it defines the index of the search
     * path entries from the resource resolver. The defined search path is used as
     * a prefix to mount this servlet. The number can be -1 which always points to the
     * last search entry. If the specified value is higher than than the highest index
     * of the search paths, the last entry is used. The index starts with 0.
     * If the value of this property is a string and parseable as a number, the above
     * logic is used.</li>
     * <li>If the value of this property is a string starting with "/", this value is applied
     * as a prefix, regardless of the configured search paths!</li>
     * <li>If the value is anything else, it is ignored.</li>
     * </ul>
     * If this property is not specified, the configuration of the {@code org.apache.sling.servlets.resolver.internal.SlingServletResolver}
     * service is used.
     * In case even that one is not set "/" is used as prefix.
     */
    public static final String SLING_SERVLET_PREFIX = "sling.servlet.prefix";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request URL selectors supported by the servlet
     * (value is "sling.servlet.selectors"). The selectors must be configured as
     * they would be specified in the URL that is as a list of dot-separated
     * strings such as <em>print.a4</em>.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_RESOURCE_TYPES} property is not set. Otherwise this property is
     * optional and ignored if not set.
     */
    public static final String SLING_SERVLET_SELECTORS = "sling.servlet.selectors";

    /**
     * The name of the service registration property of a servlet registered as
     * a service containing the request URL extensions supported by the servlet
     * for GET requests (value is "sling.servlet.extensions").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_RESOURCE_TYPES} property is not set.
     * Otherwise this property is optional and ignored if not set.
     */
    public static final String SLING_SERVLET_EXTENSIONS = "sling.servlet.extensions";

    /**
     * The name of the service registration property of a servlet registered as
     * a service containing the request methods supported by the servlet (value
     * is "sling.servlet.methods"). The value may be one of the HTTP methods or "*" for all methods.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_RESOURCE_TYPES} property is not set.
     * Otherwise this property is optional and assumed to be {@code GET} and {@code HEAD} if not set.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3">HTTP 1.1 Spec Methods</a>
     */
    public static final String SLING_SERVLET_METHODS = "sling.servlet.methods";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the name of the servlet (value is
     * "sling.core.servletName"). If this property is missing or empty, the
     * <code>component.name</code> property or the <code>service.pid</code>
     * is used. If none of the three properties is defined, the Servlet is
     * ignored.
     * @see javax.servlet.ServletConfig#getServletName()
     */
    public static final String SLING_SERVLET_NAME = "sling.core.servletName";

    /**
     * The resource type of a registered servlet used as the default servlet if
     * no other (more specific) servlet or script could be selected (value is
     * "sling/servlet/default"). If no servlet is registered under this name,
     * the {@code org.apache.sling.servlets.resolver.internal.defaults.DefaultServlet} is
     * used.
     * @see <a href="http://sling.apache.org/documentation/the-sling-engine/servlets.html#default-servlets">Default Servlet(s)</a>
     */
    public static final String DEFAULT_RESOURCE_TYPE = "sling/servlet/default";

    /**
     * The resource type used to select error handlers (value is
     * "sling/servlet/errorhandler").
     * @see <a href="http://sling.apache.org/documentation/the-sling-engine/errorhandling.html">Errorhandling</a>
     */
    public static final String DEFAULT_ERROR_HANDLER_RESOURCE_TYPE = "sling/servlet/errorhandler";

    /**
     * The HTTP "method" name of an error handler used as the default (value is
     * "default").
     * @see <a href="http://sling.apache.org/documentation/the-sling-engine/errorhandling.html#default-handler">Default Error Handle</a>
     */
    public static final String DEFAULT_ERROR_HANDLER_METHOD = "default";
}
