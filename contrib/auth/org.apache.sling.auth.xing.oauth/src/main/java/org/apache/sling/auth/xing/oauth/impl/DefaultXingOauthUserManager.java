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

import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

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
import org.apache.sling.auth.xing.oauth.XingOauth;
import org.apache.sling.auth.xing.oauth.XingOauthUserManager;
import org.apache.sling.auth.xing.oauth.XingOauthUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    label = "Apache Sling Authentication XING OAuth “Default User Manager”",
    description = "Default User Manager for Sling Authentication XING OAuth",
    immediate = true,
    metatype = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = XingOauth.SERVICE_VENDOR),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Default User Manager for Sling Authentication XING OAuth"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0, propertyPrivate = false)
})
public class DefaultXingOauthUserManager extends AbstractXingUserManager implements XingOauthUserManager {

    @Reference
    private SlingRepository slingRepository;

    private static final String FIRSTNAME_PROPERTY = "firstname";

    private static final String LASTNAME_PROPERTY = "lastname";

    @Property(boolValue = DEFAULT_AUTO_CREATE_USER)
    private static final String AUTO_CREATE_USER_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.DefaultXingOauthUserManager.user.create.auto";

    @Property(boolValue = DEFAULT_AUTO_UPDATE_USER)
    private static final String AUTO_UPDATE_USER_PARAMETER = "org.apache.sling.auth.xing.oauth.impl.DefaultXingOauthUserManager.user.update.auto";

    private final Logger logger = LoggerFactory.getLogger(DefaultXingOauthUserManager.class);

    public DefaultXingOauthUserManager() {
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
        autoCreateUser = PropertiesUtil.toBoolean(properties.get(AUTO_CREATE_USER_PARAMETER), DEFAULT_AUTO_CREATE_USER);
        autoUpdateUser = PropertiesUtil.toBoolean(properties.get(AUTO_UPDATE_USER_PARAMETER), DEFAULT_AUTO_UPDATE_USER);
    }

    @Override
    protected SlingRepository getSlingRepository() {
        return slingRepository;
    }

    @Override
    public User createUser(final Credentials credentials) {
        logger.debug("create user");
        final XingUser xingUser = XingOauthUtil.getXingUser(credentials);
        if (xingUser == null) {
            return null;
        }

        try {
            final String userId = xingUser.getId(); // TODO make configurable

            final Session session = getSession();
            final UserManager userManager = getUserManager(session);
            final User user = userManager.createUser(userId, null);

            // TODO disable user on create?
            final ValueFactory valueFactory = session.getValueFactory();
            final Value firstnameValue = valueFactory.createValue(xingUser.getFirstName());
            final Value lastnameValue = valueFactory.createValue(xingUser.getLastName());
            user.setProperty(FIRSTNAME_PROPERTY, firstnameValue);
            user.setProperty(LASTNAME_PROPERTY, lastnameValue);
            session.save();
            return user;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public User updateUser(Credentials credentials) {
        logger.debug("update user");
        final XingUser xingUser = XingOauthUtil.getXingUser(credentials);
        if (xingUser == null) {
            return null;
        }

        try {
            final Session session = getSession();
            final User user = getUser(credentials);
            final ValueFactory valueFactory = session.getValueFactory();

            final boolean firstnameUpdated = updateUserProperty(user, valueFactory, FIRSTNAME_PROPERTY, xingUser.getFirstName());
            final boolean lastnameUpdated = updateUserProperty(user, valueFactory, LASTNAME_PROPERTY, xingUser.getLastName());
            if (firstnameUpdated || lastnameUpdated) {
                session.save();
            }

            return user;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private boolean updateUserProperty(final User user, final ValueFactory valueFactory, final String property, final String string) throws RepositoryException {
        final Value[] values = user.getProperty(property);
        if (values != null && values.length > 0) {
            if (string.equals(values[0].getString())) {
                return false;
            }
        }
        final Value value = valueFactory.createValue(string);
        user.setProperty(property, value);
        return true;
    }

}
