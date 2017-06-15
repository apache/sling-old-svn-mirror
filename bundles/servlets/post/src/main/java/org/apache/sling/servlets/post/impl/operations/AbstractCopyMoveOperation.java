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

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;

/**
 * The <code>AbstractCopyMoveOperation</code> is the abstract base close for
 * the {@link CopyOperation} and {@link MoveOperation} classes implementing
 * common behavior.
 */
abstract class AbstractCopyMoveOperation extends AbstractPostOperation {

    @Override
    protected final void doRun(final SlingHttpServletRequest request,
            final PostResponse response,
            final List<Modification> changes)
    throws PersistenceException {
        final VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

        final Resource resource = request.getResource();
        final String source = resource.getPath();

        // ensure dest is not empty/null and is absolute
        String dest = request.getParameter(SlingPostConstants.RP_DEST);
        if (dest == null || dest.length() == 0) {
            throw new IllegalArgumentException("Unable to process "
                    + getOperationName() + ". Missing destination");
        }

        // register whether the path ends with a trailing slash
        final boolean trailingSlash = dest.endsWith("/");

        // ensure destination is an absolute and normalized path
        if (!dest.startsWith("/")) {
            dest = ResourceUtil.getParent(source) + "/" + dest;
        }
        dest = ResourceUtil.normalize(dest);

        // destination parent and name
        final String dstParent = trailingSlash ? dest : ResourceUtil.getParent(dest);

        // delete destination if already exists
        if (!trailingSlash && request.getResourceResolver().getResource(dest) != null ) {

            final String replaceString = request.getParameter(SlingPostConstants.RP_REPLACE);
            final boolean isReplace = "true".equalsIgnoreCase(replaceString);
            if (!isReplace) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED,
                    "Cannot " + getOperationName() + " " + resource + " to "
                        + dest + ": destination exists");
                return;
            } else {
                this.jcrSsupport.checkoutIfNecessary(request.getResourceResolver().getResource(dstParent),
                        changes, versioningConfiguration);
            }

        } else {

            // check if path to destination exists and create it, but only
            // if it's a descendant of the current node
            if (!dstParent.equals("")) {
                final Resource parentResource = request.getResourceResolver().getResource(dstParent);
                if (parentResource != null ) {
                    this.jcrSsupport.checkoutIfNecessary(parentResource, changes, versioningConfiguration);
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

        final Iterator<Resource> resources = getApplyToResources(request);
        final Resource destResource;
        if (resources == null) {

            final String dstName = trailingSlash ? null : ResourceUtil.getName(dest);
            destResource = execute(changes, resource, dstParent, dstName, versioningConfiguration);

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
                final Resource applyTo = resources.next();
                execute(changes, applyTo, dstParent, null, versioningConfiguration);
            }
            destResource = request.getResourceResolver().getResource(dest);

        }

        if ( destResource == null ) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    "Missing source " + resource + " for " + getOperationName());
            return;
        }
        // finally apply the ordering parameter
        this.jcrSsupport.orderNode(request, destResource, changes);
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
     * @throws PersistenceException May be thrown if an error occurs executing
     *             the operation.
     */
    protected abstract Resource execute(List<Modification> changes,
            Resource source,
            String destParent,
            String destName,
            VersioningConfiguration versioningConfiguration)
    throws PersistenceException;

}
