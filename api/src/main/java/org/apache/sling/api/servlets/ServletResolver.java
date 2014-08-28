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

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ServletResolver</code> defines the API for a service capable of
 * resolving <code>javax.servlet.Servlet</code> instances to handle the
 * processing of a request or resource.
 * <p>
 * Applications of the Sling Framework generally do not need the servlet
 * resolver as resolution of the servlets to process requests and sub-requests
 * through a <code>RequestDispatcher</code> is handled by the Sling Framework.
 * <p>
 */
@ProviderType
public interface ServletResolver {

    /**
     * Resolves a <code>javax.servlet.Servlet</code> whose
     * <code>service</code> method may be used to handle the given
     * <code>request</code>.
     * <p>
     * The returned servlet must be assumed to be initialized and ready to run.
     * That is, the <code>init</code> nor the <code>destroy</code> methods
     * must <em>NOT</em> be called on the returned servlet.
     * <p>
     * This method must not return a <code>Servlet</code> instance
     * implementing the {@link OptingServlet} interface and returning
     * <code>false</code> when the
     * {@link OptingServlet#accepts(SlingHttpServletRequest)} method is called.
     *
     * @param request The {@link SlingHttpServletRequest} object used to drive
     *            selection of the servlet.
     * @return The servlet whose <code>service</code> method may be called to
     *         handle the request.
     * @throws org.apache.sling.api.SlingException Is thrown if an error occurrs
     *             while trying to find an appropriate servlet to handle the
     *             request or if no servlet could be resolved to handle the
     *             request.
     */
    Servlet resolveServlet(SlingHttpServletRequest request);

    /**
     * Resolves a <code>javax.servlet.Servlet</code> whose
     * <code>service</code> method may be used to handle a request.
     * <p>
     * The returned servlet must be assumed to be initialized and ready to run.
     * That is, the <code>init</code> nor the <code>destroy</code> methods
     * must <em>NOT</em> be called on the returned servlet.
     * <p>
     * This method skips all {@link OptingServlet}s as there is no
     * request object available.
     *
     * Basically this method searches a script with the <code>scriptName</code>
     * for the resource type defined by the <code>resource</code>
     * @param resource The {@link Resource} object used to drive
     *            selection of the servlet.
     * @param scriptName The name of the script - the script might have an
     *                   extension. In this case only a script with the
     *                   matching extension is used.
     * @return The servlet whose <code>service</code> method may be called to
     *         handle the request.
     * @throws org.apache.sling.api.SlingException Is thrown if an error occurrs
     *             while trying to find an appropriate servlet to handle the
     *             request or if no servlet could be resolved to handle the
     *             request.
     * @since 2.1
     */
    Servlet resolveServlet(Resource resource, String scriptName);

    /**
     * Resolves a <code>javax.servlet.Servlet</code> whose
     * <code>service</code> method may be used to handle a request.
     * <p>
     * The returned servlet must be assumed to be initialized and ready to run.
     * That is, the <code>init</code> nor the <code>destroy</code> methods
     * must <em>NOT</em> be called on the returned servlet.
     * <p>
     * This method skips all {@link OptingServlet}s as there is no
     * request object available.
     *
     * Basically this method searches a script with the <code>scriptName</code>
     * @param resolver The {@link ResourceResolver} object used to drive
     *            selection of the servlet.
     * @param scriptName The name of the script - the script might have an
     *                   extension. In this case only a script with the
     *                   matching extension is used.
     * @return The servlet whose <code>service</code> method may be called to
     *         handle the request.
     * @throws org.apache.sling.api.SlingException Is thrown if an error occurrs
     *             while trying to find an appropriate servlet to handle the
     *             request or if no servlet could be resolved to handle the
     *             request.
     * @since 2.1
     */
    Servlet resolveServlet(ResourceResolver resolver, String scriptName);

}
