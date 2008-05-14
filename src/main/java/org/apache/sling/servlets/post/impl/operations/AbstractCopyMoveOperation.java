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
package org.apache.sling.servlets.post.impl.operations;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Holds various states and encapsulates methods that are needed to handle a
 * post request.
 */
abstract class AbstractCopyMoveOperation extends AbstractSlingPostOperation {

    @Override
    public final void doRun(SlingHttpServletRequest request, HtmlResponse response)
            throws RepositoryException {

        Resource resource = request.getResource();
        String source = resource.getPath();
        
        // ensure we have an item underlying the request's resource
        Item item = resource.adaptTo(Item.class);
        if (item == null) {
            throw new ResourceNotFoundException("Missing source " + resource
                + " for " + getOperationName());
        }

        // ensure dest is not empty/null and is absolute
        String dest = request.getParameter(SlingPostConstants.RP_DEST);
        if (dest == null || dest.length() == 0) {
            throw new IllegalArgumentException("Unable to process "
                + getOperationName() + ". Missing destination");
        } if (!dest.startsWith("/")) {
            dest = ResourceUtil.getParent(source) + "/" + dest;
            dest = ResourceUtil.normalize(dest);
        }
        
        Session session = item.getSession();

        // delete destination if already exists
        String dstParent = ResourceUtil.getParent(dest);
        if (session.itemExists(dest)) {
            final String replaceString = request.getParameter(SlingPostConstants.RP_REPLACE);
            final boolean isReplace = "true".equalsIgnoreCase(replaceString);
            if (isReplace) {
                session.getItem(dest).remove();
            } else {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
                    "Cannot " + getOperationName() + " " + resource + " to "
                        + dest + ": destination exists");
                return;
            }
        } else {
            // check if path to destination exists and create it, but only
            // if it's a descendant of the current node
            if (!dstParent.equals("") && !session.itemExists(dstParent)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
                    "Cannot " + getOperationName() + " " + resource + " to "
                        + dest + ": parent of destination does not exist");
                return;
            }

            // the destination is newly created, hence a create request
            response.setCreateRequest(true);
        }

        execute(response, session, source, dest);
        orderNode(request, session.getItem(dest));
    }

    protected abstract String getOperationName();

    protected abstract void execute(HtmlResponse response, Session session,
            String source, String dest) throws RepositoryException;

}
