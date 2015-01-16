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
package org.apache.sling.auth.openid.impl;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AbstractAuthenticationFormServlet;
import org.apache.sling.auth.openid.OpenIDConstants;
import org.apache.sling.auth.openid.OpenIDFailure;
import org.osgi.framework.Constants;

/**
 * The <code>AuthenticationFormServlet</code> provides the default login form
 * used for OpenID Authentication.
 */
@Component
@Service(value=Servlet.class)
@Properties({
    @Property(name=Constants.SERVICE_VENDOR, value="The Apache Software Foundation"),
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Default Login Form for OpenID Authentication")  
})
@SuppressWarnings("serial")
public class AuthenticationFormServlet extends AbstractAuthenticationFormServlet {

    /**
     * The constant is used to provide the service registration path
     */
    @Property(name="sling.servlet.paths")
    static final String SERVLET_PATH = "/system/sling/openid/login";

    @Property(name="sling.auth.requirements")
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
        final String reason = request.getParameter(OpenIDConstants.OPENID_FAILURE_REASON);
        if (reason != null) {

            try {

                OpenIDFailure failure = OpenIDFailure.valueOf(reason);

                // in case of missing repository user association, tell that
                // the identity is missing
                if (failure == OpenIDFailure.REPOSITORY) {
                    Object idObject = request.getParameter(OpenIDConstants.OPENID_IDENTITY);
                    if (idObject instanceof String) {
                        return "Cannot associate Repository User with OpenID identifier "
                            + idObject;
                    }
                }

                // else (or if there is no identity) use the failure type message
                return failure.toString();

            } catch (IllegalArgumentException iae) {
                // thrown if the reason is not an expected value, assume none
            }

            // raw reason string if not a failure value
            return reason;
        }

        return "";
    }
}
