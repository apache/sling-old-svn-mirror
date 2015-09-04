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
package org.apache.sling.engine;

/**
 * The <code>CoreConstants</code> interface provides some symbolic constants for
 * well known constant strings in Sling. Even though these constants will never
 * change, it is recommended that applications refer to the symbolic constants
 * instead of code the strings themselves.
 */
public class EngineConstants {

    /**
     * The name of the framework property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, the
     * repository, etc., is located.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME_URL
     * @deprecated Use
     *             {@link org.apache.sling.settings.SlingSettingsService#SLING_HOME}
     */
    @Deprecated
    public static final String SLING_HOME = "sling.home";

    /**
     * The name of the framework property defining the Sling home directory as
     * an URL (value is "sling.home.url").
     * <p>
     * The value of this property is assigned the value of
     * <code>new File(${sling.home}).toURI().toString()</code> before resolving
     * the property variables.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     *
     * @see #SLING_HOME
     * @deprecated Use
     *             {@link org.apache.sling.settings.SlingSettingsService#SLING_HOME_URL}
     */
    @Deprecated
    public static final String SLING_HOME_URL = "sling.home.url";

    /**
     * The name of the request attribute providing the name of the currently
     * executing servlet (value is "sling.core.current.servletName"). This
     * attribute is set immediately before calling the
     * <code>Servlet.service()</code> method and reset to any previously stored
     * value after the service method returns.
     *
     * @deprecated Use
     *             {@link org.apache.sling.api.SlingConstants#SLING_CURRENT_SERVLET_NAME}
     */
    @Deprecated
    public static final String SLING_CURRENT_SERVLET_NAME = "sling.core.current.servletName";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the name of the servlet (value is
     * "sling.core.servletName"). If this property is missing or empty, the
     * <code>component.name</code> property or the <code>service.pid</code> is
     * used. If none of the three properties is defined, the Servlet is ignored.
     *
     * @deprecated
     */
    @Deprecated
    public static final String SLING_SERLVET_NAME = "sling.core.servletName";

    /**
     * The <code>javax.jcr.Session</code> request attribute used to be set by
     * the <i>SlingAuthenticator</i> upon successfull authentication. with the
     * release of the Authentication Core 1.0.0 bundle, this request attribute
     * is not set any more.
     * <p>
     * Applications requiring access to the JCR Session must be modified to get
     * the Resource Resolver from the Sling request (
     * <code>SlingHttpServletRequest.getResourceResolver()</code>) and adapt it
     * to the JCR Session.
     *
     * @deprecated as of bundle version 2.1
     */
    @Deprecated
    public static final String SESSION = "javax.jcr.Session";

    /**
     * The name of the service to be used for registering filters with the
     * {@link SlingRequestProcessor}.
     *
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_NAME = "javax.servlet.Filter";

    /**
     * The name of the service registration property of a
     * <code>java.servlet.Filter</code> service to be used as a filter by the
     * {@link SlingRequestProcessor}.
     * <p>
     * The specification of the <code>filter.scope</code> service registration
     * property is required for a <code>javax.servlet.Filter</code> service to
     * be used as a filter by the Sling Engine.
     * <p>
     * The <code>filter.scope</code> registration property is expected to be a
     * scalar String, an array of Strings or a Vector of Strings. Case of the
     * value is ignored.
     *
     * @see #FILTER_SCOPE_COMPONENT
     * @see #FILTER_SCOPE_ERROR
     * @see #FILTER_SCOPE_FORWARD
     * @see #FILTER_SCOPE_INCLUDE
     * @see #FILTER_SCOPE_REQUEST
     * @since 2.1, Sling Engine 2.2
     * @deprecated Use {@link #SLING_FILTER_SCOPE} instead
     */
    @Deprecated
    public static final String FILTER_SCOPE = "filter.scope";

    /**
     * The name of the service registration property of a
     * <code>java.servlet.Filter</code> service to be used as a filter by the
     * {@link SlingRequestProcessor}.
     * <p>
     * The specification of the <code>sling.filter.scope</code> service registration
     * property is required for a <code>javax.servlet.Filter</code> service to
     * be used as a filter by the Sling Engine.
     * <p>
     * The <code>sling.filter.scope</code> registration property is expected to be a
     * scalar String, an array of Strings or a Vector of Strings. Case of the
     * value is ignored.
     *
     * @see #FILTER_SCOPE_COMPONENT
     * @see #FILTER_SCOPE_ERROR
     * @see #FILTER_SCOPE_FORWARD
     * @see #FILTER_SCOPE_INCLUDE
     * @see #FILTER_SCOPE_REQUEST
     * @since 2.1, Sling Engine 2.2
     */
    public static final String SLING_FILTER_SCOPE = "sling.filter.scope";
    
    /**
     *@since 2.2, Sling Engine 2.4
     */
    public static final String SLING_FILTER_PATTERN = "sling.filter.pattern";

    /**
     * Filter scope value identifying a component level filter.
     * <p>
     * Component level filters are called once at the beginning of each request
     * (after the {@link #FILTER_SCOPE_REQUEST} filters) and for each resource
     * being included or forwarded. The order of calling the filters is defined
     * by the <code>service.ranking</code> (or {@link #FILTER_ORDER}) property
     * and the service registration ID.
     *
     * @see #SLING_FILTER_SCOPE
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_SCOPE_COMPONENT = "COMPONENT";

    /**
     * Filter scope value identifying an error level filter.
     * <p>
     * Error level filters are called before calling the
     * {@link org.apache.sling.engine.servlets.ErrorHandler} due to either a
     * <code>HttpServletResponse.sendError</code> call or due to an uncaught
     * <code>Throwable</code>. The order of calling the filters is defined by
     * the <code>service.ranking</code> (or {@link #FILTER_ORDER}) property and
     * the service registration ID.
     *
     * @see #FILTER_SCOPE
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_SCOPE_ERROR = "ERROR";

    /**
     * Filter scope value identifying an include level filter.
     * <p>
     * Include level filters are called before the script handling an included
     * resource is called. A resource is considered included if it rendered as a
     * result of calling <code>RequestDispatcher.include</code>. This may also
     * be induced by any JSP tags causing the
     * <code>RequestDispatcher.include</code> method being called, e.g.
     * <code>&lt;sling:include&gt;</code>. The order of calling the filters is
     * defined by the <code>service.ranking</code> (or {@link #FILTER_ORDER})
     * property and the service registration ID.
     *
     * @see #FILTER_SCOPE
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_SCOPE_INCLUDE = "INCLUDE";

    /**
     * Filter scope value identifying a forward level filter.
     * <p>
     * Forward level filters are called before the script handling a forwarded
     * resource is called. A resource is considered forwarded if it rendered as
     * a result of calling <code>RequestDispatcher.forward</code>. This may also
     * be induced by any JSP tags causing the
     * <code>RequestDispatcher.forward</code> method being called, e.g.
     * <code>&lt;sling:forward&gt;</code>. The order of calling the filters is
     * defined by the <code>service.ranking</code> (or {@link #FILTER_ORDER})
     * property and the service registration ID.
     *
     * @see #FILTER_SCOPE
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_SCOPE_FORWARD = "FORWARD";

    /**
     * Filter scope value identifying a request level filter.
     * <p>
     * Request level filters are called once at the beginning of each request.
     * The order of calling the filters is defined by the
     * <code>service.ranking</code> (or {@link #FILTER_ORDER}) property and the
     * service registration ID.
     *
     * @see #FILTER_SCOPE
     * @since 2.1, Sling Engine 2.2
     */
    public static final String FILTER_SCOPE_REQUEST = "REQUEST";

    /**
     * The name of the service registration property previously used to place an
     * order amongst filters. Starting with the Sling Engine 2.2, filters should
     * be registered with a standard <code>service.ranking</code> service
     * property to implement filter ordering.
     * <p>
     * If a service is registered with both a <code>service.ranking</code> and a
     * <code>filter.order</code> property, the <code>service.ranking</code>
     * property is used for ordering. If only a <code>filter.order</code>
     * property is used it is currently still used.
     *
     * @since 2.1, Sling Engine 2.2
     * @deprecated Use <code>"service.ranking"</code>
     *             (org.osgi.framework.Constants.SERVICE_RANKING) instead
     */
    @Deprecated
    public static final String FILTER_ORDER = "filter.order";
}
