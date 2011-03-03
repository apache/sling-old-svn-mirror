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

import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;

/**
 * The <code>AbstractCopyMoveOperation</code> is the abstract base close for
 * the {@link CopyOperation} and {@link MoveOperation} classes implementing
 * commong behaviour.
 */
abstract class AbstractCopyMoveOperation extends AbstractPostOperation {

    @Override
    protected final void doRun(SlingHttpServletRequest request,
            PostResponse response,
            List<Modification> changes)
    throws RepositoryException {
        Session session = request.getResourceResolver().adaptTo(Session.class);

        VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

        Resource resource = request.getResource();
        String source = resource.getPath();

        // ensure dest is not empty/null and is absolute
        String dest = request.getParameter(SlingPostConstants.RP_DEST);
        if (dest == null || dest.length() == 0) {
            throw new IllegalArgumentException("Unable to process "
                + getOperationName() + ". Missing destination");
        }

        // register whether the path ends with a trailing slash
        boolean trailingSlash = dest.endsWith("/");

        // ensure destination is an absolute and normalized path
        if (!dest.startsWith("/")) {
            dest = ResourceUtil.getParent(source) + "/" + dest;
        }
        dest = ResourceUtil.normalize(dest);
        dest = removeAndValidateWorkspace(dest, session);

        // destination parent and name
        String dstParent = trailingSlash ? dest : ResourceUtil.getParent(dest);

        // delete destination if already exists
        if (!trailingSlash && session.itemExists(dest)) {

            final String replaceString = request.getParameter(SlingPostConstants.RP_REPLACE);
            final boolean isReplace = "true".equalsIgnoreCase(replaceString);
            if (!isReplace) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
                    "Cannot " + getOperationName() + " " + resource + " to "
                        + dest + ": destination exists");
                return;
            } else {
                checkoutIfNecessary(session.getItem(dest).getParent(), changes, versioningConfiguration);
            }

        } else {

            // check if path to destination exists and create it, but only
            // if it's a descendant of the current node
            if (!dstParent.equals("")) {
                if (session.itemExists(dstParent)) {
                    checkoutIfNecessary((Node) session.getItem(dstParent), changes, versioningConfiguration);
                } else {
                    response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
                        "Cannot " + getOperationName() + " " + resource + " to "
                            + dest + ": parent of destination does not exist");
                    return;
                }
            }

            // the destination is newly created, hence a create request
            response.setCreateRequest(true);
        }

        Iterator<Resource> resources = getApplyToResources(request);
        Item destItem = null;
        if (resources == null) {

            // ensure we have an item underlying the request's resource
            Item item = resource.adaptTo(Item.class);
            if (item == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    "Missing source " + resource + " for " + getOperationName());
                return;
            }

            String dstName = trailingSlash ? null : ResourceUtil.getName(dest);
            destItem = execute(changes, item, dstParent, dstName, versioningConfiguration);

        } else {

            // multiple applyTo requires trailing slash on destination
            if (!trailingSlash) {
                throw new IllegalArgumentException(
                    "Applying "
                        + getOperationName()
                        + " to multiple resources requires a trailing slash on the destination");
            }

            // multiple copy will never return 201/CREATED
            response.setCreateRequest(false);

            while (resources.hasNext()) {
                Resource applyTo = resources.next();
                Item item = applyTo.adaptTo(Item.class);
                if (item != null) {
                    execute(changes, item, dstParent, null, versioningConfiguration);
                }
            }
            destItem = session.getItem(dest);

        }

        // finally apply the ordering parameter
        orderNode(request, destItem, changes);
    }

    /**
     * Returns a short name to be used in log and status messages.
     */
    protected abstract String getOperationName();

    /**
     * Actually executes the operation.
     *
     * @param response The <code>HtmlResponse</code> used to record success of
     *            the operation.
     * @param source The source item to act upon.
     * @param destParent The absolute path of the parent of the target item.
     * @param destName The name of the target item inside the
     *            <code>destParent</code>. If <code>null</code> the name of
     *            the <code>source</code> is used as the target item name.
     * @throws RepositoryException May be thrown if an error occurrs executing
     *             the operation.
     */
    protected abstract Item execute(List<Modification> changes, Item source,
            String destParent, String destName,
            VersioningConfiguration versioningConfiguration) throws RepositoryException;

}
