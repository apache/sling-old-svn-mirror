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
package org.apache.sling.auth.xing.oauth.impl;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.xing.api.XingUser;
import org.apache.sling.auth.xing.oauth.XingOauth;
import org.apache.sling.auth.xing.oauth.XingOauthUserManager;
import org.apache.sling.auth.xing.oauth.XingOauthUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.osgi.framework.Constants;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling Authentication XING OAuth “Login Module Plugin”",
    description = "Login Module Plugin for Sling Authentication XING OAuth",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = XingOauth.SERVICE_VENDOR),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Login Module Plugin for Sling Authentication XING OAuth"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
/**
 * @see org.apache.jackrabbit.core.security.authentication.DefaultLoginModule
 */
public class XingOauthLoginModulePlugin implements LoginModulePlugin {

    @Reference
    private XingOauthUserManager xingOauthUserManager;

    private final Logger logger = LoggerFactory.getLogger(XingOauthLoginModulePlugin.class);

    public XingOauthLoginModulePlugin() {
    }

    @Override
    public boolean canHandle(final Credentials credentials) {
        logger.debug("canHandle({})", credentials);
        final Token accessToken = XingOauthUtil.getAccessToken(credentials);
        final XingUser xingUser = XingOauthUtil.getXingUser(credentials);
        logger.debug("access token: {}, xing user: {}", accessToken, xingUser);
        return accessToken != null && xingUser != null;
    }

    @Override
    public void doInit(final CallbackHandler callbackHandler, final Session session, final Map map) throws LoginException {
        logger.debug("doInit({}, {}, {})", callbackHandler, session, map);
    }

    @Override
    public Principal getPrincipal(final Credentials credentials) {
        logger.debug("getPrincipal({})", credentials);
        try {
            User user = xingOauthUserManager.getUser(credentials);
            if (user == null && xingOauthUserManager.autoCreate()) {
                user = xingOauthUserManager.createUser(credentials);
            }
            if (user != null) {
                return user.getPrincipal();
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void addPrincipals(final Set set) {
        logger.debug("addPrincipals({})", set);
    }

    @Override
    public AuthenticationPlugin getAuthentication(final Principal principal, final Credentials credentials) throws RepositoryException {
        logger.debug("getAuthentication({}, {})", principal, credentials);
        return new XingOauthAuthenticationPlugin(xingOauthUserManager);
    }

    @Override
    public int impersonate(final Principal principal, final Credentials credentials) throws RepositoryException, FailedLoginException {
        logger.debug("impersonate({}, {})", principal, credentials);
        return LoginModulePlugin.IMPERSONATION_DEFAULT;
    }

}
