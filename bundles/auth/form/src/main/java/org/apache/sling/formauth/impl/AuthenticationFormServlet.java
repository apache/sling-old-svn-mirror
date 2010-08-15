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
package org.apache.sling.formauth.impl;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.commons.auth.spi.AbstractAuthenticationFormServlet;
import org.apache.sling.formauth.FormReason;

/**
 * The <code>AuthenticationFormServlet</code> provides the default login form
 * used for Form Based Authentication.
 *
 * @scr.component metatype="no"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *               value="Default Login Form for Form Based Authentication"
 * @scr.service interface="javax.servlet.Servlet"
 */
@SuppressWarnings("serial")
public class AuthenticationFormServlet extends AbstractAuthenticationFormServlet {

    /**
     * The constant is used to provide the service registration path
     *
     * @scr.property name="sling.servlet.paths"
     */
    static final String SERVLET_PATH = "/system/sling/form/login";

    /**
     * This constant is used to provide the service registration property
     * indicating to pass requests to this servlet unauthenticated.
     *
     * @scr.property name="sling.auth.requirements"
     */
    @SuppressWarnings("unused")
    private static final String AUTH_REQUIREMENT = "-" + SERVLET_PATH;

    /**
     * Returns an informational message according to the value provided in the
     * <code>j_reason</code> request parameter. Supported reasons are invalid
     * credentials and session timeout.
     *
     * @param request The request providing the parameter
     * @return The "translated" reason to render the login form or an empty
     *         string if there is no specific reason
     */
    protected String getReason(final HttpServletRequest request) {
        // return the resource attribute if set to a non-empty string
        Object resObj = request.getAttribute(FormAuthenticationHandler.PAR_J_REASON);
        if (resObj instanceof FormReason) {
            return ((FormReason) resObj).toString();
        }

        final String reason = request.getParameter(FormAuthenticationHandler.PAR_J_REASON);
        if (reason != null) {
            try {
                return FormReason.valueOf(reason).toString();
            } catch (IllegalArgumentException iae) {
                // thrown if the reason is not an expected value, assume none
            }

            // no valid FormReason value, use raw value
            return reason;
        }

        return "";
    }
}
