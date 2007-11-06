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
 * The <code>Constants</code> interface provides some symbolic constants for
 * well known constant strings in Sling. Even though these constants will never
 * change, it is recommended that applications refer to the symbolic constants
 * instead of code the strings themselves.
 */
public interface Constants {

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
     * The name of the request attribute containing the
     * <code>JcrContentManager</code> (value is
     * "org.apache.sling.jcr.content_manager").
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.content.ContentManager</code>.
     */
    public static final String ATTR_CONTENT_MANAGER = "org.apache.sling.jcr.content_manager";

    /**
     * The name of the request attribute containing the <code>Component</code>
     * which included the component currently being active (value is
     * "org.apache.sling.component.request.component"). This attribute is
     * <code>null</code> if the current component is the component handling
     * the client request.
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.component.Component</code>.
     */
    public static final String ATTR_REQUEST_COMPONENT = "org.apache.sling.component.request.component";

    /**
     * The name of the request attribute containing the <code>Content</code>
     * underlying the <code>Compoennt</code> which included the component
     * currently being active (value is
     * "org.apache.sling.component.request.content"). This attribute is
     * <code>null</code> if the current component is the component handling
     * the client request.
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.component.Content</code>.
     */
    public static final String ATTR_REQUEST_CONTENT = "org.apache.sling.component.request.content";

    /**
     * The name of the request attribute containing request context path if
     * Sling (the Sling Servlet actually) is called by the servlet containing as
     * a result of a standard Servlet request include (value is
     * "javax.servlet.include.context_path"). Sling never sets this attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>. This
     * attribute corresponds to the
     * <code>HttpServletRequest.getContextPath()</code>.
     */
    public static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";

    /**
     * The name of the request attribute containing request path info if Sling
     * (the Sling Servlet actually) is called by the servlet containing as a
     * result of a standard Servlet request include (value is
     * "javax.servlet.include.path_info"). Sling never sets this attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>. This
     * attribute corresponds to the
     * <code>HttpServletRequest.getPathInfo()</code>.
     */
    public static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * The name of the request attribute containing request query string if
     * Sling (the Sling Servlet actually) is called by the servlet containing as
     * a result of a standard Servlet request include (value is
     * "javax.servlet.include.query_string"). Sling never sets this attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>. This
     * attribute corresponds to the
     * <code>HttpServletRequest.getQueryString()</code>.
     */
    public static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

    /**
     * The name of the request attribute containing request uri if Sling (the
     * Sling Servlet actually) is called by the servlet containing as a result
     * of a standard Servlet request include (value is
     * "javax.servlet.include.request_uri"). Sling never sets this attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>. This
     * attribute corresponds to the
     * <code>HttpServletRequest.getRequestURI()</code>.
     */
    public static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";

    /**
     * The name of the request attribute containing servlet path if Sling (the
     * Sling Servlet actually) is called by the servlet containing as a result
     * of a standard Servlet request include (value is
     * "javax.servlet.include.servlet_path"). Sling never sets this attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>. This
     * attribute corresponds to the
     * <code>HttpServletRequest.getServletPath()</code>.
     */
    public static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    /**
     * The name of the request attribute containing the name of the
     * <code>Component</code> causing the error handling (value is
     * "org.apache.sling.error.componentId"). This attribute is only available
     * to error handling components. If the error occurred while the request
     * processing component is not known, this attribute is not set either.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_COMPONENT_ID = "org.apache.sling.error.componentId";

    /**
     * The name of the request attribute containing the exception thrown causing
     * the error handler to be called (value is
     * "javax.servlet.error.exception"). This attribute is only available to
     * error handling components and only if an exception has been thrown
     * causing error handling.
     * <p>
     * The type of the attribute value is <code>java.lang.Throwable</code>.
     */
    public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

    /**
     * The name of the request attribute containing the fully qualified class
     * name of the exception thrown causing the error handler to be called
     * (value is "javax.servlet.error.exception_type"). This attribute is only
     * available to error handling components and only if an exception has been
     * thrown causing error handling. This attribute is present for backwards
     * compatibility only. Error handling component implementors are advised to
     * use the {@link #ERROR_EXCEPTION Throwable} itself.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

    /**
     * The name of the request attribute containing the message of the error
     * situation (value is "javax.servlet.error.message"). If an exception
     * caused error handling, this is the exceptions message from
     * <code>Throwable.getMessage()</code>. If error handling is caused by a
     * call to one of the <code>ComponentResponse.sendError</code> methods,
     * this attribute contains the optional message.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_MESSAGE = "javax.servlet.error.message";

    /**
     * The name of the request attribute containing the URL requested by the
     * client during whose processing the error handling was caused (value is
     * "javax.servlet.error.request_uri"). This property is retrieved calling
     * the <code>ComponentRequest.getRequestURI()</code> method.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

    /**
     * The name of the request attribute containing the name of the servlet
     * which caused the error handling (value is
     * "javax.servlet.error.servlet_name"). This is actually not really the
     * servlet name but the result of calling the
     * <code>ComponentContext.getServerInfo()</code> on the component context
     * of error handler being called. The name of the component which caused the
     * error handling is available from the {@link #ERROR_COMPONENT_ID}
     * attribute.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

    /**
     * The name of the request attribute containing the status code sent to the
     * client (value is "javax.servlet.error.status_code"). Error handling
     * components may set this status code on their response to the client or
     * they may choose to set another status code. For example a handler for
     * NOT_FOUND status (404) may opt to redirect to a new location and thus not
     * set the 404 status but a MOVED_PERMANENTLY (301) status. If this
     * attribute is not set and the error handler is not configured to set its
     * own status code anyway, a default value of INTERNAL_SERVER_ERROR (500)
     * should be sent.
     * <p>
     * The type of the attribute value is <code>java.lang.Integer</code>.
     */
    public static final String ERROR_STATUS = "javax.servlet.error.status_code";

}
