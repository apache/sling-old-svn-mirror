package org.apache.sling.jmx.jolokia.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.osgi.service.http.HttpContext;

public class AuthHttpContext implements HttpContext {

    /**
     * Handles security
     * 
     * @see #handleSecurity(HttpServletRequest, HttpServletResponse)
     */
    private final AuthenticationSupport authenticator;

    /**
     * The set of userIds which will be allowed access to Jolokia.
     */
    private Collection<String> allowedUserIds;

    public AuthHttpContext(final AuthenticationSupport authSupport, final Collection<String> allowedUserIds) {
        this.authenticator = authSupport;
        this.allowedUserIds = allowedUserIds;
    }

    public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final AuthenticationSupport localAuthenticator = this.authenticator;
        if (localAuthenticator != null) {
            final boolean authResult = localAuthenticator.handleSecurity(request, response);
            if (authResult) {
                if (this.allowedUserIds.size() == 0) {
                    return true;
                } else {
                    final String userId = (String) request.getAttribute(HttpContext.REMOTE_USER);
                    return this.allowedUserIds.contains(userId);
                }
            }
            return false;
        }
        // send 503/SERVICE UNAVAILABLE, flush to ensure delivery
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.flushBuffer();

        // terminate this request now
        return false;
    }

    public URL getResource(String name) {
        return null;
    }

    public String getMimeType(String name) {
        return null;
    }

}
