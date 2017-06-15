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

import java.security.Principal;
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
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jackrabbit.usermanager.CreateGroup;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * <p>
 * Sling Post Servlet implementation for creating a group in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Creates a new group. Maps on to nodes of resourceType <code>sling/groups</code> like
 * <code>/rep:system/rep:userManager/rep:groups</code> mapped to a resource url
 * <code>/system/userManager/group</code>. This servlet responds at
 * <code>/system/userManager/group.create.html</code>
 * </p>
 * <h3>Methods</h3>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h3>Post Parameters</h3>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new group (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h3>Response</h3>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h3>Example</h3>
 * 
 * <code>
 * curl -F:name=newGroupA  -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html
 * </code>
 * 
 * <h4>Notes</h4>
 */

@Component(service = {Servlet.class, CreateGroup.class},
property = {
		   "sling.servlet.resourceTypes=sling/groups",
		   "sling.servlet.methods=POST",
		   "sling.servlet.selectors=create",
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=EEE MMM dd yyyy HH:mm:ss 'GMT'Z", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss.SSSZ", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd'T'HH:mm:ss", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=yyyy-MM-dd", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy HH:mm:ss", 
		   AbstractAuthorizablePostServlet.PROP_DATE_FORMAT + "=dd.MM.yyyy"
})
public class CreateGroupServlet extends AbstractGroupPostServlet implements CreateGroup {
    private static final long serialVersionUID = -1084915263933901466L;

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

        Session session = request.getResourceResolver().adaptTo(Session.class);
        String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        Group group = createGroup(session, 
                principalName, 
                request.getRequestParameterMap(), 
                changes);

        String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + group.getID();
        response.setPath(groupPath);
        response.setLocation(externalizePath(request, groupPath));
        response.setParentLocation(externalizePath(request,
            AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
        
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.CreateGroup#createGroup(javax.jcr.Session, java.lang.String, java.util.Map, java.util.List)
     */
    public Group createGroup(Session jcrSession, final String name,
            Map<String, ?> properties, List<Modification> changes)
            throws RepositoryException {
        // check that the parameter values have valid values.
        if (jcrSession == null) {
            throw new IllegalArgumentException("JCR Session not found");
        }

        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Group name was not supplied");
        }

        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);

        Group group = null;
        if (authorizable != null) {
            // principal already exists!
            throw new RepositoryException(
                "A group already exists with the requested name: "
                    + name);
        } else {
            group = userManager.createGroup(new Principal() {
                public String getName() {
                    return name;
                }
            });

            String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                + group.getID();
            
            Collection<RequestProperty> reqProperties = collectContent(properties);
            changes.add(Modification.onCreated(groupPath));

            // write content from form
            writeContent(jcrSession, group, reqProperties, changes);

            // update the group memberships
            ResourceResolver resourceResolver = null;
            try {
                //create a resource resolver to resolve the relative paths used for group membership values
            	final Map<String, Object> authInfo = new HashMap<String, Object>();
            	authInfo.put(org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION, jcrSession);
                resourceResolver = resourceResolverFactory.getResourceResolver(authInfo);
                Resource baseResource = resourceResolver.getResource(AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH);
                updateGroupMembership(baseResource, properties, group, changes);
            } catch (LoginException e) {
				throw new RepositoryException(e);
			} finally {
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }
        
        return group;
    }
    
}
