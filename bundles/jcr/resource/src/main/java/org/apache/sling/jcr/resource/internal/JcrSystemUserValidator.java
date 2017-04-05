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
package org.apache.sling.jcr.resource.internal;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link org.apache.sling.serviceusermapping.ServiceUserValidator}
 * interface that verifies that all registered service users are represented by
 * {@link org.apache.jackrabbit.api.security.user.User#isSystemUser() system users}
 * in the underlying JCR repository.
 *
 * @see org.apache.jackrabbit.api.security.user.User#isSystemUser()
 */
@Designate(ocd = JcrSystemUserValidator.Config.class)
@Component(service = ServiceUserValidator.class,
           property = {
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public class JcrSystemUserValidator implements ServiceUserValidator {

    @ObjectClassDefinition(
            name = "Apache Sling JCR System User Validator",
            description = "Enforces the usage of JCR system users for all user mappings being used in the 'Sling Service User Mapper Service'")
    public @interface Config {

        @AttributeDefinition(name = "Allow only JCR System Users",
                description="If set to true, only user IDs bound to JCR system users are allowed in the user mappings of the 'Sling Service User Mapper Service'. Otherwise all users are allowed!")
        boolean allow_only_system_user() default true;
    }
    /**
     * logger instance
     */
    private final Logger log = LoggerFactory.getLogger(JcrSystemUserValidator.class);

    @Reference
    private volatile SlingRepository repository;

    private final Method isSystemUserMethod;

    private final Set<String> validIds = new CopyOnWriteArraySet<String>();

    private boolean allowOnlySystemUsers;

    public JcrSystemUserValidator() {
        Method m = null;
        try {
            m = User.class.getMethod("isSystemUser");
        } catch (Exception e) {
            log.debug("Exception while accessing isSystemUser method", e);
        }
        isSystemUserMethod = m;
    }

    @Activate
    public void activate(final Config config) {
        allowOnlySystemUsers = config.allow_only_system_user();
    }

    @Override
    public boolean isValid(final String serviceUserId, final String serviceName, final String subServiceName) {
        if (serviceUserId == null) {
            log.debug("The provided service user id is null");
            return false;
        }
        if (!allowOnlySystemUsers) {
            log.debug("There is no enforcement of JCR system users, therefore service user id '{}' is valid", serviceUserId);
            return true;
        }
        if (validIds.contains(serviceUserId)) {
            log.debug("The provided service user id '{}' has been already validated and is a known JCR system user id", serviceUserId);
            return true;
        } else {
            Session administrativeSession = null;
            try {
                try {
                    /*
                     * TODO: Instead of using the deprecated loginAdministrative
                     * method, this bundle could be configured with an appropriate
                     * user for service authentication and do:
                     *     tmpSession = repository.loginService(null, workspace);
                     * For now, we keep loginAdministrative as switching to a service user
                     * will result in a endless recursion (this method checks if
                     * a service user is allowed, so using a service user here
                     * calls this method again...and again...and again)
                     */
                    administrativeSession = repository.loginAdministrative(null);
                    if (administrativeSession instanceof JackrabbitSession) {
                        final UserManager userManager = ((JackrabbitSession) administrativeSession).getUserManager();
                        final Authorizable authorizable = userManager.getAuthorizable(serviceUserId);
                        if (authorizable != null && !authorizable.isGroup() && (isSystemUser((User)authorizable))) {
                            validIds.add(serviceUserId);
                            log.debug("The provided service user id {} is a known JCR system user id", serviceUserId);
                            return true;
                        }
                    }
                } catch (final RepositoryException e) {
                    log.warn("Could not get user information", e);
                }
            } finally {
                if (administrativeSession != null) {
                    administrativeSession.logout();
                }
            }
            log.warn("The provided service user id '{}' is not a known JCR system user id and therefore not allowed in the Sling Service User Mapper.", serviceUserId);
            return false;
        }
    }


    private boolean isSystemUser(final User user){
        if (isSystemUserMethod != null) {
            try {
                return (Boolean) isSystemUserMethod.invoke(user);
            } catch (Exception e) {
                log.debug("Exception while invoking isSystemUser method", e);
                return true;
            }
         } else {
             return true;
         }
    }
}