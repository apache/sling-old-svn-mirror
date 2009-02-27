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
package org.apache.sling.jackrabbit.usermanager.post;

import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;

/**
 * Sling Post Operation implementation for deleting users and/or groups from the 
 * jackrabbit UserManager.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostOperation"
 * @scr.property name="sling.post.operation" value="deleteAuthorizable"
 */
public class DeleteAuthorizableOperation extends AbstractAuthorizableOperation {

	/* (non-Javadoc)
	 * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void doRun(SlingHttpServletRequest request,
			HtmlResponse response, List<Modification> changes)
			throws RepositoryException {

        Iterator<Resource> res = getApplyToResources(request);
        if (res == null) {
            Resource resource = request.getResource();
            Authorizable item = resource.adaptTo(Authorizable.class);
            if (item == null) {
  	            String msg = "Missing source " + resource.getPath() + " for delete";
                response.setStatus(HttpServletResponse.SC_NOT_FOUND, msg);
               	throw new ResourceNotFoundException(msg);
            }

            item.remove();
            changes.add(Modification.onDeleted(resource.getPath()));
        } else {
            while (res.hasNext()) {
                Resource resource = res.next();
                Authorizable item = resource.adaptTo(Authorizable.class);
                if (item != null) {
                    item.remove();
                    changes.add(Modification.onDeleted(resource.getPath()));
                }
            }
        }
	}
}
