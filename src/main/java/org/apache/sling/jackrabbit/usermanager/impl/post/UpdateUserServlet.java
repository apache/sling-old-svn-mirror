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

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jackrabbit.usermanager.UpdateUser;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;

/**
 * <p>
 * Sling Post Operation implementation for updating a user in the jackrabbit UserManager.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Updates a users properties. Maps on to nodes of resourceType <code>sling/users</code> like
 * <code>/rep:system/rep:userManager/rep:users</code> mapped to a resource url
 * <code>/system/userManager/user/ieb</code>. This servlet responds at
 * <code>/system/userManager/user/ieb.update.html</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the user node (optional)</dd>
 * <dt>*@Delete</dt>
 * <dd>Delete the property eg prop3@Delete means prop3 will be deleted (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the users resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found</dd>
 * <dt>500</dt>
 * <dd>Failure</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -Fprop1=value2 -Fproperty1=value1 http://localhost:8080/system/userManager/user/ieb.update.html
 * </code>
 */
@Component (immediate=true, metatype=true,
		label="%updateUser.post.operation.name",
		description="%updateUser.post.operation.description")
@Service (value={
	Servlet.class,
	UpdateUser.class
})		
@Properties ({
	@Property (name="sling.servlet.resourceTypes",
			value="sling/user"),
	@Property (name="sling.servlet.methods",
			value="POST"),
	@Property (name="sling.servlet.selectors",
			value="update")
})
public class UpdateUserServlet extends AbstractUserPostServlet 
        implements UpdateUser {
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
        Resource resource = request.getResource();
        Session session = request.getResourceResolver().adaptTo(Session.class);
        updateUser(session,
                resource.getName(),
                request.getRequestParameterMap(), 
                changes);
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.jackrabbit.usermanager.UpdateUser#updateUser(javax.jcr.Session, java.lang.String, java.util.Map, java.util.List)
     */
    public User updateUser(Session jcrSession, String name,
            Map<String, ?> properties, List<Modification> changes)
            throws RepositoryException {
        
        User user;
        UserManager userManager = AccessControlUtil.getUserManager(jcrSession);
        Authorizable authorizable = userManager.getAuthorizable(name);
        if (authorizable instanceof User) {
            user = (User)authorizable;
        } else {
            throw new ResourceNotFoundException(
                "User to update could not be determined");
        }
        
        String userPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
            + user.getID();

        Map<String, RequestProperty> reqProperties = collectContent(properties, userPath);
        try {
            // cleanup any old content (@Delete parameters)
            processDeletes(user, reqProperties, changes);

            // write content from form
            writeContent(jcrSession, user, reqProperties, changes);
            
            //SLING-2072 set the user as enabled or disabled if the request
            // has supplied the relevant properties
            String disabledParam = convertToString(properties.get(":disabled"));
            if ("true".equalsIgnoreCase(disabledParam)) {
                //set the user as disabled
                String disabledReason = convertToString(properties.get(":disabledReason"));
                if (disabledReason == null) {
                    disabledReason = "";
                }
                user.disable(disabledReason);
            } else if ("false".equalsIgnoreCase(disabledParam)) {
                //re-enable a disabled user
                user.disable(null);
            }
        } catch (RepositoryException re) {
            throw new RepositoryException("Failed to update user.", re);
        }
        return user;
    }    
}
