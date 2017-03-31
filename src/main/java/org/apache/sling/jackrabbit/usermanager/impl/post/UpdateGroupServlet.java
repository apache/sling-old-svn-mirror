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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jackrabbit.usermanager.UpdateGroup;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * <p>
 * Sling Post Operation implementation for updating a group in the 
 * jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a group's properties. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups/ae/3f/ed/testGroup</code> mapped to a resource url
 * <code>/system/userManager/group/testGroup</code>. This servlet responds at
 * <code>/system/userManager/group/testGroup.update.html</code>
 * </p>
 * <h3>Methods</h3>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h3>Post Parameters</h3>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>The property is deleted, eg prop1@Delete</dd>
 * </dl>
 * <h3>Response</h3>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group's resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h3>Example</h3>
 * 
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/group/testGroup.update.html
 * </code>
 */

@Component(service = {Servlet.class, UpdateGroup.class},
property = {
		   "sling.servlet.resourceTypes=sling/group",
		   "sling.servlet.methods=POST",
		   "sling.servlet.selectors=update",
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=EEE MMM dd yyyy HH:mm:ss 'GMT'Z", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss.SSSZ", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy HH:mm:ss", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy"
})
public class UpdateGroupServlet extends AbstractGroupPostServlet 
        implements UpdateGroup {
    private static final long serialVersionUID = -8292054361992488797L;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Override
    @Activate
    protected void activate(final Map<String, Object> props) {
        super.activate(props);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

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
        Resource resource = request.getResource();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        updateGroup(session,
                        resource.getName(),
                        request.getRequestParameterMap(), 
                        changes);
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.UpdateGroup#updateGroup(javax.jcr.Session, java.lang.String, java.util.Map, java.util.List)
     */
    public Group updateGroup(Session jcrSession, 
                                String name,
                                Map<String, ?> properties, 
                                List<Modification> changes)
            throws RepositoryException {

        Group group = null;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof Group) {
            group = (Group)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "Group to update could not be determined");
        }
        
        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + group.getID();

        Collection<RequestProperty> reqProperties = collectContent(properties);
        try {
            // cleanup any old content (@Delete parameters)
            processDeletes(group, reqProperties, changes);

            // write content from form
            writeContent(jcrSession, group, reqProperties, changes);

            // update the group memberships
            ResourceResolver resourceResolver = null;
            try {
                //create a resource resolver to resolve the relative paths used for group membership values
            	final Map<String, Object> authInfo = new HashMap<String, Object>();
            	authInfo.put(org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION, jcrSession);
                resourceResolver = resourceResolverFactory.getResourceResolver(authInfo);
                Resource baseResource = resourceResolver.getResource(groupPath);
                updateGroupMembership(baseResource, properties, group, changes);
            } catch (LoginException e) {
				throw new RepositoryException(e);
			} finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        } catch (RepositoryException re) {
            throw new RepositoryException("Failed to update group.", re);
        }
        return group;
    }
    
}
