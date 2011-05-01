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
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

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
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:name</dt>
 * <dd>The name of the new group (required)</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success, a redirect is sent to the group resource locator. The redirect comes with
 * HTML describing the status.</dd>
 * <dt>500</dt>
 * <dd>Failure, including group already exists. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example</h4>
 * 
 * <code>
 * curl -F:name=newGroupA  -Fproperty1=value1 http://localhost:8080/system/userManager/group.create.html
 * </code>
 * 
 * <h4>Notes</h4>
 * 
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/groups"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="create"
 */
public class CreateGroupServlet extends AbstractGroupPostServlet {
    private static final long serialVersionUID = -1084915263933901466L;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet
     * #handleOperation(org.apache.sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
     */
    @Override
    protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse response, List<Modification> changes)
            throws RepositoryException {

        // check that the submitted parameter values have valid values.
        final String principalName = request.getParameter(SlingPostConstants.RP_NODE_NAME);
        if (principalName == null || principalName.length() == 0) {
            throw new RepositoryException("Group name was not submitted");
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        if (session == null) {
            throw new RepositoryException("JCR Session not found");
        }

        UserManager userManager = AccessControlUtil.getUserManager(session);
        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable != null) {
            // principal already exists!
            throw new RepositoryException(
                "A principal already exists with the requested name: "
                    + principalName);
        } else {
            Group group = userManager.createGroup(new Principal() {
                public String getName() {
                    return principalName;
                }
            });

            String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                + group.getID();
            
            Map<String, RequestProperty> reqProperties = collectContent(
                request, response, groupPath);
            response.setPath(groupPath);
            response.setLocation(externalizePath(request, groupPath));
            response.setParentLocation(externalizePath(request,
                AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PATH));
            changes.add(Modification.onCreated(groupPath));

            // write content from form
            writeContent(session, group, reqProperties, changes);

            // update the group memberships
            updateGroupMembership(request, group, changes);
        }
    }
}
