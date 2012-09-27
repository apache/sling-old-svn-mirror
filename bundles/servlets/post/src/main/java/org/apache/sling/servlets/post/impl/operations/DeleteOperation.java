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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.VersioningConfiguration;

/**
 * The <code>DeleteOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_DELETE delete}
 * operation for the Sling default POST servlet.
 */
public class DeleteOperation extends AbstractPostOperation {

    @Override
    protected void doRun(final SlingHttpServletRequest request,
                    final PostResponse response,
                    final List<Modification> changes)
    throws RepositoryException {
        final VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

        final Iterator<Resource> res = getApplyToResources(request);
        if (res == null) {

            final Resource resource = request.getResource();
            final Node node = resource.adaptTo(Node.class);
            if ( node != null ) {
                checkoutIfNecessary(node.getParent(), changes, versioningConfiguration);

                node.remove();
            } else {
                try {
                    request.getResourceResolver().delete(resource);
                } catch (final PersistenceException pe) {
                    if ( pe.getCause() instanceof RepositoryException ) {
                        throw (RepositoryException)pe.getCause();
                    }
                    throw new RepositoryException(pe);
                }
            }
            changes.add(Modification.onDeleted(resource.getPath()));

        } else {

            while (res.hasNext()) {
                final Resource resource = res.next();
                final Node node = resource.adaptTo(Node.class);
                if ( node != null ) {
                    checkoutIfNecessary(node.getParent(), changes, versioningConfiguration);
                    node.remove();
                } else {
                    try {
                        request.getResourceResolver().delete(resource);
                    } catch (final PersistenceException pe) {
                        if ( pe.getCause() instanceof RepositoryException ) {
                            throw (RepositoryException)pe.getCause();
                        }
                        throw new RepositoryException(pe);
                    }
                }
                changes.add(Modification.onDeleted(resource.getPath()));
            }

        }

    }
}