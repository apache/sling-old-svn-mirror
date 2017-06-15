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
package org.apache.sling.jackrabbit.usermanager.impl.resource;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource Provider implementation for jackrabbit UserManager resources.
 */
@Component(service = ResourceProvider.class,
    property={
    		"service.description=Resource provider implementation for UserManager resources",
    		"service.vendor=The Apache Software Foundation",
    		ResourceProvider.PROPERTY_ROOT + "=" + AuthorizableResourceProvider.SYSTEM_USER_MANAGER_PATH
    })
public class AuthorizableResourceProvider extends ResourceProvider<Object> {

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String SYSTEM_USER_MANAGER_PATH = "/system/userManager";

    public static final String SYSTEM_USER_MANAGER_USER_PATH = SYSTEM_USER_MANAGER_PATH
        + "/user";

    public static final String SYSTEM_USER_MANAGER_GROUP_PATH = SYSTEM_USER_MANAGER_PATH
        + "/group";

    public static final String SYSTEM_USER_MANAGER_USER_PREFIX = SYSTEM_USER_MANAGER_USER_PATH
        + "/";

    public static final String SYSTEM_USER_MANAGER_GROUP_PREFIX = SYSTEM_USER_MANAGER_GROUP_PATH
        + "/";

    
    @Override
	public Resource getResource(ResolveContext<Object> ctx, 
		    String path, 
		    ResourceContext resourceContext,
			Resource parent) {

        // handle resources for the virtual container resources
        if (path.equals(SYSTEM_USER_MANAGER_PATH)) {
            return new SyntheticResource(ctx.getResourceResolver(), path,
                "sling/userManager");
        } else if (path.equals(SYSTEM_USER_MANAGER_USER_PATH)) {
            return new SyntheticResource(ctx.getResourceResolver(), path, "sling/users");
        } else if (path.equals(SYSTEM_USER_MANAGER_GROUP_PATH)) {
            return new SyntheticResource(ctx.getResourceResolver(), path, "sling/groups");
        }

        // the principalId should be the first segment after the prefix
        String pid = null;
        if (path.startsWith(SYSTEM_USER_MANAGER_USER_PREFIX)) {
            pid = path.substring(SYSTEM_USER_MANAGER_USER_PREFIX.length());
        } else if (path.startsWith(SYSTEM_USER_MANAGER_GROUP_PREFIX)) {
            pid = path.substring(SYSTEM_USER_MANAGER_GROUP_PREFIX.length());
        }

        if (pid != null) {
            if (pid.indexOf('/') != -1) {
                return null; // something bogus on the end of the path so bail
                             // out now.
            }
            try {
                Session session = ctx.getResourceResolver().adaptTo(Session.class);
                if (session != null) {
                    UserManager userManager = AccessControlUtil.getUserManager(session);
                    if (userManager != null) {
                        Authorizable authorizable = userManager.getAuthorizable(pid);
                        if (authorizable != null) {
                            // found the Authorizable, so return the resource
                            // that wraps it.
                            return new AuthorizableResource(authorizable,
                            		ctx.getResourceResolver(), path);
                        }
                    }
                }
            } catch (RepositoryException re) {
                throw new SlingException(
                    "Error looking up Authorizable for principal: " + pid, re);
            }
        }
        return null;
    }

	@Override
	public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
        try {
            String path = parent.getPath();
            ResourceResolver resourceResolver = parent.getResourceResolver();

            // handle children of /system/userManager
            if (SYSTEM_USER_MANAGER_PATH.equals(path)) {
                List<Resource> resources = new ArrayList<Resource>();
                if (resourceResolver != null) {
                    resources.add(getResource(ctx,
                        SYSTEM_USER_MANAGER_USER_PATH, null, null));
                    resources.add(getResource(ctx,
                        SYSTEM_USER_MANAGER_GROUP_PATH, null, null));
                }
                return resources.iterator();
            }

            int searchType = -1;
            if (SYSTEM_USER_MANAGER_USER_PATH.equals(path)) {
                searchType = PrincipalManager.SEARCH_TYPE_NOT_GROUP;
            } else if (SYSTEM_USER_MANAGER_GROUP_PATH.equals(path)) {
                searchType = PrincipalManager.SEARCH_TYPE_GROUP;
            }
            if (searchType != -1) {
                PrincipalIterator principals = null;
                Session session = resourceResolver.adaptTo(Session.class);
                if (session != null) {
                    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
                    principals = principalManager.getPrincipals(searchType);
                }

                if (principals != null) {
                    return new ChildrenIterator(parent, principals);
                }
            }
        } catch (RepositoryException re) {
            throw new SlingException("Error listing children of resource: "
                + parent.getPath(), re);
        }

        return null;
    }

    private final class ChildrenIterator implements Iterator<Resource> {
        private PrincipalIterator principals;

        private Resource parent;

        public ChildrenIterator(Resource parent, PrincipalIterator principals) {
            this.parent = parent;
            this.principals = principals;
        }

        public boolean hasNext() {
            return principals.hasNext();
        }

        public Resource next() {
            Principal nextPrincipal = principals.nextPrincipal();
            try {
                ResourceResolver resourceResolver = parent.getResourceResolver();
                if (resourceResolver != null) {
                    Session session = resourceResolver.adaptTo(Session.class);
                    if (session != null) {
                        UserManager userManager = AccessControlUtil.getUserManager(session);
                        if (userManager != null) {
                            Authorizable authorizable = userManager.getAuthorizable(nextPrincipal.getName());
                            if (authorizable != null) {
                                String path;
                                if (authorizable.isGroup()) {
                                    path = SYSTEM_USER_MANAGER_GROUP_PREFIX
                                        + nextPrincipal.getName();
                                } else {
                                    path = SYSTEM_USER_MANAGER_USER_PREFIX
                                        + nextPrincipal.getName();
                                }
                                return new AuthorizableResource(authorizable,
                                    resourceResolver, path);
                            }
                        }
                    }
                }
            } catch (RepositoryException re) {
                log.error("Exception while looking up authorizable resource.",
                    re);
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
