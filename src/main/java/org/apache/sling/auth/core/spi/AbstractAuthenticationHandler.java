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
package org.apache.sling.auth.core.spi;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.api.auth.Authenticator;

/**
 * The <code>AbstractAuthenticationHandler</code> implements the
 * <code>AuthenticationHandler</code> interface and extends the
 * {@link DefaultAuthenticationFeedbackHandler} providing some helper methods
 * which may be used by authentication handlers.
 */
public abstract class AbstractAuthenticationHandler extends
        DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    /**
     * Returns the value of the named request attribute or parameter as a string
     * as follows:
     * <ol>
     * <li>If there is a request attribute of that name, which is a non-empty
     * string, it is returned.</li>If there is a non-empty request parameter of
     * that name, this parameter is returned.
     * <li>Otherwise the <code>defaultValue</code> is returned.
     *
     * @param request The request from which to return the attribute or request
     *            parameter
     * @param name The name of the attribute/parameter
     * @param defaultValue The default value to use if neither a non-empty
     *            string attribute or a non-empty parameter exists in the
     *            request.
     * @return The attribute, parameter or <code>defaultValue</code> as defined
     *         above.
     */
    public static String getAttributeOrParameter(
            final HttpServletRequest request, final String name,
            final String defaultValue) {

        Object resObj = request.getAttribute(name);
        if ((resObj instanceof String) && ((String) resObj).length() > 0) {
            return (String) resObj;
        }

        final String resource = request.getParameter(name);
        if (resource != null && resource.length() > 0) {
            return resource;
        }

        return defaultValue;
    }

    /**
     * Returns any resource target to redirect to after successful
     * authentication. This method either returns a non-empty string or the
     * <code>defaultLoginResource</code> parameter. First the
     * <code>resource</code> request attribute is checked. If it is a non-empty
     * string, it is returned. Second the <code>resource</code> request
     * parameter is checked and returned if it is a non-empty string.
     *
     * @param request The request providing the attribute or parameter
     * @param defaultLoginResource The default login resource value
     * @return The non-empty redirection target or
     *         <code>defaultLoginResource</code>.
     */
    public static String getLoginResource(final HttpServletRequest request,
            String defaultLoginResource) {
        return getAttributeOrParameter(request, Authenticator.LOGIN_RESOURCE,
            defaultLoginResource);
    }
}
