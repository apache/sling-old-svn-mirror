/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.davex.impl.servlets;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.webdav.util.CSRFUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DavEx WebDav servlet which acquires a Repository instance via the OSGi
 * service registry.
 *
 */
@SuppressWarnings("serial")
@Component(metatype = true, label = "%dav.name", description = "%dav.description")
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling JcrRemoting Servlet"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation") })
public class SlingDavExServlet extends JcrRemotingServlet {

    /**
     * Default value for the DavEx servlet registration.
     */
    private static final String DEFAULT_DAV_ROOT = "/server";

    /**
     * Name of the property to configure the location for the DavEx servlet
     * registration. Default for the property is {@link #DEFAULT_DAV_ROOT}.
     */
    @Property(value=DEFAULT_DAV_ROOT)
    private static final String PROP_DAV_ROOT = "alias";

    private static final boolean DEFAULT_CREATE_ABSOLUTE_URI = true;

    /**
     * Name of the property to configure whether absolute URIs ({@code true}) or
     * absolute paths ({@code false}) are generated in responses. Default for
     * the property is {@link #DEFAULT_CREATE_ABSOLUTE_URI}.
     */
    @Property(boolValue=DEFAULT_CREATE_ABSOLUTE_URI)
    private static final String PROP_CREATE_ABSOLUTE_URI = "dav.create-absolute-uri";

    /**
     * The name of the service property of the registered dummy service to cause
     * the path to the DavEx servlet to not be subject to forced authentication.
     */
    private static final String PAR_AUTH_REQ = "sling.auth.requirements";

    private static char[] EMPTY_PW = new char[0];

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private SlingRepository repository;

    @Reference
    private HttpService httpService;

    @Reference
    private AuthenticationSupport authSupport;

    /**
     * The path at which the DavEx servlet has successfully been
     * registered in the {@link #activate(Map)} method. If this is
     * <code>null</code> the DavEx servlet is not registered with the
     * Http Service.
     */
    private String servletAlias;

    /**
     * The dummy service registration to convey to the Sling Authenticator
     * that everything under the alias must not be forcibly authenticated.
     * This will be <code>null</code> if the DavEx servlet registration
     * fails.
     */
    private ServiceRegistration dummyService;

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, ?> config) {
        final String davRoot = OsgiUtil.toString(config.get(PROP_DAV_ROOT), DEFAULT_DAV_ROOT);
        final boolean createAbsoluteUri = OsgiUtil.toBoolean(config.get(PROP_CREATE_ABSOLUTE_URI), DEFAULT_CREATE_ABSOLUTE_URI);

        final AuthHttpContext context = new AuthHttpContext(davRoot);
        context.setAuthenticationSupport(authSupport);

        // prepare DavEx servlet config
        final Dictionary<String, String> initProps = new Hashtable<String, String>();

        // prefix to the servlet
        initProps.put(INIT_PARAM_RESOURCE_PATH_PREFIX, davRoot);

        // create absolute URIs (or absolute paths)
        initProps.put(INIT_PARAM_CREATE_ABSOLUTE_URI, Boolean.toString(createAbsoluteUri));

        // disable CSRF checks for now (should be handled by Sling)
        initProps.put(INIT_PARAM_CSRF_PROTECTION, CSRFUtil.DISABLED);

        // register and handle registration failure
        try {
            this.httpService.registerServlet(davRoot, this, initProps, context);
            this.servletAlias = davRoot;

            java.util.Properties dummyServiceProperties = new java.util.Properties();
            dummyServiceProperties.put(Constants.SERVICE_VENDOR, config.get(Constants.SERVICE_VENDOR));
            dummyServiceProperties.put(Constants.SERVICE_DESCRIPTION,
                "Helper for " + config.get(Constants.SERVICE_DESCRIPTION));
            dummyServiceProperties.put(PAR_AUTH_REQ, "-" + davRoot);
            this.dummyService = bundleContext.registerService("java.lang.Object", new Object(), dummyServiceProperties);
        } catch (Exception e) {
            log.error("activate: Failed registering DavEx Servlet at " + davRoot, e);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.dummyService != null) {
            this.dummyService.unregister();
            this.dummyService = null;
        }

        if (this.servletAlias != null) {
            this.httpService.unregister(servletAlias);
            this.servletAlias = null;
        }
    }

    @Override
    protected Repository getRepository() {
        return repository;
    }

    @Override
    protected SessionProvider getSessionProvider() {
        return new SessionProvider() {

            public Session getSession(final HttpServletRequest req, final Repository repository, final String workspace)
                    throws LoginException, RepositoryException, ServletException {
                final ResourceResolver resolver = (ResourceResolver) req.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
                if (resolver != null) {
                    final Session session = resolver.adaptTo(Session.class);
                    if (session != null) {
                        final Session newSession = getLongLivedSession(session);
                        log.debug("getSession: Creating new Session ({}) for {}", newSession, newSession.getUserID());
                        return newSession;
                    }
                }

                throw new ServletException("ResourceResolver missing or not providing on JCR Session");
            }

            public void releaseSession(final Session session) {
                log.debug("releaseSession: Logging out long lived Session ({})", session);
                session.logout();
            }

            /**
             * Creates a new session for the user of the slingSession in the
             * same workspace as the slingSession.
             * <p>
             * Assumption: The admin session has permission to impersonate
             * as any user without restriction. If this is not the case
             * the Session.impersonate method throws a LoginException
             * which is folded into a RepositoryException.
             *
             * @param slingSession The session provided by the Sling
             *            authentication mechanis,
             * @return a new session which may (and will) outlast the request
             * @throws RepositoryException If an error occurrs creating the
             *             session.
             */
            private Session getLongLivedSession(final Session slingSession) throws RepositoryException {
                Session adminSession = null;
                final String user = slingSession.getUserID();
                try {
                    final SimpleCredentials credentials = new SimpleCredentials(user, EMPTY_PW);
                    final String wsp = slingSession.getWorkspace().getName();
                    adminSession = SlingDavExServlet.this.repository.loginAdministrative(wsp);
                    return adminSession.impersonate(credentials);
                } catch (RepositoryException re) {

                    // LoginException from impersonate (missing permission)
                    // and RepositoryException from loginAdministrative and
                    // impersonate folded into RepositoryException to
                    // cause a 403/FORBIDDEN response
                    throw new RepositoryException("Cannot get session for " + user, re);

                } finally {
                    if (adminSession != null) {
                        adminSession.logout();
                    }
                }
            }
        };
    }
}
