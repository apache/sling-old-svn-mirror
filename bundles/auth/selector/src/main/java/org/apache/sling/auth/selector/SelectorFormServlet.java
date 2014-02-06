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
package org.apache.sling.auth.selector;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AbstractAuthenticationFormServlet;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.form.FormReason;
import org.apache.sling.auth.openid.OpenIDConstants;
import org.apache.sling.auth.openid.OpenIDFailure;

/**
 * The <code>SelectorFormServlet</code> provides the default login form used for
 * Form Based or OpenID Authentication.
 */
@Component(metatype = false)
@Properties({
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = "service.description", value = "Default Login Form for the Authenticator Selector") })
@Service(value = Servlet.class)
@SuppressWarnings("serial")
public class SelectorFormServlet extends AbstractAuthenticationFormServlet {

    /**
     * The constant is used to provide the service registration path
     */
    @Property(name = "sling.servlet.paths")
    static final String SERVLET_PATH = "/system/sling/selector/login";

    /**
     * This constant is used to provide the service registration property
     * indicating to pass requests to this servlet unauthenticated.
     */
    @Property(name = "sling.auth.requirements", propertyPrivate = true)
    @SuppressWarnings("unused")
    private static final String[] AUTH_REQUIREMENT = { "-" + SERVLET_PATH,
        "-/sling.css", "-/sling-logo.png", "-/signup.html" };

    @Override
    protected String getForm(final HttpServletRequest request)
            throws IOException {
        String form = super.getForm(request);
        form = form.replace("${selectedAuthType}", getSelectedAuthType(request));
        return form;
    }

    private String getSelectedAuthType(final HttpServletRequest request) {
        String type = request.getParameter(SelectorAuthenticationHandler.PAR_SELECTED_AUTH_TYPE);
        if (type == null || type.length() == 0) {
            return "null";
        } else {
            type = StringEscapeUtils.escapeJavaScript(type);
        }
        return "\"" + type + "\"";
    }

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

        // 1. Check whether there is a reason from the Form Based Authentication
        // Handler
        Object formResObj = request.getAttribute(AuthenticationHandler.FAILURE_REASON);
        if (formResObj instanceof Enum<?>) {
            return formResObj.toString();
        }

        final String jReason = request.getParameter(AuthenticationHandler.FAILURE_REASON);
        if (jReason != null) {
            try {
                return FormReason.valueOf(jReason).toString();
            } catch (IllegalArgumentException iae) {
                // thrown if the reason is not an expected value, assume none
            }

            try {
                OpenIDFailure failure = OpenIDFailure.valueOf(jReason);

                // in case of missing repository user association, tell that
                // the identity is missing
                if (failure == OpenIDFailure.REPOSITORY) {
                    Object idObject = request.getParameter(OpenIDConstants.OPENID_IDENTITY);
                    if (idObject instanceof String) {
                        return "Cannot associate Repository User with OpenID identifier "
                            + idObject;
                    }
                }

                return failure.toString();

            } catch (IllegalArgumentException iae) {
                // thrown if the reason is not an expected value, assume none
            }

            // no valid FormReason/OpenIDFailure value, use raw value
            return jReason;
        }

        return "";
    }
}
