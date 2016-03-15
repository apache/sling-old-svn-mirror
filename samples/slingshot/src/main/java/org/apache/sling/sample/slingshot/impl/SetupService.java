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

import java.io.IOException;
import java.security.Principal;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The setup service sets up difference things.
 */
@Component
public class SetupService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory factory;

    @Reference
    private ConfigurationAdmin configAdmin;

    private static final String[] USERS = new String[] {"slingshot1", "slingshot2"};

    private static final String[] FOLDERS = new String[] {
        "content:" + SlingshotConstants.RESOURCETYPE_CONTENT,
        "info",
        "profile",
        "ugc"};

    @Activate
    protected void activate(final BundleContext bc) throws IOException, LoginException, PersistenceException, RepositoryException {
        logger.info("Setting up SlingShot...");
        ResourceResolver resolver = null;
        try {
            resolver = this.factory.getAdministrativeResourceResolver(null);
            setupUsers(bc, resolver);
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

        // create default slingshot users
        for(final String principalId : USERS) {
            // user home
            final String resourcePath = SlingshotConstants.APP_ROOT_PATH + "/users/" + principalId;
            modifyAce(session, resourcePath, principalId, Privilege.JCR_ALL, true);

            // ugc path
            final String ugcPath = resourcePath + "/ugc";
            modifyAce(session, ugcPath,
                    InternalConstants.SERVICE_USER_NAME, Privilege.JCR_ALL, true);
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

    private void setupUsers(final BundleContext bc, final ResourceResolver resolver) throws RepositoryException, IOException {
        final Session session = resolver.adaptTo(Session.class);
        final UserManager um = AccessControlUtil.getUserManager(session);
        for(final String userName : USERS) {
            Authorizable user = um.getAuthorizable(userName);
            if ( user == null ) {
                logger.info("Creating user {}", userName);
                um.createUser(userName, userName);
                session.save();
            }
        }

        // create a service user
        Authorizable user = um.getAuthorizable(InternalConstants.SERVICE_USER_NAME);
        if ( user == null ) {
            logger.info("Creating service user {}", InternalConstants.SERVICE_USER_NAME);
            um.createSystemUser(InternalConstants.SERVICE_USER_NAME, null);
            session.save();
        }

        // check for service user config
        boolean exists = false;
        try {
            final Configuration[] configs = this.configAdmin.listConfigurations("(&("
                    + ConfigurationAdmin.SERVICE_FACTORYPID + "=org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended"
                    + ")(user.mapping=" + bc.getBundle().getSymbolicName() + "*"
                    + "))");
            if ( configs != null && configs.length > 0 ) {
                exists = true;
            }
        } catch (final InvalidSyntaxException e) {
            exists = false;
        }
        if ( !exists ) {
            logger.info("Creating service user mapping");
            final Configuration c = this.configAdmin.createFactoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended", null);
            final Dictionary<String, Object> dict = new Hashtable<String, Object>();
            dict.put("user.mapping", bc.getBundle().getSymbolicName() + "=" + InternalConstants.SERVICE_USER_NAME);

            c.update(dict);
        }
    }

    private void setupContent(final ResourceResolver resolver) throws PersistenceException {
        final Resource root = resolver.getResource(SlingshotConstants.APP_ROOT_PATH);
        if ( root != null ) {
            // fix resource type of root folder
            if ( !root.isResourceType(InternalConstants.RESOURCETYPE_HOME)) {
                final ModifiableValueMap mvm = root.adaptTo(ModifiableValueMap.class);
                mvm.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, InternalConstants.RESOURCETYPE_HOME);
                resolver.commit();
            }
            final Resource usersResource = root.getChild("users");
            for(final String userName : USERS) {
                Resource homeResource = resolver.getResource(usersResource, userName);
                if ( homeResource == null ) {
                    final Map<String, Object> props = new HashMap<String, Object>();
                    props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, SlingshotConstants.RESOURCETYPE_USER);
                    homeResource = resolver.create(usersResource, userName, props);
                    resolver.commit();
                }
                for(final String def : FOLDERS) {
                    final int index = def.indexOf(':');
                    final String name;
                    final String rt;
                    if ( index == -1 ) {
                        name = def;
                        rt = "sling:OrderedFolder";
                    } else {
                        name = def.substring(0, index);
                        rt = def.substring(index + 1);
                    }
                    final Resource rsrc = resolver.getResource(homeResource, name);
                    if ( rsrc == null ) {
                        final Map<String, Object> props = new HashMap<String, Object>();
                        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, rt);
                        resolver.create(homeResource, name, props);
                        resolver.commit();
                    }
                }
            }
        }
    }
}
