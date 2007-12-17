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
package org.apache.sling.core;

/**
 * The <code>CoreConstants</code> interface provides some symbolic constants
 * for well known constant strings in Sling. Even though these constants will
 * never change, it is recommended that applications refer to the symbolic
 * constants instead of code the strings themselves.
 */
public class CoreConstants {

    /**
     * The name of the framework property defining the Sling home directory
     * (value is "sling.home"). This is a Platform file system directory below
     * which all runtime data, such as the Felix bundle archives, logfiles, CRX
     * repository, etc., is located.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     * 
     * @see #SLING_HOME_URL
     */
    public static final String SLING_HOME = "sling.home";

    /**
     * The name of the framework property defining the Sling home directory as
     * an URL (value is "sling.home.url").
     * <p>
     * The value of this property is assigned the value of
     * <code>new File(${sling.home}).toURI().toString()</code> before
     * resolving the property variables.
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     * 
     * @see #SLING_HOME
     */
    public static final String SLING_HOME_URL = "sling.home.url";

    /**
     * The name of the framework property containing the identifier of the
     * running Sling instance (value is "sling.id"). This value of this property
     * is managed by this class and cannot be overwritten by the configuration
     * file(s).
     * <p>
     * This property is available calling the
     * <code>BundleContext.getProperty(String)</code> method.
     */
    public static final String SLING_ID = "sling.id";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the name of the servlet (value is
     * "sling.core.servletName"). If this property is missing or empty, the
     * <code>component.name</code> property or the <code>service.pid</code>
     * is used. If none of the three properties is defined, the Servlet is
     * ignored.
     */
    public static final String SLING_SERLVET_NAME = "sling.core.servletName";

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the resource type(s) supported by the servlet (value
     * is "sling.core.resourceTypes"). The type of this property is a String or
     * String[] (array of strings) denoting the resource types. If this property
     * is missing or empty the Servlet is ignored.
     */
    public static final String SLING_RESOURCE_TYPES = "sling.core.resourceTypes";

    /**
     * The name of the registered servlet used as the default servlet if no
     * other servlet or script could be selected (value is
     * "sling.core.servlet.default"). If no servlet is registered under this
     * name, the {@link org.apache.sling.core.servlets.DefaultServlet} is used.
     */
    public static final String DEFAULT_SERVLET_NAME = "sling.core.servlet.default";

}
