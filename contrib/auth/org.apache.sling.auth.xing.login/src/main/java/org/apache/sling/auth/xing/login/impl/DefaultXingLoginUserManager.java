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

import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.auth.xing.api.AbstractXingUserManager;
import org.apache.sling.auth.xing.api.XingUser;
import org.apache.sling.auth.xing.login.XingLogin;
import org.apache.sling.auth.xing.login.XingLoginUserManager;
import org.apache.sling.auth.xing.login.XingLoginUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling Authentication XING Login “Default User Manager”",
    description = "Default User Manager for Sling Authentication XING Login",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = XingLogin.SERVICE_VENDOR),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Default User Manager for Sling Authentication XING Login"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
public class DefaultXingLoginUserManager extends AbstractXingUserManager implements XingLoginUserManager {

    private String secretKey;

    private String userDataProperty;

    private String userHashProperty;

    @Reference
    private SlingRepository slingRepository;

    private static final String DEFAULT_USER_DATA_PROPERTY = "data";

    private static final String DEFAULT_USER_HASH_PROPERTY = "hash";

    @Property(value = "")
    private static final String SECRET_KEY_PARAMETER = "org.apache.sling.auth.xing.login.impl.DefaultXingLoginUserManager.secretKey";

    @Property(value = DEFAULT_USER_DATA_PROPERTY)
    private static final String USER_DATA_PROPERTY_PARAMETER = "org.apache.sling.auth.xing.login.impl.DefaultXingLoginUserManager.user.property.data";

    @Property(value = DEFAULT_USER_HASH_PROPERTY)
    private static final String USER_HASH_PROPERTY_PARAMETER = "org.apache.sling.auth.xing.login.impl.DefaultXingLoginUserManager.user.property.hash";

    @Property(boolValue = DEFAULT_AUTO_CREATE_USER)
    private static final String AUTO_CREATE_USER_PARAMETER = "org.apache.sling.auth.xing.login.impl.DefaultXingLoginUserManager.user.create.auto";

    @Property(boolValue = DEFAULT_AUTO_UPDATE_USER)
    private static final String AUTO_UPDATE_USER_PARAMETER = "org.apache.sling.auth.xing.login.impl.DefaultXingLoginUserManager.user.update.auto";

    private final Logger logger = LoggerFactory.getLogger(DefaultXingLoginUserManager.class);

    public DefaultXingLoginUserManager() {
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        logger.debug("activate");
        configure(componentContext);
    }

    @Modified
    protected void modified(final ComponentContext componentContext) {
        logger.debug("modified");
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(final ComponentContext componentContext) {
        logger.debug("deactivate");
        if (session != null) {
            session.logout();
            session = null;
        }
    }

    protected synchronized void configure(final ComponentContext componentContext) {
        final Dictionary properties = componentContext.getProperties();
        secretKey = PropertiesUtil.toString(properties.get(SECRET_KEY_PARAMETER), "").trim();
        userDataProperty = PropertiesUtil.toString(properties.get(USER_DATA_PROPERTY_PARAMETER), DEFAULT_USER_DATA_PROPERTY).trim();
        userHashProperty = PropertiesUtil.toString(properties.get(USER_HASH_PROPERTY_PARAMETER), DEFAULT_USER_HASH_PROPERTY).trim();
        autoCreateUser = PropertiesUtil.toBoolean(properties.get(AUTO_CREATE_USER_PARAMETER), DEFAULT_AUTO_CREATE_USER);
        autoUpdateUser = PropertiesUtil.toBoolean(properties.get(AUTO_UPDATE_USER_PARAMETER), DEFAULT_AUTO_UPDATE_USER);

        if (StringUtils.isEmpty(secretKey)) {
            logger.warn("configured secret key is empty");
        }
    }

    @Override
    protected SlingRepository getSlingRepository() {
        return slingRepository;
    }

    @Override
    public User createUser(final Credentials credentials) {
        logger.debug("create user");
        return storeUser(credentials);
    }

    @Override
    public User updateUser(Credentials credentials) {
        logger.debug("update user");
        return storeUser(credentials);
    }

    protected User storeUser(Credentials credentials) {
        final String givenHash = XingLoginUtil.getHash(credentials);
        final String json = XingLoginUtil.getUser(credentials);

        if (givenHash == null || json == null) {
            logger.debug("unable to get hash and/or user data from given credentials");
            return null;
        }

        // validate user data with hash
        try {
            final String computedHash = XingLoginUtil.hash(json, secretKey, XingLogin.HASH_ALGORITHM);
            final boolean match = givenHash.equals(computedHash);
            if (!match) {
                logger.warn("invalid hash or user data given, aborting");
                return null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        try {
            final XingUser xingUser = XingLoginUtil.fromJson(json);
            final String userId = xingUser.getId(); // TODO make configurable

            User user = getUser(userId);
            if (user == null) {
                logger.debug("creating a new user with id '{}'", userId);
                final Session session = getSession();
                final UserManager userManager = getUserManager(session);
                user = userManager.createUser(userId, null);
            } else {
                logger.debug("updating an existing user with id '{}'", userId);
            }

            // TODO disable user on create?
            final ValueFactory valueFactory = getSession().getValueFactory();
            final Value dataValue = valueFactory.createValue(json);
            final Value hashValue = valueFactory.createValue(givenHash);
            user.setProperty(userDataProperty, dataValue);
            user.setProperty(userHashProperty, hashValue);
            session.save();
            return user;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getHash(final User user) {
        try {
            final Value[] values = user.getProperty(userHashProperty);
            if (values != null && values.length == 1) {
                final Value value = values[0];
                return value.getString();
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
