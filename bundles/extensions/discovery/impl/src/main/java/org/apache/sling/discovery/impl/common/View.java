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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.impl.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for a view stored in the repository.
 */
public class View {

    protected static final String VIEW_PROPERTY_CLUSTER_ID = "clusterId";
    protected static final String VIEW_PROPERTY_CLUSTER_ID_DEFINED_AT = "clusterIdDefinedAt";
    protected static final String VIEW_PROPERTY_CLUSTER_ID_DEFINED_BY = "clusterIdDefinedBy";
    
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
    	final ValueMap props = getResource().adaptTo(ValueMap.class);
    	final String clusterId = props.get(VIEW_PROPERTY_CLUSTER_ID, String.class);
    	if (clusterId != null && clusterId.length() > 0) {
    		return clusterId;
    	} else {
    		return getResource().getName();
    	}
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
        try{
	        final Iterator<Resource> it = members.getChildren().iterator();
	        while (it.hasNext()) {
	            Resource aMemberRes = it.next();
	
	            if (!viewCopy.remove(aMemberRes.getName())) {
	                return false;
	            }
	        }
        } catch(RuntimeException re) {
        	// SLING-2945 : the members resource could have been deleted
        	//              by another party simultaneously
        	//              so treat this situation nicely
        	logger.info("matches: cannot compare due to "+re);
        	return false;
        }
        // now the ViewCopy set must be empty to represent a match
        return (viewCopy.size() == 0);
    }

    /**
     * Delete this view from the repository
     */
    public void remove() {
        final ResourceResolver resourceResolver = getResource().getResourceResolver();
        try{
            resourceResolver.delete(getResource());
            resourceResolver.commit();
        } catch(PersistenceException pe) {
            logger.error("remove: Could not remove node: " + pe, pe);
            resourceResolver.refresh();
        }
    }

}
