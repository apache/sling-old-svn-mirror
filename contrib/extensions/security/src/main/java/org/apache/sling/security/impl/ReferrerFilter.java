/*
 * Copyright 1997-2011 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.security.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingFilter(order=-100000,scope=SlingFilterScope.REQUEST,metatype=true,
        description="%referrer.description",
        label="%referrer.name")
public class ReferrerFilter implements Filter {

    private static final boolean DEFAULT_ALLOW_EMPTY = true;

    @Property(boolValue=DEFAULT_ALLOW_EMPTY)
    private static final String PROP_ALLOW_EMPTY = "allow.empty";

    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_HOSTS = "allow.hosts";

    private boolean allowEmpty;

    private String[] allowHosts;

    /**
     * Activate
     */
    protected void activate(final ComponentContext ctx) {
        this.allowEmpty = OsgiUtil.toBoolean(ctx.getProperties().get(PROP_ALLOW_EMPTY), DEFAULT_ALLOW_EMPTY);
        this.allowHosts = OsgiUtil.toStringArray(ctx.getProperties().get(PROP_HOSTS));
        if ( this.allowHosts != null ) {
            if ( this.allowHosts.length == 0 ) {
                this.allowHosts = null;
            } else if ( this.allowHosts.length == 1 && this.allowHosts[0].trim().length() == 0 ) {
                this.allowHosts = null;
            }
        }
    }

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean isModification(final HttpServletRequest req) {
        final String method = req.getMethod();
        if ("POST".equals(method)) {
            return true;
        } else if ("PUT".equals(method)) {
            return true;
        } else if ("DELETE".equals(method)) {
            return true;
        }
        return false;
    }

    public void doFilter(final ServletRequest req,
                         final ServletResponse res,
                         final FilterChain chain)
    throws IOException, ServletException {
        if ( req instanceof HttpServletRequest && res instanceof HttpServletResponse ) {
            final HttpServletRequest request = (HttpServletRequest)req;

            // is this a modification request
            if ( this.isModification(request) ) {
                if ( !this.isValidRequest(request) ) {
                    final HttpServletResponse response = (HttpServletResponse)res;
                    // we use 500
                    response.sendError(500);
                    return;
                }
            }
        }
        chain.doFilter(req, res);
    }

    private boolean isValidRequest(final HttpServletRequest request) {
        final String referrer = request.getHeader("referer");
        // check for missing/empty referrer
        if ( referrer == null || referrer.trim().length() == 0 ) {
            if ( !this.allowEmpty ) {
                this.logger.info("Rejected empty referrer header for {} request to {}", request.getMethod(), request.getRequestURI());
            }
            return this.allowEmpty;
        }
        // check for relative referrer - which is always allowed
        if ( referrer.indexOf(":/") == - 1 ) {
            return true;
        }
        final URI uri;
        try {
            uri = new URI(referrer);
        } catch (URISyntaxException e) {
            // if this is invalid we just return invalid
            this.logger.info("Rejected illegal referrer header for {} request to {} : {}",
                    new Object[] {request.getMethod(), request.getRequestURI(), referrer});
            return false;
        }
        final String host = uri.getHost();
        final boolean valid;
        if ( this.allowHosts == null ) {
            valid = host.equals(request.getServerName());
        } else {
            boolean flag = false;
            for(final String allowHost : this.allowHosts) {
                if ( host.equals(allowHost) ) {
                    flag = true;
                    break;
                }
            }
            valid = flag;
        }
        if ( !valid) {
            this.logger.info("Rejected referrer header for {} request to {} : {}",
                    new Object[] {request.getMethod(), request.getRequestURI(), referrer});
        }
        return valid;
    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig arg0) throws ServletException {
        // nothing to do
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // nothing to do
    }
}
