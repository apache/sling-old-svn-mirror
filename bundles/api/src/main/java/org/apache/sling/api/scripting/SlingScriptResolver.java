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
package org.apache.sling.api.scripting;

import org.apache.sling.api.resource.ResourceResolver;

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
 */
public interface SlingScriptResolver {

    /**
     * Finds the given name to a {@link SlingScript}.
     * <p>
     * The semantic meaning of the name is implementation specific: It may be an
     * absolute path to a <code>Resource</code> providing the script source or
     * it may be a relative path resolved according to some path settings.
     * Finally, the name may also just be used as an itentifier to find the
     * script in some registry.
     *
     * @param resourceResolver The <code>ResourceResolver</code> used to
     *            access the script.
     * @param name The script name. Must not be <code>null</code>.
     * @return The {@link SlingScript} to which the name resolved or
     *         <code>null</code> otherwise.
     * @throws org.apache.sling.api.SlingException If an error occurrs trying to resolve the name.
     */
    SlingScript findScript(ResourceResolver resourceResolver, String name);
}
