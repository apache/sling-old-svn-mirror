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
package org.apache.sling.jackrabbit.usermanager.impl.post;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.DeleteAuthorizables;
import org.apache.sling.jackrabbit.usermanager.DeleteGroup;
import org.apache.sling.jackrabbit.usermanager.DeleteUser;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 
 * <h2>Rest Service Description</h2>
 * <p>
 * Deletes an Authorizable, currently a user or a group. Maps on to nodes of resourceType <code>sling/users</code> or <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> or <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/user</code> or <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/user.delete.html</code> or <code>/system/userManager/group.delete.html</code>.
 * The servlet also responds to single delete requests eg <code>/system/userManager/group/newGroup.delete.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:applyTo</dt>
 * <dd>An array of relative resource references to Authorizables to be deleted, if this parameter is present, the url is ignored and all the Authorizables in the list are removed.</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, no body.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -Fgo=1 http://localhost:8080/system/userManager/user/ieb.delete.html
 * </code>
 */
@Component (immediate=true, metatype=true,
		label="%deleteAuthorizable.post.operation.name",
		description="%deleteAuthorizable.post.operation.description")
@Service (value={
	Servlet.class,
	DeleteUser.class,
	DeleteGroup.class,
	DeleteAuthorizables.class
})		
@Properties ({
	@Property (name="sling.servlet.resourceTypes",
			value={
					"sling/user",
					"sling/group",
					"sling/userManager"
				}),
	@Property (name="sling.servlet.methods",
			value="POST"),
	@Property (name="sling.servlet.selectors",
			value="delete")
})
public class DeleteAuthorizableServlet extends AbstractPostServlet
        implements DeleteUser, DeleteGroup, DeleteAuthorizables {
    private static final long serialVersionUID = 5874621724096106496L;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
    		AbstractPostResponse response, List<Modification> changes)
            throws RepositoryException {

        Session session = request.getResourceResolver().adaptTo(Session.class);
        Resource resource = request.getResource();
        String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
        if (applyTo != null) {
            deleteAuthorizables(session,
                    resource,
                    applyTo, 
                    changes);
        } else {
            Authorizable item = resource.adaptTo(Authorizable.class);
            if (item == null) {
                String msg = "Missing source " + resource.getPath()
                    + " for delete";
                response.setStatus(HttpServletResponse.SC_NOT_FOUND, msg);
                throw new ResourceNotFoundException(msg);
            } else {
                if (item instanceof User) {
                    deleteUser(session, item.getID(), changes);
                } else if (item instanceof Group) {
                    deleteGroup(session, item.getID(), changes);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.DeleteUser#deleteUser(javax.jcr.Session, java.lang.String, java.util.List)
     */
    public void deleteUser(Session jcrSession, String name,
            List<Modification> changes) throws RepositoryException {

        User user;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof User) {
            user = (User)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "User to delete could not be determined");
        }
        
        String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
                            + user.getID();
        user.remove();
        changes.add(Modification.onDeleted(userPath));
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.DeleteGroup#deleteGroup(javax.jcr.Session, java.lang.String, java.util.List)
     */
    public void deleteGroup(Session jcrSession, 
                            String name,
                            List<Modification> changes) throws RepositoryException {

        Group group;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof Group) {
            group = (Group)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "Group to delete could not be determined");
        }
        
        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                                + group.getID();
        group.remove();
        changes.add(Modification.onDeleted(groupPath));
    }

    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.DeleteAuthorizables#deleteAuthorizables(javax.jcr.Session, org.apache.sling.api.resource.Resource, java.lang.String[], java.util.List)
     */
    public void deleteAuthorizables(Session jcrSession, 
                                    Resource baseResource,
                                    String[] paths, 
                                    List<Modification> changes)
            throws RepositoryException {

        ApplyToIterator iterator = new ApplyToIterator(baseResource, paths);
        while (iterator.hasNext()) {
            Resource resource = iterator.next();
            Authorizable item = resource.adaptTo(Authorizable.class);
            if (item != null) {
                item.remove();
                changes.add(Modification.onDeleted(resource.getPath()));
            }
        }
    }

    private static class ApplyToIterator implements Iterator<Resource> {

        private final ResourceResolver resolver;

        private final Resource baseResource;

        private final String [] paths;

        private int pathIndex;

        private Resource nextResource;

        ApplyToIterator(Resource baseResource, String [] paths) {
            this.resolver = baseResource.getResourceResolver();
            this.baseResource = baseResource;
            this.paths = paths;
            this.pathIndex = 0;

            nextResource = seek();
        }

        public boolean hasNext() {
            return nextResource != null;
        }

        public Resource next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Resource result = nextResource;
            nextResource = seek();

            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Resource seek() {
            while (pathIndex < paths.length) {
                String path = paths[pathIndex];
                pathIndex++;

                Resource res = resolver.getResource(baseResource, path);
                if (res != null) {
                    return res;
                }
            }

            // no more elements in the array
            return null;
        }
    }

}
