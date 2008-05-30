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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.SlingServletException;
import org.apache.sling.api.request.RequestDispatcherOptions;

/**
 * The <code>SlingScriptHelper</code> interface defines the API of a helper
 * class which is provided to the scripts called from sling through the global
 * <code>{@link SlingBindings#SLING sling}</code> variable.
 */
public interface SlingScriptHelper {

    /**
     * Returns the {@link SlingHttpServletRequest} representing the input of the
     * request.
     */
    SlingHttpServletRequest getRequest();

    /**
     * Returns the {@link SlingHttpServletResponse} representing the output of
     * the request.
     */
    SlingHttpServletResponse getResponse();

    /**
     * Returns the {@link SlingScript} being called to handle the request. This
     * is the same instance as given to the {@link javax.script.ScriptEngine}
     * for evaluation.
     */
    SlingScript getScript();

    /**
     * Same as {@link #include(String,RequestDispatcherOptions)}, but using
     * empty options.
     *
     * @throws SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     */
    void include(String path);

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
     * @throws SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions#RequestDispatcherOptions(String)
     * @see #include(String, RequestDispatcherOptions)
     */
    void include(String path, String requestDispatcherOptions);

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
     * @throws SlingIOException Wrapping a <code>IOException</code> thrown
     *             while handling the include.
     * @throws SlingServletException Wrapping a <code>ServletException</code>
     *             thrown while handling the include.
     * @see RequestDispatcherOptions
     * @see #include(String, String)
     */
    void include(String path, RequestDispatcherOptions options);

    /**
     * Lookup a single service
     *
     * @param serviceType The type (interface) of the service.
     * @return The service instance, or null if the service is not available.
     */
    <ServiceType> ServiceType getService(Class<ServiceType> serviceType);

    /**
     * Lookup one or several services
     *
     * @param serviceType The type (interface) of the service.
     * @param filter An optional filter (LDAP-like, see OSGi spec)
     * @return The services object or null.
     * @throws InvalidServiceFilterSyntaxException If the <code>filter</code>
     *             string is not a valid OSGi service filter string.
     */
    <ServiceType> ServiceType[] getServices(Class<ServiceType> serviceType,
            String filter);

    /**
     * Dispose the helper.
     * This method can be used to clean up the script helper after the script is run.
     */
    void dispose();
}
