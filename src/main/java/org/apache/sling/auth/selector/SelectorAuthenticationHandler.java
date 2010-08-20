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
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.auth.core.spi.AbstractAuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.openid.OpenIDConstants;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SelectorAuthenticationHandler</code> is a pseudo authentication
 * handler which only implements the
 * {@link #requestCredentials(HttpServletRequest, HttpServletResponse)} method
 * to cause an input form to be rendered for the client to be able to provide
 * credentials.
 * <p>
 * The default form provided by this handler allows the client to choose from
 * Form Based and OpenID authentication.
 */
@Component(label = "%auth.selector.name", description = "%auth.selector.description", name = "org.apache.sling.auth.selector.SelectorAuthenticationHandler")
@Properties( {
    @Property(name = "service.description", value = "Apache Sling Selector Form Handler"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = org.apache.sling.auth.core.spi.AuthenticationHandler.PATH_PROPERTY, value = "/"),
    @Property(name = "service.ranking", intValue = 5000, propertyPrivate = false) })
@Service
public class SelectorAuthenticationHandler extends
        AbstractAuthenticationHandler {

    /**
     * The name of the request parameter indicating to the login form why the
     * form is being rendered. If this parameter is not set the form is called
     * for the first time and the implied reason is that the authenticator just
     * requests credentials. Otherwise the parameter is set to a
     * {@link FormReason} value.
     */
    static final String PAR_J_REASON = "j_reason";

    /**
     * Request parameter indicating which authentication type was selected by
     * the user. This is used to present the appropriate form if login fails.
     */
    static final String PAR_SELECTED_AUTH_TYPE = "selectedAuthType";

    /**
     * The name of the parameter providing the login form URL.
     */
    @Property(value = SelectorFormServlet.SERVLET_PATH)
    private static final String PAR_LOGIN_FORM = "form.login.form";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String loginForm;

    /**
     * Always returns <code>null</code> because this handler cannot provide
     * credentials from the request.
     */
    public AuthenticationInfo extractCredentials(HttpServletRequest request,
            HttpServletResponse response) {
        return null;
    }

    public boolean requestCredentials(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String resource = getLoginResource(request, null);
        if (resource == null) {
            resource = request.getContextPath() + request.getPathInfo();
            request.setAttribute(Authenticator.LOGIN_RESOURCE, resource);
        }

        // prepare the login form redirection target
        final StringBuilder targetBuilder = new StringBuilder();
        targetBuilder.append(request.getContextPath());
        targetBuilder.append(loginForm);

        // append originally requested resource (for redirect after login)
        char parSep = '?';

        if (resource != null) {
            targetBuilder.append(parSep).append(Authenticator.LOGIN_RESOURCE);
            targetBuilder.append("=").append(
                URLEncoder.encode(resource, "UTF-8"));
            parSep = '&';
        }

        // append indication of previous login failure
        if (request.getAttribute(PAR_J_REASON) != null) {
            final Object jReason = request.getAttribute(PAR_J_REASON);
            @SuppressWarnings("unchecked")
            final String reason = (jReason instanceof Enum)
                    ? ((Enum) jReason).name()
                    : jReason.toString();
            targetBuilder.append(parSep).append(PAR_J_REASON);
            targetBuilder.append("=").append(URLEncoder.encode(reason, "UTF-8"));
            parSep = '&';
        } else if (request.getAttribute(OpenIDConstants.OPENID_FAILURE_REASON) != null) {
            final Object jReason = request.getAttribute(OpenIDConstants.OPENID_FAILURE_REASON);
            @SuppressWarnings("unchecked")
            final String reason = (jReason instanceof Enum)
                    ? ((Enum) jReason).name()
                    : jReason.toString();
            targetBuilder.append(parSep).append(PAR_J_REASON);
            targetBuilder.append("=").append(URLEncoder.encode(reason, "UTF-8"));
            parSep = '&';
        }

        // append selected authentication type of previous request
        if (request.getParameter(PAR_SELECTED_AUTH_TYPE) != null) {
            targetBuilder.append(parSep).append(PAR_SELECTED_AUTH_TYPE);
            targetBuilder.append("=").append(
                request.getParameter(PAR_SELECTED_AUTH_TYPE));
            parSep = '&';
        }

        // finally redirect to the login form
        final String target = targetBuilder.toString();
        try {
            response.sendRedirect(target);
        } catch (IOException e) {
            log.error("Failed to redirect to the page: " + target, e);
        }

        return true;
    }

    /**
     * Nothing to do, this handler does not manage concrete credentials
     */
    public void dropCredentials(HttpServletRequest request,
            HttpServletResponse response) {
    }

    // ---------- SCR integration

    @SuppressWarnings("unused")
    @Activate
    @Modified
    private void configure(Map<String, ?> properties) {
        this.loginForm = OsgiUtil.toString(properties.get(PAR_LOGIN_FORM),
            SelectorFormServlet.SERVLET_PATH);
        log.info("Login Form URL {}", loginForm);
    }
}
