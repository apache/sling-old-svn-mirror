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
 * The <code>CoreConstants</code> interface provides some symbolic constants
 * for well known constant strings in Sling. Even though these constants will
 * never change, it is recommended that applications refer to the symbolic
 * constants instead of code the strings themselves.
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
     * The name of the request attribute providing the name of the currently
     * executing servlet (value is "sling.core.current.servletName"). This
     * attribute is set immediately before calling the
     * <code>Servlet.service()</code> method and reset to any previously
     * stored value after the service method returns.
     */
    public static final String SLING_CURRENT_SERVLET_NAME = "sling.core.current.servletName";

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
     * The name of the request attribute set by the
     * {@link org.apache.sling.engine.impl.auth.SlingAuthenticator}
     * when authenticating the request user (value is "javax.jcr.Session").
     * Existence of this attribute in the request, provided it is a JCR Session,
     * signals that authentication has already taken place. This may be used
     * when including through the servlet container.
     */
    public static final String SESSION = "javax.jcr.Session";

}
