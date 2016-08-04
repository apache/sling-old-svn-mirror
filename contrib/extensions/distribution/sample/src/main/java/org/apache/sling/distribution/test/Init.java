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


package org.apache.sling.distribution.test;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.jcr.Session;


@Component(immediate = true)
public class Init {

    Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    SlingRepository slingRepository;

    @Activate
    public void activate() throws Exception {

        try {
            final String defaultAgentUserName = "distribution-agent-user";
            final String serviceUserName = "testDistributionUser";
            final String distributorUserName = "testDistributorUser";

            Session session = slingRepository.loginAdministrative(null);

            JackrabbitSession jackrabittSession  = (JackrabbitSession) session;
            UserManager userManager = jackrabittSession.getUserManager();
            User serviceUser = createOrGetServiceUser(userManager, serviceUserName);

            if (serviceUser != null) {
                AccessControlUtils.addAccessControlEntry(session, "/var/sling/distribution/packages", serviceUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);
                AccessControlUtils.addAccessControlEntry(session, "/content", serviceUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);
                AccessControlUtils.addAccessControlEntry(session, null, serviceUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);

            }

            Authorizable distributorUser = createOrGetRegularUser(userManager, distributorUserName);

            JcrUtils.getOrCreateByPath("/content", "sling:Folder", session);

            if (distributorUser != null) {
                AccessControlUtils.addAccessControlEntry(session, "/var/sling/distribution/packages", distributorUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);
                AccessControlUtils.addAccessControlEntry(session, "/content", distributorUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);
                AccessControlUtils.addAccessControlEntry(session, "/libs/sling/distribution", distributorUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);
                AccessControlUtils.addAccessControlEntry(session, "/etc/distribution", distributorUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);

                AccessControlUtils.addAccessControlEntry(session, null, distributorUser.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);

            }

            User defaultAgentUser = createOrGetServiceUser(userManager, defaultAgentUserName);

            if (defaultAgentUser != null) {
                ((User) distributorUser).getImpersonation().grantImpersonation(defaultAgentUser.getPrincipal());
                ((User) serviceUser).getImpersonation().grantImpersonation(defaultAgentUser.getPrincipal());
            }

            session.save();
            session.logout();
        } catch (Throwable t) {
            log.error("cannot create user", t);
        }
    }


    private User createOrGetServiceUser(UserManager userManager, String serviceUserName) throws RepositoryException {
        Authorizable serviceUser = userManager.getAuthorizable(serviceUserName);

        if (serviceUser == null) {
            try {
                serviceUser = userManager.createSystemUser(serviceUserName, null);
                log.info("created system user {}", serviceUserName);

            } catch (Throwable t) {
                serviceUser = userManager.createUser(serviceUserName, "123");
                log.info("created regular user {}", serviceUserName);
            }
        }

        return (User) serviceUser;

    }

    private User createOrGetRegularUser(UserManager userManager, String userName) throws RepositoryException {
        Authorizable serviceUser = userManager.getAuthorizable(userName);

        if (serviceUser == null) {
            serviceUser = userManager.createUser(userName, "123");
            log.info("created regular user {}", userName);
        }

        return (User) serviceUser;

    }




}
