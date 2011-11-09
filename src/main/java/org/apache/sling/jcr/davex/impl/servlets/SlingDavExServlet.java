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
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.http.HttpService;
import org.slf4j.LoggerFactory;

/**
 * DavEx WebDav servlet which acquires a Repository instance via the OSGi
 * service registry.
 *
 */
@SuppressWarnings("serial")
@Component(metatype = true, label = "%dav.name", description = "%dav.description")
@Properties({ @Property(name = "service.description", value = "Sling JcrRemoting Servlet"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation") })
public class SlingDavExServlet extends JcrRemotingServlet {

    /**
     * Default value for the DavEx servlet registration.
     */
    private static final String DEFAULT_ALIAS = "/server";

    /**
     * Name of the property to configure the location for the DavEx servlet
     * registration. Default for the property is {@link #DEFAULT_ALIAS}.
     */
    @Property(value=DEFAULT_ALIAS)
    private static final String PROP_ALIAS = "alias";

    @Reference
    private Repository repository;

    @Reference
    private HttpService httpService;

    @Reference
    private AuthenticationSupport authSupport;

    @Reference
    private SlingSettingsService slingSettings;

    /**
     * The path at which the DavEx servlet has successfully been
     * registered in the {@link #activate(Map)} method. If this is
     * <code>null</code> the DavEx servlet is not registered with the
     * Http Service.
     */
    private String servletAlias;

    @Activate
    protected void activate(final Map<String, ?> config) {
        final AuthHttpContext context = new AuthHttpContext();
        context.setAuthenticationSupport(authSupport);

        final String alias = OsgiUtil.toString(config.get(PROP_ALIAS), DEFAULT_ALIAS);

        // prepare DavEx servlet config
        final Dictionary<String, String> initProps = new Hashtable<String, String>();

        // prefix to the servlet
        initProps.put(INIT_PARAM_RESOURCE_PATH_PREFIX, alias);

        // put the tmp files into Sling home -- or configurable ???
        initProps.put(INIT_PARAM_HOME, slingSettings.getSlingHome() + "/jackrabbit");
        initProps.put(INIT_PARAM_TMP_DIRECTORY, "tmp");

        // disable CSRF checks for now (should be handled by Sling)
        initProps.put(INIT_PARAM_CSRF_PROTECTION, CSRFUtil.DISABLED);

        // register and handle registration failure
        try {
            this.httpService.registerServlet(alias, this, initProps, context);
            this.servletAlias = alias;
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("activate: Failed registering DavEx Servlet at " + alias, e);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.servletAlias != null) {
            this.httpService.unregister(servletAlias);
            this.servletAlias = null;
        }
    }

    @Override
    protected Repository getRepository() {
        return repository;
    }

    private static char[] EMPTY_PW = new char[0];

    @Override
    protected SessionProvider getSessionProvider() {
        return new SessionProvider() {

            public Session getSession(final HttpServletRequest req,
                    final Repository repository,
                    final String workspace)
            throws LoginException, RepositoryException {
                final ResourceResolver resolver = (ResourceResolver) req.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
                if ( resolver != null ) {
                    final Session session = resolver.adaptTo(Session.class);
                    // as the session might be longer used by davex than the request
                    // we have to create a new session!
                    if ( session != null ) {
                        final Credentials credentials = new SimpleCredentials(session.getUserID(), EMPTY_PW);
                        final Session newSession = session.impersonate(credentials);
                        return newSession;
                    }
                }
                return null;
            }

            public void releaseSession(final Session session) {
                session.logout();
            }
        };
    }

}
