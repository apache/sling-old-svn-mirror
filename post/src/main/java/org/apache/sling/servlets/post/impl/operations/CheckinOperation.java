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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.AbstractPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostResponse;

/**
 * The <code>CheckinOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_CHECKIN checkin}
 * operation for the Sling default POST servlet.
 */
public class CheckinOperation extends AbstractPostOperation {

    @Override
    protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes)
            throws RepositoryException {
        Iterator<Resource> res = getApplyToResources(request);
        if (res == null) {

            Resource resource = request.getResource();
            Node node = resource.adaptTo(Node.class);
            if (node == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND,
                    "Missing source " + resource + " for checkout");
                return;
            }

            node.checkin();
            changes.add(Modification.onCheckin(resource.getPath()));

        } else {

            while (res.hasNext()) {
                Resource resource = res.next();
                Node node = resource.adaptTo(Node.class);
                if (node != null) {
                    node.checkin();
                    changes.add(Modification.onCheckin(resource.getPath()));
                }
            }

        }

    }

    /**
     * Checkin operation always checks in.
     */
    @Override
    protected boolean isSkipCheckin(SlingHttpServletRequest request) {
        return false;
    }
}
