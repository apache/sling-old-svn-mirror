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
package org.apache.sling.sample.slingshot.impl;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SetupService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory factory;

    private static final String[] USERS = new String[] {"slingshot1", "slingshot2"};

    @Activate
    protected void activate() throws LoginException, PersistenceException, RepositoryException {
        logger.info("Setting up SlingShot...");
        ResourceResolver resolver = null;
        try {
            resolver = this.factory.getAdministrativeResourceResolver(null);
            setupUsers(resolver);
            setupContent(resolver);
            setupACL(resolver);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        logger.info("Finished setting up SlingShot");
    }

    private void setupACL(final ResourceResolver resolver) throws RepositoryException {
        final Session session = resolver.adaptTo(Session.class);

        for(final String principalId : USERS) {
            final String resourcePath = SlingshotConstants.APP_ROOT_PATH + "/public/" + principalId;

            final Map<String, String> privileges = new HashMap<String, String>();
            privileges.put(Privilege.JCR_ALL, "granted");

            modifyAce(session, resourcePath, principalId, Privilege.JCR_ALL, true);

            privileges.clear();
        }
    }

    private void modifyAce(final Session jcrSession,
            final String resourcePath,
            final String principalId,
            final String privilege,
            final boolean granted)
    throws RepositoryException {
        final PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(jcrSession);
        final Principal principal = principalManager.getPrincipal(principalId);

        final String[] grantedPrivilegeNames;
        final String[] deniedPrivilegeNames;
        if ( granted ) {
            grantedPrivilegeNames = new String[] {privilege};
            deniedPrivilegeNames = null;
        } else {
            grantedPrivilegeNames = null;
            deniedPrivilegeNames = new String[] {privilege};
        }

        AccessControlUtil.replaceAccessControlEntry(jcrSession, resourcePath, principal,
                grantedPrivilegeNames,
                deniedPrivilegeNames,
                null,
                null);
        if (jcrSession.hasPendingChanges()) {
            jcrSession.save();
        }
    }

    private void setupUsers(final ResourceResolver resolver) throws RepositoryException {
        final UserManager um = AccessControlUtil.getUserManager(resolver.adaptTo(Session.class));
        for(final String userName : USERS) {
            Authorizable user = um.getAuthorizable(userName);
            if ( user == null ) {
                logger.info("Creating user {}", userName);
                um.createUser(userName, userName);
            }
        }
    }

    private void setupContent(final ResourceResolver resolver) throws PersistenceException {
        final Resource root = resolver.getResource(SlingshotConstants.APP_ROOT_PATH);
        if ( root != null ) {
            // fix resource type of root folder
            if ( !root.isResourceType(SlingshotConstants.RESOURCETYPE_HOME)) {
                final ModifiableValueMap mvm = root.adaptTo(ModifiableValueMap.class);
                mvm.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_HOME);
                resolver.commit();
            }
            final Resource publicResource = root.getChild("public");
            for(final String userName : USERS) {
                final String path = SlingshotConstants.APP_ROOT_PATH + "/public/" + userName;
                final Resource homeResource = resolver.getResource(path);
                if ( homeResource == null ) {
                    final Map<String, Object> props = new HashMap<String, Object>();
                    props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_USER);
                    resolver.create(publicResource, userName, props);
                    resolver.commit();
                }
            }
        }
    }
}
