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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.xing.api.XingUser;
import org.apache.sling.auth.xing.oauth.XingOauthUserManager;
import org.apache.sling.auth.xing.oauth.XingOauthUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.scribe.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XingOauthAuthenticationPlugin implements AuthenticationPlugin {

    final XingOauthUserManager xingOauthUserManager;

    private final Logger logger = LoggerFactory.getLogger(XingOauthAuthenticationPlugin.class);

    public XingOauthAuthenticationPlugin(final XingOauthUserManager xingOauthUserManager) {
        this.xingOauthUserManager = xingOauthUserManager;
    }

    @Override
    public boolean authenticate(final Credentials credentials) throws RepositoryException {
        logger.debug("authenticate");

        final Token accessToken = XingOauthUtil.getAccessToken(credentials);
        final XingUser xingUser = XingOauthUtil.getXingUser(credentials);
        if (accessToken == null || xingUser == null) {
            return false;
        }

        User user = xingOauthUserManager.getUser(credentials);
        if (user == null) { // check if given credentials pulled up an existing user
            logger.debug("no user found for given credentials");
            if (xingOauthUserManager.autoCreate()) {
                logger.debug("creating a new user from given user data");
                user = xingOauthUserManager.createUser(credentials);
            }
        } else {
            if (xingOauthUserManager.autoUpdate()) {
                xingOauthUserManager.updateUser(credentials);
            }
        }

        return user != null;
    }

}
