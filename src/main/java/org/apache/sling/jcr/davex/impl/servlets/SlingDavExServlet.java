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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Credentials;
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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;

/**
 * DavEx WebDav servlet which acquires a Repository instance via the OSGi
 * service registry.
 *
 */
@Component(label = "%dav.name", description = "%dav.description")
@Properties( { @Property(name = "alias", value = "/server"),
        @Property(name = "init.resource-path-prefix", value = "/server"),
        @Property(name = "init.missing-auth-mapping", value = ""),
        @Property(name = "service.description", value = "Sling JcrRemoting Servlet"),
        @Property(name = "service.vendor", value = "The Apache Software Foundation") })
public class SlingDavExServlet extends JcrRemotingServlet {

    private static final String INIT_KEY_PREFIX = "init.";

    @Reference
    private Repository repository;

    @Reference
    private HttpService httpService;

    @Reference
    private AuthenticationSupport authentiator;

    @Activate
    protected void activate(final ComponentContext ctx)
    throws Exception {
        final AuthHttpContext context = new AuthHttpContext();
        context.setAuthenticationSupport(authentiator);

        final String alias = (String)ctx.getProperties().get("alias");
        final Dictionary<String, String> initProps = new Hashtable<String, String>();
        @SuppressWarnings("unchecked")
        final Enumeration<String> keyEnum = ctx.getProperties().keys();
        while ( keyEnum.hasMoreElements() ) {
            final String key = keyEnum.nextElement();
            if ( key.startsWith(INIT_KEY_PREFIX) ) {
                final String paramKey = key.substring(INIT_KEY_PREFIX.length());
                final Object paramValue = ctx.getProperties().get(key);

                if (paramValue != null) {
                    initProps.put(paramKey, paramValue.toString());
                }
            }
        }

        this.httpService.registerServlet(alias, this, initProps, context);
    }

    @Deactivate
    protected void deactivate(final Map<String, Object> props) {
        final String alias = (String)props.get("alias");
        this.httpService.unregister(alias);
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
            throws LoginException, ServletException, RepositoryException {
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
