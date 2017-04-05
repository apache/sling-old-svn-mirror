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
package org.apache.sling.discovery.commons.providers;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * Default implementation of the ClusterView interface.
 * <p>
 * Besides implementing the interface methods it also
 * adds add/remove of InstanceDescriptions as well as 
 * implementing equals and hashCode.
 */
public class DefaultClusterView implements ClusterView {

    /** the id of this cluster view **/
    private final String id;

    /** the list of instances as part of this cluster **/
    private final List<InstanceDescription> instances = new LinkedList<InstanceDescription>();

    public DefaultClusterView(final String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id must not be null");
        }
        this.id = id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof DefaultClusterView)) {
            return false;
        }
        final DefaultClusterView other = (DefaultClusterView) obj;
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
    public String toString() {
        if (instances.size() == 0) {
            return "a ClusterView[no instances]";
        } else if (instances.size() == 1) {
            return "a ClusterView[1 instance: "+instances.get(0).getSlingId()+"]";
        } else {
            StringBuffer sb = new StringBuffer();
            for (InstanceDescription id : instances) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(id.getSlingId());
            }
            return "a ClusterView[" + instances.size() + " instances: " + sb.toString() + "]";
        }
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
    public void addInstanceDescription(final DefaultInstanceDescription instance) {
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

    /**
     * Removes the given instance from this cluster.
     * <p>
     * Note that the instance will still have a pointer to this cluster however.
     * @param instance the instance to remove from this cluster
     */
    public boolean removeInstanceDescription(InstanceDescription instance) {
        return instances.remove(instance);
    }
    
    /**
     * Returns the local InstanceDescription or null if no local instance is listed
     * @return the local InstanceDescription or null if no local instance is listed
     * @throws IllegalStateException if multiple local instances are listed
     */
    public InstanceDescription getLocalInstance() {
        InstanceDescription local = null;
        for (Iterator<InstanceDescription> it = getInstances().iterator(); 
                it.hasNext();) {
            InstanceDescription instance = it.next();
            if (instance.isLocal()) {
                if (local!=null) {
                    throw new IllegalStateException("found multiple local instances!?");
                }
                local = instance;
                break;
            }
        }
        return local;
    }

}
