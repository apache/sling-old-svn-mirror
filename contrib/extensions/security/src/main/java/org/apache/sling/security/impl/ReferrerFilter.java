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
package org.apache.sling.security.impl;

import java.io.IOException;

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

@SlingFilter(order=-1500000000,scope=SlingFilterScope.REQUEST,metatype=true,
        description="%referrer.description",
        label="%referrer.name")
public class ReferrerFilter implements Filter {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Default value for allow empty. */
    private static final boolean DEFAULT_ALLOW_EMPTY = true;

    /** Allow empty property. */
    @Property(boolValue=DEFAULT_ALLOW_EMPTY)
    private static final String PROP_ALLOW_EMPTY = "allow.empty";

    /** Default value for allow localhost. */
    private static final boolean DEFAULT_ALLOW_LOCALHOST = true;

    /** Allow localhost property. */
    @Property(boolValue=DEFAULT_ALLOW_LOCALHOST)
    private static final String PROP_ALLOW_LOCALHOST = "allow.localhost";

    /** Allow empty property. */
    @Property(unbounded=PropertyUnbounded.ARRAY)
    private static final String PROP_HOSTS = "allow.hosts";

    /** Do we allow empty referrer? */
    private boolean allowEmpty;

    /** Do we allow localhost referrer? */
    private boolean allowLocalhost;

    /** Allowed hosts */
    private String[] allowHosts;

    /**
     * Activate
     */
    protected void activate(final ComponentContext ctx) {
        this.allowEmpty = OsgiUtil.toBoolean(ctx.getProperties().get(PROP_ALLOW_EMPTY), DEFAULT_ALLOW_EMPTY);
        this.allowHosts = OsgiUtil.toStringArray(ctx.getProperties().get(PROP_HOSTS));
        this.allowLocalhost = OsgiUtil.toBoolean(ctx.getProperties().get(PROP_ALLOW_LOCALHOST), DEFAULT_ALLOW_LOCALHOST);
        if ( this.allowHosts != null ) {
            if ( this.allowHosts.length == 0 ) {
                this.allowHosts = null;
            } else if ( this.allowHosts.length == 1 && this.allowHosts[0].trim().length() == 0 ) {
                this.allowHosts = null;
            }
        }
    }

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

    String getHost(final String referrer) {
        final int startPos = referrer.indexOf("://") + 3;
        if ( startPos == 2 ) {
            // we consider this illegal
            return null;
        }
        final int paramStart = referrer.indexOf('?');
        final String hostAndPath = (paramStart == -1 ? referrer : referrer.substring(0, paramStart));
        final int endPos = hostAndPath.indexOf('/', startPos);
        final String hostPart = (endPos == -1 ? hostAndPath.substring(startPos) : hostAndPath.substring(startPos, endPos));
        final int hostNameStart = hostPart.indexOf('@') + 1;
        final int hostNameEnd = hostPart.lastIndexOf(':');
        if (hostNameEnd < hostNameStart ) {
            return hostPart.substring(hostNameStart);
        }
        return hostPart.substring(hostNameStart, hostNameEnd);
    }

    boolean isValidRequest(final HttpServletRequest request) {
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

        final String host = getHost(referrer);
        if ( host == null ) {
            // if this is invalid we just return invalid
            this.logger.info("Rejected illegal referrer header for {} request to {} : {}",
                    new Object[] {request.getMethod(), request.getRequestURI(), referrer});
            return false;
        }
        final boolean valid;
        boolean isValidLocalHost = false;
        if ( this.allowLocalhost ) {
            if ( "localhost".equals(host) || "127.0.0.1".equals(host) ) {
                isValidLocalHost = true;
            }
        }
        if ( isValidLocalHost ) {
            valid = true;
        } else if ( this.allowHosts == null ) {
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
