/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.impl.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.discovery.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for a view stored in the repository.
 */
public class View {

    /**
     * use static logger to avoid frequent initialization as is potentially the
     * case with ClusterViewResource.
     **/
    private final static Logger logger = LoggerFactory.getLogger(View.class);

    /** the underlying resource which represents this view **/
    private final Resource resource;

    public View(final Resource resource) {
        this.resource = resource;
    }

    /**
     * Returns the underlying resource of this view.
     * @return the underlying resource of this view
     */
    public Resource getResource() {
        return resource;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        } else if (!(obj instanceof View)) {
            return false;
        }
        View other = (View) obj;
        return getResource().getPath().equals(other.getResource().getPath());
    }

    @Override
    public int hashCode() {
        return getResource().getPath().hashCode();
    }

    /**
     * Returns the id of this view.
     * @return the id of this view
     */
    public String getViewId() {
        return getResource().getName();
    }

    /**
     * Checks whether this view matches the 'live view' as represented in the clusterInstances resource
     * @param clusterInstancesRes the clusterInstances resource against which to check
     * @return
     */
    public boolean matchesLiveView(final Resource clusterInstancesRes, final Config config) {
        return matches(ViewHelper.determineLiveInstances(clusterInstancesRes,
                config));
    }

    /**
     * Compare this view with the given set of slingIds
     * @param view a set of slingIds against which to compare this view
     * @return true if this view matches the given set of slingIds
     */
    public boolean matches(final Set<String> view) {
        final Set<String> viewCopy = new HashSet<String>(view);
        final Resource members = getResource().getChild("members");
        if (members == null) {
            return false;
        }
        final Iterator<Resource> it = members.getChildren().iterator();
        while (it.hasNext()) {
            Resource aMemberRes = it.next();

            if (!viewCopy.remove(aMemberRes.getName())) {
                return false;
            }
        }
        // now the ViewCopy set must be empty to represent a match
        return (viewCopy.size() == 0);
    }

    /**
     * Delete this view from the repository
     */
    public void remove() {
        final Node myNode = getResource().adaptTo(Node.class);
        Session session = null;
        try {
            session = myNode.getSession();
            myNode.remove();
            session.save();
            session = null;
        } catch (RepositoryException e) {
            logger.error("remove: Could not remove node: " + e, e);
        } finally {
            if (session != null) {
                try {
                    session.refresh(false);
                } catch (RepositoryException e1) {
                    logger.error("remove: Could not refresh session: " + e1, e1);
                }
            }
        }
    }

}
