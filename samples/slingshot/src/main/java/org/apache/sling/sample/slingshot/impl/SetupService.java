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
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.apache.sling.sample.slingshot.model.User;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The setup service checks the resource types for the main folders,
 * as some of them can't be set through initial content.
 */
@Component
public class SetupService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory factory;

    private static final String[] USERS = new String[] {"slingshot1", "slingshot2"};

    private static final String[] FOLDERS = new String[] {
        "info",
        "settings",
        "ugc"};

    @Activate
    protected void activate(final BundleContext bc) throws IOException, LoginException, PersistenceException, RepositoryException {
        logger.info("Setting up SlingShot...");
        ResourceResolver resolver = null;
        try {
            resolver = this.factory.getServiceResourceResolver(null);
            setupContent(resolver);
        } finally {
            if ( resolver != null ) {
                resolver.close();
            }
        }
        logger.info("Finished setting up SlingShot");
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
                    props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, User.RESOURCETYPE);
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
