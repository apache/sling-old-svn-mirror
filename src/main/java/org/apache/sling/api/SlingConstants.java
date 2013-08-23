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
 * <p>
 * This class is not intended to be extended or instantiated because it just
 * provides constants not intended to be overwritten.
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
     * "org.apache.sling.api.include.servlet"). This attribute is only set if
     * the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>javax.servlet.Servlet</code>.
     */
    public static final String ATTR_REQUEST_SERVLET = "org.apache.sling.api.include.servlet";

    /**
     * The name of the request attribute containing the <code>Resource</code>
     * underlying the <code>Servlet</code> which included the servlet currently
     * being active (value is "org.apache.sling.api.include.resource"). This
     * attribute is only set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.api.resource.Resource</code>.
     */
    public static final String ATTR_REQUEST_CONTENT = "org.apache.sling.api.include.resource";

    /**
     * The name of the request attribute containing the
     * <code>RequestPathInfo</code> underlying the <code>Servlet</code> which
     * included the servlet currently being active (value is
     * "org.apache.sling.api.include.request_path_info"). This attribute is only
     * set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is
     * <code>org.apache.sling.api.request.RequestPathInfo</code>.
     */
    public static final String ATTR_REQUEST_PATH_INFO = "org.apache.sling.api.include.request_path_info";

    /**
     * The name of the request attribute containing the
     * <code>HttpServletRequest.getRequestURI()</code> of the request which
     * included the servlet currently being active underlying the
     * <code>Servlet</code> which included the servlet currently being active
     * (value is "javax.servlet.include.request_uri"). This attribute is only
     * set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>String</code>.
     * <p>
     * <b>Note:</b> In Sling, the
     * <code>HttpServletRequest.getRequestURI()</code> method will always return
     * the same result regardless of whether it is called from the client
     * request processing servlet or script or from an included servlet or
     * script. This request attribute is set for compatibility with the Servlet
     * API specification.
     */
    public static final String ATTR_INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";

    /**
     * The name of the request attribute containing the
     * <code>HttpServletRequest.getContextPath()</code> of the request which
     * included the servlet currently being active underlying the
     * <code>Servlet</code> which included the servlet currently being active
     * (value is "javax.servlet.include.context_path"). This attribute is only
     * set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>String</code>.
     * <p>
     * <b>Note:</b> In Sling, the
     * <code>HttpServletRequest.getContextPath()</code> method will always
     * return the same result regardless of whether it is called from the client
     * request processing servlet or script or from an included servlet or
     * script. This request attribute is set for compatibility with the Servlet
     * API specification.
     */
    public static final String ATTR_INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";

    /**
     * The name of the request attribute containing the
     * <code>HttpServletRequest.getServletPath()</code> of the request which
     * included the servlet currently being active underlying the
     * <code>Servlet</code> which included the servlet currently being active
     * (value is "javax.servlet.include.servlet_path"). This attribute is only
     * set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>String</code>.
     * <p>
     * <b>Note:</b> In Sling, the
     * <code>HttpServletRequest.getServletPath()</code> method will always
     * return the same result regardless of whether it is called from the client
     * request processing servlet or script or from an included servlet or
     * script. This request attribute is set for compatibility with the Servlet
     * API specification.
     */
    public static final String ATTR_INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    /**
     * The name of the request attribute containing the
     * <code>HttpServletRequest.getPathInfo()</code> of the request which
     * included the servlet currently being active underlying the
     * <code>Servlet</code> which included the servlet currently being active
     * (value is "javax.servlet.include.path_info"). This attribute is only set
     * if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>String</code>.
     * <p>
     * <b>Note:</b> In Sling, the <code>HttpServletRequest.getPathInfo()</code>
     * method will always return the same result regardless of whether it is
     * called from the client request processing servlet or script or from an
     * included servlet or script. This request attribute is set for
     * compatibility with the Servlet API specification.
     */
    public static final String ATTR_INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    /**
     * The name of the request attribute containing the
     * <code>HttpServletRequest.getQueryString()</code> of the request which
     * included the servlet currently being active underlying the
     * <code>Servlet</code> which included the servlet currently being active
     * (value is "javax.servlet.include.query_string"). This attribute is only
     * set if the serlvet or script is included via
     * <code>RequestDispatcher.include</code> from another servlet or script.
     * <p>
     * The type of the attribute value is <code>String</code>.
     * <p>
     * <b>Note:</b> In Sling, the
     * <code>HttpServletRequest.getQueryString()</code> method will always
     * return the same result regardless of whether it is called from the client
     * request processing servlet or script or from an included servlet or
     * script. This request attribute is set for compatibility with the Servlet
     * API specification.
     */
    public static final String ATTR_INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

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

    /**
     * The topic for the OSGi event which is sent when a resource has been added
     * to the resource tree.
     * The event contains at least the {@link #PROPERTY_PATH}, {@link #PROPERTY_RESOURCE_SUPER_TYPE}
     * and {@link #PROPERTY_RESOURCE_TYPE} properties.
     * @since 2.0.6
     */
    public static final String TOPIC_RESOURCE_ADDED = "org/apache/sling/api/resource/Resource/ADDED";

    /**
     * The topic for the OSGi event which is sent when a resource has been removed
     * from the resource tree.
     * The event contains at least the {@link #PROPERTY_PATH}. As the resource has already been removed
     * no further information like resource type etc. might be available.
     * @since 2.0.6
     */
    public static final String TOPIC_RESOURCE_REMOVED = "org/apache/sling/api/resource/Resource/REMOVED";

    /**
     * The topic for the OSGi event which is sent when a resource has been changed
     * in the resource tree.
     * The event contains at least the {@link #PROPERTY_PATH}, {@link #PROPERTY_RESOURCE_SUPER_TYPE}
     * and {@link #PROPERTY_RESOURCE_TYPE} properties.
     * Since 2.2.0 the event might contain these properties {@link #PROPERTY_ADDED_ATTRIBUTES},
     * {@link #PROPERTY_REMOVED_ATTRIBUTES}, {@link #PROPERTY_CHANGED_ATTRIBUTES}. All of them are
     * optional.
     * @since 2.0.6
     */
    public static final String TOPIC_RESOURCE_CHANGED = "org/apache/sling/api/resource/Resource/CHANGED";

    /**
     * The topic for the OSGi event which is sent when a resource provider has been
     * added to the resource tree.
     * The event contains at least the {@link #PROPERTY_PATH} property.
     * @since 2.0.6
     */
    public static final String TOPIC_RESOURCE_PROVIDER_ADDED = "org/apache/sling/api/resource/ResourceProvider/ADDED";

    /**
     * The topic for the OSGi event which is sent when a resource provider has been
     * removed from the resource tree.
     * The event contains at least the {@link #PROPERTY_PATH} property.
     * @since 2.0.6
     */
    public static final String TOPIC_RESOURCE_PROVIDER_REMOVED = "org/apache/sling/api/resource/ResourceProvider/REMOVED";

    /**
     * The topic for the OSGi event which is sent when the resource mapping changes.
     * @since 2.2.0
     */
    public static final String TOPIC_RESOURCE_RESOLVER_MAPPING_CHANGED = "org/apache/sling/api/resource/ResourceResolverMapping/CHANGED";

    /**
     * The name of the event property holding the resource path.
     * @since 2.0.6
     */
    public static final String PROPERTY_PATH = "path";

    /**
     * The name of the event property holding the userid. This property is optional.
     * @since 2.1.0
     */
    public static final String PROPERTY_USERID = "userid";

    /**
     * The name of the event property holding the resource type.
     * @since 2.0.6
     */
    public static final String PROPERTY_RESOURCE_TYPE = "resourceType";

    /**
     * The name of the event property holding the resource super type.
     * @since 2.0.6
     */
    public static final String PROPERTY_RESOURCE_SUPER_TYPE = "resourceSuperType";

    /**
     * The name of the event property holding the changed attribute names
     * of a resource for an {@link #TOPIC_RESOURCE_CHANGED} event.
     * The value of the property is a string array.
     * @since 2.2.0
     */
    public static final String PROPERTY_CHANGED_ATTRIBUTES = "resourceChangedAttributes";

    /**
     * The name of the event property holding the added attribute names
     * of a resource for an {@link #TOPIC_RESOURCE_CHANGED} event.
     * The value of the property is a string array.
     * @since 2.2.0
     */
    public static final String PROPERTY_ADDED_ATTRIBUTES = "resourceAddedAttributes";

    /**
     * The name of the event property holding the removed attribute names
     * of a resource for an {@link #TOPIC_RESOURCE_CHANGED} event.
     * The value of the property is a string array.
     * @since 2.2.0
     */
    public static final String PROPERTY_REMOVED_ATTRIBUTES = "resourceRemovedAttributes";

    /**
     * The topic for the OSGi event which is sent when an adapter factory has been added.
     * The event contains at least the {@link #PROPERTY_ADAPTABLE_CLASSES},
     * and {@link #PROPERTY_ADAPTER_CLASSES} poperties.
     * @since 2.0.6
     */
    public static final String TOPIC_ADAPTER_FACTORY_ADDED = "org/apache/sling/api/adapter/AdapterFactory/ADDED";

    /**
     * The topic for the OSGi event which is sent when an adapter factory has been removed.
     * The event contains at least the {@link #PROPERTY_ADAPTABLE_CLASSES},
     * and {@link #PROPERTY_ADAPTER_CLASSES} poperties.
     * @since 2.0.6
     */
    public static final String TOPIC_ADAPTER_FACTORY_REMOVED = "org/apache/sling/api/adapter/AdapterFactory/REMOVED";

    /**
     * The event property listing the fully qualified names of
     * classes which can be adapted by this adapter factory (value is
     * "adaptables"). The type of the value is a string array.
     * @since 2.0.6
     */
    public static final String PROPERTY_ADAPTABLE_CLASSES = "adaptables";

    /**
     * The event property listing the fully qualified names of
     * classes to which this factory can adapt adaptables (value is "adapters").
     * The type of the value is a string array.
     * @since 2.0.6
     */
    public static final String PROPERTY_ADAPTER_CLASSES = "adapters";

    /**
     * The name of the request attribute providing the name of the currently
     * executing servlet (value is "sling.core.current.servletName"). This
     * attribute is set immediately before calling the
     * <code>Servlet.service()</code> method and reset to any previously
     * stored value after the service method returns.
     * @since 2.1
     */
    public static final String SLING_CURRENT_SERVLET_NAME = "sling.core.current.servletName";
}
