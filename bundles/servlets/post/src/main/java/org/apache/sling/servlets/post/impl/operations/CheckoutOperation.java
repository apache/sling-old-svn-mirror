/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.servlets.post.impl.operations;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;

/**
 * The <code>CheckoutOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_CHECKOUT checkout}
 * operation for the Sling default POST servlet.
 * The checkout operation depends on the resources being backed up by a JCR node.
 */
public class CheckoutOperation extends AbstractPostOperation {
    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes)
            throws PersistenceException {
        try {
            Iterator<Resource> res = getApplyToResources(request);
            if (res == null) {

                Resource resource = request.getResource();
                Node node = resource.adaptTo(Node.class);
                if (node == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                        "Missing source " + resource + " for checkout");
                    return;
                }

                node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
                changes.add(Modification.onCheckout(resource.getPath()));

            } else {

                while (res.hasNext()) {
                    Resource resource = res.next();
                    Node node = resource.adaptTo(Node.class);
                    if (node != null) {
                        node.getSession().getWorkspace().getVersionManager().checkout(node.getPath());
                        changes.add(Modification.onCheckout(resource.getPath()));
                    }
                }

            }
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    /**
     * Checkout operation is always skipping checkin.
     */
    @Override
    protected boolean isSkipCheckin(SlingHttpServletRequest request) {
        return true;
    }
}
