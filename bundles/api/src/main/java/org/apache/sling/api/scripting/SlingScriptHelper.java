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
package org.apache.sling.api.scripting;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The <code>SlingScriptHelper</code> interface defines the API of a helper
 * class which is provided to the scripts called from sling through the global
 * <code>{@link SlingBindings#SLING sling}</code> variable.
 */
@ProviderType
public interface SlingScriptHelper {

    /**
     * Returns the {@link SlingHttpServletRequest} representing the input of the
     * request.
     * @return The request
     */
    @Nonnull SlingHttpServletRequest getRequest();

    /**
     * Returns the {@link SlingHttpServletResponse} representing the output of
     * the request.
     * @return The response
     */
    @Nonnull SlingHttpServletResponse getResponse();

    /**
     * Returns the {@link SlingScript} being called to handle the request.
     * @return The script
     */
    @Nonnull SlingScript getScript();

    /**
     * Same as {@link #include(String,RequestDispatcherOptions)}, but using
     * empty options.
     *
     * @param path The path to include
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    void include(@Nonnull String path);

    /**
     * Helper method to include the result of processing the request for the
     * given <code>path</code> and <code>requestDispatcherOptions</code>.
     * This method is intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path,
     *     &quot;option:xyz&quot;);
     * if (dispatcher != null) {
     *     dispatcher.include(getRequest(), getResponse());
     * }
     * </pre>
     *
     * <p>
     * This method creates a <code>RequestDispatcherOptions</code> object by
     * calling the
     * {@link RequestDispatcherOptions#RequestDispatcherOptions(String)}
     * constructor.
     *
     * @param path The path to the resource to include.
     * @param requestDispatcherOptions influence the rendering of the included
     *            Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions#RequestDispatcherOptions(String)
     * @see #include(String, RequestDispatcherOptions)
     */
    void include(@Nonnull String path, String requestDispatcherOptions);

    /**
     * Helper method to include the result of processing the request for the
     * given <code>path</code> and <code>options</code>. This method is
     * intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcherOptions opts = new RequestDispatcherOptions();
     * opts.put(&quot;option&quot;, &quot;xyz&quot;);
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path, opts);
     * if (dispatcher != null) {
     *     dispatcher.include(getRequest(), getResponse());
     * }
     * </pre>
     *
     * @param path The path to the resource to include.
     * @param options influence the rendering of the included Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions
     * @see #include(String, String)
     */
    void include(@Nonnull String path, RequestDispatcherOptions options);

    /**
     * Same as {@link #include(Resource,RequestDispatcherOptions)}, but using
     * empty options.
     *
     * @param resource The resource to include
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    void include(@Nonnull Resource resource);

    /**
     * Helper method to include the result of processing the request for the
     * given <code>resource</code> and <code>requestDispatcherOptions</code>.
     * This method is intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(resource,
     *     &quot;option:xyz&quot;);
     * if (dispatcher != null) {
     *     dispatcher.include(getRequest(), getResponse());
     * }
     * </pre>
     *
     * <p>
     * This method creates a <code>RequestDispatcherOptions</code> object by
     * calling the
     * {@link RequestDispatcherOptions#RequestDispatcherOptions(String)}
     * constructor.
     *
     * @param resource The resource to include.
     * @param requestDispatcherOptions influence the rendering of the included
     *            Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions#RequestDispatcherOptions(String)
     * @see #include(String, RequestDispatcherOptions)
     */
    void include(@Nonnull Resource resource, String requestDispatcherOptions);

    /**
     * Helper method to include the result of processing the request for the
     * given <code>resource</code> and <code>options</code>. This method is
     * intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcherOptions opts = new RequestDispatcherOptions();
     * opts.put(&quot;option&quot;, &quot;xyz&quot;);
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(resource, opts);
     * if (dispatcher != null) {
     *     dispatcher.include(getRequest(), getResponse());
     * }
     * </pre>
     *
     * @param resource The resource to include.
     * @param options influence the rendering of the included Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions
     * @see #include(String, String)
     */
    void include(@Nonnull Resource resource, RequestDispatcherOptions options);

    /**
     * Same as {@link #forward(String,RequestDispatcherOptions)}, but using
     * empty options.
     *
     * @param path The path to forward to
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     */
    void forward(@Nonnull String path);

    /**
     * Helper method to forward the request to a Servlet or script for the given
     * <code>path</code> and <code>requestDispatcherOptions</code>. This method
     * is intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path,
     *     &quot;option:xyz&quot;);
     * if (dispatcher != null) {
     *     dispatcher.forward(getRequest(), getResponse());
     * }
     * </pre>
     *
     * <p>
     * This method creates a <code>RequestDispatcherOptions</code> object by
     * calling the
     * {@link RequestDispatcherOptions#RequestDispatcherOptions(String)}
     * constructor.
     *
     * @param path The path to the resource to forward to.
     * @param requestDispatcherOptions influence the rendering of the forwarded
     *            Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     * @see RequestDispatcherOptions#RequestDispatcherOptions(String)
     * @see #forward(String, RequestDispatcherOptions)
     */
    void forward(@Nonnull String path, String requestDispatcherOptions);

    /**
     * Helper method to forward the request to a Servlet or script for the given
     * <code>path</code> and <code>options</code>. This method is intended
     * to be implemented as follows:
     *
     * <pre>
     * RequestDispatcherOptions opts = new RequestDispatcherOptions();
     * opts.put(&quot;option&quot;, &quot;xyz&quot;);
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(path, opts);
     * if (dispatcher != null) {
     *     dispatcher.forward(getRequest(), getResponse());
     * }
     * </pre>
     *
     * @param path The path to the resource to forward the request to.
     * @param options influence the rendering of the forwarded Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     * @throws IllegalStateException If the respoonse has already been committed
     * @see RequestDispatcherOptions
     */
    void forward(@Nonnull String path, RequestDispatcherOptions options);

    /**
     * Same as {@link #forward(Resource,RequestDispatcherOptions)}, but using
     * empty options.
     *
     * @param resource The resource to forward to.
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     */
    void forward(@Nonnull Resource resource);

    /**
     * Helper method to forward the request to a Servlet or script for the given
     * <code>resource</code> and <code>requestDispatcherOptions</code>. This method
     * is intended to be implemented as follows:
     *
     * <pre>
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(resource,
     *     &quot;option:xyz&quot;);
     * if (dispatcher != null) {
     *     dispatcher.forward(getRequest(), getResponse());
     * }
     * </pre>
     *
     * <p>
     * This method creates a <code>RequestDispatcherOptions</code> object by
     * calling the
     * {@link RequestDispatcherOptions#RequestDispatcherOptions(String)}
     * constructor.
     *
     * @param resource The resource to forward to.
     * @param requestDispatcherOptions influence the rendering of the forwarded
     *            Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     * @see RequestDispatcherOptions#RequestDispatcherOptions(String)
     * @see #forward(String, RequestDispatcherOptions)
     */
    void forward(@Nonnull Resource resource, String requestDispatcherOptions);

    /**
     * Helper method to forward the request to a Servlet or script for the given
     * <code>resource</code> and <code>options</code>. This method is intended
     * to be implemented as follows:
     *
     * <pre>
     * RequestDispatcherOptions opts = new RequestDispatcherOptions();
     * opts.put(&quot;option&quot;, &quot;xyz&quot;);
     * RequestDispatcher dispatcher = getRequest().getRequestDispatcher(resource, opts);
     * if (dispatcher != null) {
     *     dispatcher.forward(getRequest(), getResponse());
     * }
     * </pre>
     *
     * @param resource The resource to forward the request to.
     * @param options influence the rendering of the forwarded Resource
     * @throws org.apache.sling.api.SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the forward.
     * @throws org.apache.sling.api.SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the forward.
     * @throws IllegalStateException If the respoonse has already been committed
     * @see RequestDispatcherOptions
     */
    void forward(@Nonnull Resource resource, RequestDispatcherOptions options);

    /**
     * Lookup a single service.
     * <p>
     * If multiple such services exist, the service with the highest ranking (as specified in its Constants.SERVICE_RANKING property) is returned.
     * If there is a tie in ranking, the service with the lowest service ID (as specified in its Constants.SERVICE_ID property); that is, the service that was registered first is returned.
     * </p>
     * <p>This is equal to the semantics from
     * <a href="https://osgi.org/javadoc/r5/core/org/osgi/framework/BundleContext.html#getServiceReference(java.lang.Class)">
     * BundleContext.getServiceReference(Class)</a>.</p>
     * @param serviceType The type (interface) of the service.
     * @param <ServiceType> The type (interface) of the service.
     * @return The service instance, or {@code null} if no services are registered which implement the specified class.
     */
    @CheckForNull <ServiceType> ServiceType getService(@Nonnull Class<ServiceType> serviceType);

    /**
     * Lookup one or several services.
     * <p>
     * The returned array is sorted descending by service ranking (i.e. the service with the highest ranking is returned first).
     * If there is a tie in ranking, the service with the lowest service ID
     * (as specified in its Constants.SERVICE_ID property);
     * that is, the service that was registered first is returned first.
     * </p>
     *
     * @param serviceType The type (interface) of the service.
     * @param <ServiceType> The type (interface) of the service.
     * @param filter An optional filter (LDAP-like, see OSGi spec)
     * @return An array of services objects or {@code null}.
     * @throws InvalidServiceFilterSyntaxException If the <code>filter</code>
     *             string is not a valid OSGi service filter string.
     *
     * @see <a href="https://osgi.org/javadoc/r5/core/org/osgi/framework/Filter.html">Filter class in OSGi</a>
     */
    @CheckForNull <ServiceType> ServiceType[] getServices(@Nonnull Class<ServiceType> serviceType,
            String filter);

    /**
     * Dispose the helper. This method can be used to clean up the script helper
     * after the script is run.
     * @deprecated This method is deprecated since version 2.1 and will be removed.
     *             It should never be called by clients.
     */
    @Deprecated
    void dispose();
}
