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
package org.apache.sling.auth.xing.login.impl;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.xing.login.XingLoginUserManager;
import org.apache.sling.auth.xing.login.XingLoginUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XingLoginAuthenticationPlugin implements AuthenticationPlugin {

    private final XingLoginUserManager xingLoginUserManager;

    private final Logger logger = LoggerFactory.getLogger(XingLoginAuthenticationPlugin.class);

    public XingLoginAuthenticationPlugin(final XingLoginUserManager xingLoginUserManager) {
        this.xingLoginUserManager = xingLoginUserManager;
    }

    @Override
    public boolean authenticate(final Credentials credentials) throws RepositoryException {
        logger.debug("authenticate");

        // check if given credentials have a hash
        final String givenHash = XingLoginUtil.getHash(credentials);
        if (givenHash == null) {
            logger.debug("unable to get hash from given credentials");
            return false;
        }

        User user = xingLoginUserManager.getUser(credentials);
        if (user == null) { // check if given credentials pull up an existing user
            logger.debug("no user found for given credentials");
            if (xingLoginUserManager.autoCreate()) {
                logger.debug("creating a new user from given user data");
                // validating with hash happens in createUser(credentials)
                user = xingLoginUserManager.createUser(credentials);
            }
        }

        // re-check
        if (user == null) {
            return false;
        }

        // check if stored user has a hash
        final String storedHash = xingLoginUserManager.getHash(user);
        if (storedHash == null) { // should not happen, so return false
            logger.debug("no hash found for user '{}'", user.getID());
            return false;
        }

        // check if hashes match
        if (givenHash.equals(storedHash)) {
            logger.debug("hashes for user '{}' do match", user.getID());
            return true;
        } else {
            logger.debug("hashes for user '{}' do not match", user.getID());
            if (xingLoginUserManager.autoUpdate()) {
                logger.debug("updating an existing user from given user data");
                // validating with hash happens in updateUser(credentials)
                return xingLoginUserManager.updateUser(credentials) != null;
            }
        }

        return false;
    }

}
