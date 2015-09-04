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
import org.apache.jackrabbit.api.security.user.UserManager;
import javax.jcr.security.Privilege;
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
            final String userName = "testDistributionUser";
            Session session = slingRepository.loginAdministrative(null);

            JackrabbitSession jackrabittSession  = (JackrabbitSession) session;
            UserManager userManager = jackrabittSession.getUserManager();
            Authorizable user = userManager.getAuthorizable(userName);

            if (user == null) {
                try {
                    user = userManager.createSystemUser(userName, null);
                    log.error("created system user", user);

                } catch (Throwable t) {
                    user = userManager.createUser(userName, "123");
                    log.error("created regular user", user);

                }
            }

            if (user != null) {
                AccessControlUtils.addAccessControlEntry(session, "/", user.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);

                AccessControlUtils.addAccessControlEntry(session, null, user.getPrincipal(), new String[]{ Privilege.JCR_ALL }, true);

                session.save();

                session.logout();
            }

        } catch (Throwable t) {
            log.error("cannot create user", t);
        }



    }



}
