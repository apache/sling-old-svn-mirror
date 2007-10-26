/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting;

import org.apache.sling.SlingHttpServletRequest;
import org.apache.sling.exceptions.SlingException;

/**
 * The <code>ScriptResolver</code> interface defines the API for a service
 * capable of locating scripts. Where the script is actually located is an
 * implementation detail of the service implementation.
 * <p>
 * Find scripts in the repository, based on the current Resource type. The
 * script filename is built using the current HTTP request method name, followed
 * by the extension of the current request and the desired script extension. For
 * example, a "js" script for a GET request on a Resource of type some/type with
 * request extension "html" should be stored as
 *
 * <pre>
 *      /sling/scripts/some/type/get.html.js
 * </pre>
 *
 * in the repository.
 * <p>
 * Applications of the Sling Framework generally do not need the servlet
 * resolver as resolution of the servlets to process requests and sub-requests
 * through a <code>RequestDispatcher</code> is handled by the Sling Framework.
 * <p>
 * The script resolver service is available from the
 * {@link org.apache.sling.helpers.ServiceLocator} using the fully qualified
 * name of this interface as its name. This name is provided as the
 * {@link #NAME} field for convenience.
 */
public interface SlingScriptResolver {

    /**
     * The service name which may be used to ask the
     * {@link org.apache.sling.helpers.ServiceLocator} for the resource resolver
     * (value is the fully qualified name of this interface class).
     */
    static final String NAME = SlingScriptResolver.class.getName();

    /**
     * Resolves a {@link SlingScript} to handle the given request.
     *
     * @param request The {@link SlingHttpServletRequest} for which a
     *            {@link SlingScript} is to be found.
     * @return The {@link SlingScript} or <code>null</code> if no script can
     *         be found to handle the request.
     * @throws SlingException If an error occurrs trying to find a script.
     */
    SlingScript resolveScript(SlingHttpServletRequest request)
            throws SlingException;

}
