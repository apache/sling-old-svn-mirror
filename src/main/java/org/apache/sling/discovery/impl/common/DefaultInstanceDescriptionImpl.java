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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.common.resource.ResourceHelper;

/**
 * Base implementation for the InstanceDescription interface.
 * <p>
 * Allows creation of the object with clusterview and/or properties null - to be
 * set later but before usage!
 * <p>
 */
public class DefaultInstanceDescriptionImpl implements InstanceDescription {

    /** the cluster view of which this instance is part of **/
    private ClusterView clusterView;

    /** whether this instance is the leader in the cluster **/
    private boolean isLeader;

    /** whether this instance is the local/own one **/
    private boolean isLocal;

    /** the sling id of this instance **/
    private String slingId;

    /** the properties of this instance **/
    private Map<String, String> properties;

    public DefaultInstanceDescriptionImpl(final DefaultClusterViewImpl clusterView,
            final boolean isLeader, final boolean isOwn, final String slingId,
            final Map<String, String> properties) {
        // slingId must not be null - clusterView and properties can be though
        if (slingId == null || slingId.length() == 0) {
            throw new IllegalArgumentException("slingId must not be null");
        }
        this.isLeader = isLeader;
        this.isLocal = isOwn;
        this.slingId = slingId;
        this.properties = filterValidProperties(properties);
        if (clusterView != null) {
            clusterView.addInstanceDescription(this);
            if (this.clusterView == null) {
                throw new IllegalStateException(
                        "clusterView should have been set by now");
            }
        }
    }

    @Override
    public String toString() {
    	final String clusterInfo;
    	if (clusterView==null) {
    		clusterInfo = "";
    	} else {
    		clusterInfo = ", clusterViewId="+clusterView.getId();
    	}
        return "an InstanceDescription[slindId=" + slingId + ", isLeader="
                + isLeader + ", isOwn=" + isLocal + clusterInfo + ", properties=" + this.properties + "]";
    }

    @Override
    public int hashCode() {
        return slingId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof DefaultInstanceDescriptionImpl)) {
            return false;
        }
        final DefaultInstanceDescriptionImpl other = (DefaultInstanceDescriptionImpl) obj;
        if (!this.slingId.equals(other.slingId)) {
            return false;
        }
        if (!this.slingId.equals(other.slingId)) {
            return false;
        }
        if (!properties.equals(other.properties)) {
            return false;
        }
        if (!this.getClusterView().getId()
                .equals(other.getClusterView().getId())) {
            return false;
        }
        return true;
    }

    public ClusterView getClusterView() {
        if (clusterView == null) {
            throw new IllegalStateException("clusterView was never set");
        }
        return clusterView;
    }

    /**
     * Sets the cluster on this instance
     * @param clusterView
     */
    protected void setClusterView(ClusterView clusterView) {
        if (this.clusterView != null) {
            throw new IllegalStateException("can only set clusterView once");
        }
        if (clusterView == null) {
            throw new IllegalArgumentException("clusterView must not be null");
        }
        this.clusterView = clusterView;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public String getSlingId() {
        return slingId;
    }

    public String getProperty(final String name) {
        if (properties == null) {
            throw new IllegalStateException("properties were never set");
        }
        return properties.get(name);
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            throw new IllegalStateException("properties were never set");
        }
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Sets the properties of this instance
     * @param properties
     */
    protected void setProperties(final Map<String, String> properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.properties = filterValidProperties(properties);
    }

    /** SLING-2883 : filter (pass-through) valid properties only **/
	private Map<String, String> filterValidProperties(
			Map<String, String> rawProps) {
		if (rawProps==null) {
			return null;
		}

		final HashMap<String, String> filteredProps = new HashMap<String, String>();
		final Set<Entry<String, String>> entries = rawProps.entrySet();
		final Iterator<Entry<String, String>> it = entries.iterator();
		while(it.hasNext()) {
			final Entry<String, String> anEntry = it.next();
			if (ResourceHelper.isValidPropertyName(anEntry.getKey())) {
				filteredProps.put(anEntry.getKey(), anEntry.getValue());
			}
		}
		return filteredProps;
	}
}
