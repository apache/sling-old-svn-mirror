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
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Sling Post Operation implementation for deleting one or more users and/or groups from the 
 * jackrabbit UserManager.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sling/user" values.1="sling/group" values.2="sling/userManager"
 * @scr.property name="sling.servlet.methods" value="POST" 
 * @scr.property name="sling.servlet.selectors" value="delete" 
 */
public class DeleteAuthorizableServlet extends AbstractAuthorizablePostServlet {
	private static final long serialVersionUID = 5874621724096106496L;

	/* (non-Javadoc)
	 * @see org.apache.sling.jackrabbit.usermanager.post.AbstractAuthorizablePostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
	 */
	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes)
			throws RepositoryException {

        Iterator<Resource> res = getApplyToResources(request);
        if (res == null) {
            Resource resource = request.getResource();
            Authorizable item = resource.adaptTo(Authorizable.class);
            if (item == null) {
  	            String msg = "Missing source " + resource.getPath() + " for delete";
  	            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND, msg);
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
	
	
    /**
     * Returns an iterator on <code>Resource</code> instances addressed in the
     * {@link SlingPostConstants#RP_APPLY_TO} request parameter. If the request
     * parameter is not set, <code>null</code> is returned. If the parameter
     * is set with valid resources an empty iterator is returned. Any resources
     * addressed in the {@link SlingPostConstants#RP_APPLY_TO} parameter is
     * ignored.
     *
     * @param request The <code>SlingHttpServletRequest</code> object used to
     *            get the {@link SlingPostConstants#RP_APPLY_TO} parameter.
     * @return The iterator of resources listed in the parameter or
     *         <code>null</code> if the parameter is not set in the request.
     */
    protected Iterator<Resource> getApplyToResources(
            SlingHttpServletRequest request) {

        String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
        if (applyTo == null) {
            return null;
        }

        return new ApplyToIterator(request, applyTo);
    }

    private static class ApplyToIterator implements Iterator<Resource> {

        private final ResourceResolver resolver;
        private final Resource baseResource;
        private final String[] paths;

        private int pathIndex;

        private Resource nextResource;

        ApplyToIterator(SlingHttpServletRequest request, String[] paths) {
            this.resolver = request.getResourceResolver();
            this.baseResource = request.getResource();
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
