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
package org.apache.sling.servlets.resolver;

public class ServletResolverConstants {

    /**
     * The name of the service registration property of a Servlet registered as
     * a service providing the absolute paths under which the servlet is
     * accessible as a Resource (value is "sling.servlet.paths")
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types.
     */
    public static final String SLING_SERVLET_PATHS = "sling.servlet.paths";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the resource type(s) supported by the servlet (value
     * is "sling.servlet.resourceTypes").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_PATHS} property is set. Otherwise this property
     * must be set or the servlet is ignored.
     */
    public static final String SLING_SERVLET_RESOURCE_TYPES = "sling.servlet.resourceTypes";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request URL selectors supported by the servlet
     * (value is "sling.servlet.selectors"). The selectors must be configured as
     * they would be specified in the URL that is as a list of dot-separated
     * strings such as <em>print.a4</em>.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_PATHS} property is set. Otherwise this property is
     * optional and ignored if not set.
     */
    public static final String SLING_SERVLET_SELECTORS = "sling.servlet.selectors";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request URL extensions supported by the servlet
     * for GET requests (value is "sling.servlet.extensions").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_PATHS} property is set. Otherwise this property or
     * the {@link #SLING_SERVLET_METHODS} is optional and ignored if not set.
     */
    public static final String SLING_SERVLET_EXTENSIONS = "sling.servlet.extensions";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request methods supported by the servlet (value
     * is "sling.servlet.methods").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #SLING_SERVLET_PATHS} property is set. Otherwise this property or
     * the {@link #SLING_SERVLET_EXTENSIONS} is optional and ignored if not set.
     */
    public static final String SLING_SERVLET_METHODS = "sling.servlet.methods";

    /**
     * The resource type of a registered servlet used as the default servlet if
     * no other servlet or script could be selected (value is
     * "sling/servlet/default"). If no servlet is registered under this name,
     * the {@link org.apache.sling.servlets.resolver.defaults.DefaultServlet} is
     * used.
     */
    public static final String DEFAULT_SERVLET_NAME = "sling/servlet/default";

    /**
     * The resource used to select error handlers (value is
     * "sling/servlet/errorhandler").
     */
    public static final String ERROR_HANDLER_PATH = "sling/servlet/errorhandler";

}
