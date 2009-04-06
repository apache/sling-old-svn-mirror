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
package org.apache.sling.api;

/**
 * The <code>SlingConstants</code> interface provides some symbolic constants
 * for well known constant strings in Sling. Even though these constants will
 * never change, it is recommended that applications refer to the symbolic
 * constants instead of code the strings themselves.
 */
public class SlingConstants {

    /**
     * The namespace prefix used throughout Sling (value is "sling").
     * <p>
     * The actual use depends on the environment. For example a
     * {@link org.apache.sling.api.resource.ResourceResolver} using a JCR
     * repository may name Sling node types and items using namespaces mapped to
     * this prefix. A JSP tag library for Sling may use this prefix as the
     * namespace prefix for its tags.
     */
    public static final String NAMESPACE_PREFIX = "sling";

    /**
     * The namespace URI prefix to be used by Sling projects to define
     * namespaces (value is "http://sling.apache.org/").
     * <p>
     * The actual namespace URI depends on the environment. For example a
     * {@link org.apache.sling.api.resource.ResourceResolver} using a JCR
     * repository may define its namespace as
     * <code><em>NAMESPACE_URI_ROOT + "jcr/sling/1.0"</em></code>. A JSP
     * tag library for Sling may define its namespace as
     * <code><em>NAMESPACE_URI_ROOT + "taglib/sling/1.0"</em></code>.
     */
    public static final String NAMESPACE_URI_ROOT = "http://sling.apache.org/";

    /**
     * The name of the request attribute containing the <code>Servlet</code>
     * which included the servlet currently being active (value is
     * "org.apache.sling.api.include.servlet"). This attribute is
     * <code>null</code> if the current servlet is the servlet handling the
     * client request.
     * <p>
     * The type of the attribute value is <code>javax.servlet.Servlet</code>.
     */
    public static final String ATTR_REQUEST_SERVLET = "org.apache.sling.api.include.servlet";

    /**
     * The name of the request attribute containing the <code>Resource</code>
     * underlying the <code>Servlet</code> which included the servlet
     * currently being active (value is
     * "org.apache.sling.api.include.resource"). This attribute is
     * <code>null</code> if the current servlet is the servlet handling the
     * client request.
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.api.resource.Resource</code>.
     */
    public static final String ATTR_REQUEST_CONTENT = "org.apache.sling.api.include.resource";

    // ---------- Error handling -----------------------------------------------

    /**
     * The name of the request attribute containing the exception thrown causing
     * the error handler to be called (value is
     * "javax.servlet.error.exception"). This attribute is only available to
     * error handling servlets and only if an exception has been thrown causing
     * error handling.
     * <p>
     * The type of the attribute value is <code>java.lang.Throwable</code>.
     */
    public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

    /**
     * The name of the request attribute containing the fully qualified class
     * name of the exception thrown causing the error handler to be called
     * (value is "javax.servlet.error.exception_type"). This attribute is only
     * available to error handling servlets and only if an exception has been
     * thrown causing error handling. This attribute is present for backwards
     * compatibility only. Error handling servlet implementors are advised to
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
     * call to one of the <code>SlingHttpServletResponse.sendError</code>
     * methods, this attribute contains the optional message.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_MESSAGE = "javax.servlet.error.message";

    /**
     * The name of the request attribute containing the URL requested by the
     * client during whose processing the error handling was caused (value is
     * "javax.servlet.error.request_uri"). This property is retrieved calling
     * the <code>SlingHttpServletRequest.getRequestURI()</code> method.
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

    /**
     * The name of the request attribute containing the name of the servlet
     * which caused the error handling (value is
     * "javax.servlet.error.servlet_name").
     * <p>
     * The type of the attribute value is <code>java.lang.String</code>.
     */
    public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

    /**
     * The name of the request attribute containing the status code sent to the
     * client (value is "javax.servlet.error.status_code"). Error handling
     * servlets may set this status code on their response to the client or they
     * may choose to set another status code. For example a handler for
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
