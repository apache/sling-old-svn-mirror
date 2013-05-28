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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * Default implementation of the ClusterView interface.
 * <p>
 */
public class DefaultClusterViewImpl implements ClusterView {

    /** the id of this cluster view **/
    private final String id;

    /** the list of instances as part of this cluster **/
    private final List<InstanceDescription> instances = new LinkedList<InstanceDescription>();

    public DefaultClusterViewImpl(final String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id must not be null");
        }
        this.id = id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof DefaultClusterViewImpl)) {
            return false;
        }
        final DefaultClusterViewImpl other = (DefaultClusterViewImpl) obj;
        if (!this.id.equals(other.id)) {
            return false;
        }
        if (!this.getLeader().equals(other.getLeader())) {
            return false;
        }
        if (this.instances.size() != other.instances.size()) {
            return false;
        }
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            if (!other.instances.contains(instance)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getId() {
        return id;
    }

    /**
     * Add the given instance to this cluster and set the cluster on the instance (back pointer)
     * @param instance the instance to add to this cluster
     */
    public void addInstanceDescription(final DefaultInstanceDescriptionImpl instance) {
        if (instances.contains(instance)) {
            throw new IllegalArgumentException("cannot add same instance twice");
        }
        if (instance.isLeader() && doGetLeader() != null) {
            throw new IllegalArgumentException(
                    "cannot add another leader. there already is one");
        }
        instances.add(instance);
        instance.setClusterView(this);
    }

    public List<InstanceDescription> getInstances() {
        if (instances.size() == 0) {
            throw new IllegalStateException("no instance was ever added");
        }
        return Collections.unmodifiableList(instances);
    }

    public InstanceDescription getLeader() {
        final InstanceDescription result = doGetLeader();
        if (result != null) {
            return result;
        }
        throw new IllegalStateException("no leader was added");
    }

    /**
     * Lookup the leader of this cluster
     * @return the leader of this cluster - should never return null
     */
    private InstanceDescription doGetLeader() {
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription anInstance = it.next();
            if (anInstance.isLeader()) {
                return anInstance;
            }
        }
        return null;
    }

}
