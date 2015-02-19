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
import java.util.HashSet;
import java.util.Set; 
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
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
@Component(label = "Service User Validation Service", description = "Service user validation for JCR system users.")
@Service(ServiceUserValidator.class)
public class JcrSystemUserValidator implements ServiceUserValidator {
    
    /**
     * logger instance
     */
    private final Logger log = LoggerFactory.getLogger(JcrSystemUserValidator.class);

    @Reference
    private volatile SlingRepository repository;
    
    private  Method isSystemUserMethod; 

    private Set<String> validIds = new HashSet<String>();
    
    public JcrSystemUserValidator(){
        try {
            isSystemUserMethod = User.class.getMethod("isSystemUser");
        } catch (Exception e) {
            log.debug("Exception while accessing isSystemUser method", e);
            isSystemUserMethod = null;
        }
    }

    public boolean isValid(String serviceUserId, String serviceName, String subServiceName) {
        if (serviceUserId == null) {
            log.debug("the provided service user id is null");
            return false;
        }
        if (validIds.contains(serviceUserId)) {
            log.debug("the provided service user id {} has been already validated", serviceUserId);
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
                     * For now, we keep loginAdministrative
                     */
                    administrativeSession = repository.loginAdministrative(null);
                    if (administrativeSession instanceof JackrabbitSession) {
                        UserManager userManager = ((JackrabbitSession) administrativeSession).getUserManager();
                        Authorizable authorizable = userManager.getAuthorizable(serviceUserId);
                        if (authorizable != null && !authorizable.isGroup() && (isSystemUser((User)authorizable))) {
                            validIds.add(serviceUserId);
                            return true;
                        }
                    }
                } catch (RepositoryException e) {
                    log.debug(e.getMessage());
                }
            } finally {
                if (administrativeSession != null) {
                    administrativeSession.logout();
                }
            }
            return false;
        }
    }
    
    
    private boolean isSystemUser(User user){
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