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
package org.apache.sling.jcr.resource.internal;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;

/**
 * First attempt of an resource resolver factory implementation.
 * WORK IN PROGRESS - see SLING-1262
 */
@Component
@Service(value=ResourceResolverFactory.class)
public class ResourceResolverFactoryImpl implements ResourceResolverFactory {

    @Reference
    private SlingRepository repository;

    @Reference
    private JcrResourceResolverFactory factory;

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    public ResourceResolver getAdministrativeResourceResolver(final Map<String, Object> authenticationInfo)
    throws LoginException {
        final String workspace = getWorkspace(authenticationInfo);
        final Session session;
        try {
            session = this.repository.loginAdministrative(workspace);
        } catch (RepositoryException re) {
            throw getLoginException(re);
        }
        return factory.getResourceResolver(handleSudo(session, authenticationInfo));
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    public ResourceResolver getResourceResolver(final Map<String, Object> authenticationInfo)
    throws LoginException {
        final Credentials credentials = getCredentials(authenticationInfo);
        final String workspace = getWorkspace(authenticationInfo);
        final Session session;
        try {
            if ( credentials == null ) {
                session = this.repository.login(workspace);
            } else {
                session = this.repository.login(credentials, workspace);
            }
        } catch (RepositoryException re) {
            throw getLoginException(re);
        }
        return factory.getResourceResolver(handleSudo(session, authenticationInfo));
    }

    /**
     * Create a login exception from a repository exception.
     * If the repository exception is a  {@link javax.jcr.LoginException}
     * a {@link LoginException} is created with the same information.
     * Otherwise a {@link LoginException} is created which wraps the
     * repository exception.
     * @param re The repository exception.
     * @return The login exception.
     */
    private LoginException getLoginException(final RepositoryException re) {
        if ( re instanceof javax.jcr.LoginException ) {
            return new LoginException(re.getMessage(), re.getCause());
        }
        return new LoginException("Unable to login " + re.getMessage(), re);
    }

    /**
     * Return the workspace name.
     * If the workspace name is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured workspace name or <code>null</code>
     */
    private String getWorkspace(final Map<String, Object> authenticationInfo) {
        if ( authenticationInfo != null ) {
            return (String) authenticationInfo.get("user.jcr.workspace");
        }
        return null;
    }

    /**
     * Return the sudo user information.
     * If the sudo user info is provided, it is returned, otherwise
     * <code>null</code> is returned.
     * @param authenticationInfo Optional authentication info.
     * @return The configured sudo user information or <code>null</code>
     */
    private String getSudoUser(final Map<String, Object> authenticationInfo) {
        if ( authenticationInfo != null ) {
            return (String) authenticationInfo.get(ResourceResolverFactory.SUDO_USER_ID);
        }
        return null;
    }

    /**
     * Handle the sudo if configured.
     * If the authentication info does not contain a sudo info, this method simply returns
     * the passed in session.
     * If a sudo user info is available, the session is tried to be impersonated. The new
     * impersonated session is returned. The original session is closed. The session is
     * also closed if the impersonation fails.
     * @param session The session.
     * @param authenticationInfo The optional authentication info.
     * @return The original session or impersonated session.
     * @throws LoginException If something goes wrong.
     */
    private Session handleSudo(final Session session, final Map<String, Object> authenticationInfo)
    throws LoginException {
        final String sudoUser = getSudoUser(authenticationInfo);
        if ( sudoUser != null ) {
            try {
                final SimpleCredentials creds = new SimpleCredentials(sudoUser, new char[0]);
                return session.impersonate(creds);
            } catch ( RepositoryException re) {
                throw getLoginException(re);
            } finally {
                session.logout();
            }
        }
        return session;
    }

    /**
     * Create a credentials object from the provided authentication info.
     * If no map is provided, <code>null</code> is returned.
     * If a map is provided and contains a credentials object, this object is
     * returned.
     * If a map is provided but does not contain a credentials object nor a
     * user, <code>null</code> is returned.
     * if a map is provided with a user name but without a credentials object
     * a new credentials object is created and all values from the authentication
     * info are added as attributes.
     * @param authenticationInfo Optional authentication info
     * @return A credentials object or <code>null</code>
     */
    private Credentials getCredentials(final Map<String, Object> authenticationInfo) {
        if ( authenticationInfo == null ) {
            return null;
        }
        Credentials credentials = (Credentials) authenticationInfo.get("user.jcr.credentials");
        if ( credentials == null ) {
            // otherwise try to create SimpleCredentials if the userId is set
            final String userId = (String) authenticationInfo.get("user.name");
            if (userId != null) {
                final char[] password = (char[]) authenticationInfo.get("user.password");
                credentials = new SimpleCredentials(userId, (password == null ? new char[0] : password));

                // add attributes
                final Iterator<Map.Entry<String, Object>> i = authenticationInfo.entrySet().iterator();
                while  (i.hasNext() ) {
                    final Map.Entry<String, Object> current = i.next();
                    ((SimpleCredentials)credentials).setAttribute(current.getKey(), current.getValue());
                }
            }
        }
        return credentials;
    }
}
