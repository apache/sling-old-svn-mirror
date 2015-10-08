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
package org.apache.sling.discovery.commons.providers.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;

public class SimpleTopologyView extends BaseTopologyView {

    private List<InstanceDescription> instances = new LinkedList<InstanceDescription>();

    private final String id;

    public SimpleTopologyView() {
        id = UUID.randomUUID().toString();
    }
    
    public SimpleTopologyView(String id) {
        this.id = id;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleTopologyView)) {
            return false;
        }
        final SimpleTopologyView other = (SimpleTopologyView) obj;
        if (this==other) {
            return true;
        }
        if (!id.equals(other.id)) {
            return false;
        }
        if (this.instances.size()!=other.instances.size()) {
            return false;
        }
        for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
            InstanceDescription instanceDescription = (InstanceDescription) it
                    .next();
            boolean found = false;
            for (Iterator<?> it2 = other.instances.iterator(); it2
                    .hasNext();) {
                InstanceDescription otherId = (InstanceDescription) it2
                        .next();
                if (instanceDescription.equals(otherId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int c=0;
        for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
            InstanceDescription instanceDescription = (InstanceDescription) it
                    .next();
            c+=instanceDescription.hashCode();
        }
        return c;
    }

    public void addInstanceDescription(InstanceDescription id) {
        instances.add(id);
    }
    
    @Override
    public InstanceDescription getLocalInstance() {
        InstanceDescription result = null;
        for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
            InstanceDescription instanceDescription = (InstanceDescription) it
                    .next();
            if (instanceDescription.isLocal()) {
                if (result!=null) {
                    throw new IllegalStateException("multiple local instances");
                }
                result = instanceDescription;
            }
        }
        if (result==null) {
            throw new IllegalStateException("no local instance found");
        }
        return result;
    }

    @Override
    public Set<InstanceDescription> getInstances() {
        return new HashSet<InstanceDescription>(instances);
    }

    @Override
    public Set<InstanceDescription> findInstances(InstanceFilter filter) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public Set<ClusterView> getClusterViews() {
        Set<ClusterView> clusters = new HashSet<ClusterView>();
        for (InstanceDescription instanceDescription : instances) {
            clusters.add(instanceDescription.getClusterView());
        }
        return clusters;
    }

    @Override
    public String getLocalClusterSyncTokenId() {
        return id;
    }

    public SimpleTopologyView addInstance() {
        final String slingId = UUID.randomUUID().toString();
        final String clusterId = UUID.randomUUID().toString();
        final SimpleClusterView cluster = new SimpleClusterView(clusterId);
        final SimpleInstanceDescription instance = new SimpleInstanceDescription(true, true, slingId, null);
        instance.setClusterView(cluster);
        cluster.addInstanceDescription(instance);
        instances.add(instance);
        return this;
    }

    public SimpleTopologyView addInstance(String slingId, SimpleClusterView cluster, boolean isLeader, boolean isLocal) {
        final SimpleInstanceDescription instance = new SimpleInstanceDescription(isLeader, isLocal, slingId, null);
        instance.setClusterView(cluster);
        cluster.addInstanceDescription(instance);
        instances.add(instance);
        return this;
    }

    public SimpleTopologyView addInstance(InstanceDescription artefact) {
        final String slingId = artefact.getSlingId();
        final boolean isLeader = artefact.isLeader();
        final boolean isLocal = artefact.isLocal();
        SimpleClusterView cluster = (SimpleClusterView) artefact.getClusterView();
        final SimpleInstanceDescription instance = new SimpleInstanceDescription(isLeader, isLocal, slingId, artefact.getProperties());
        instance.setClusterView(cluster);
        cluster.addInstanceDescription(instance);
        instances.add(instance);
        return this;
    }

    public SimpleTopologyView removeInstance(String slingId) {
        for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
            InstanceDescription id = (InstanceDescription) it.next();
            if (id.getSlingId().equals(slingId)) {
                it.remove();
                SimpleClusterView cluster = (SimpleClusterView) id.getClusterView();
                if (!cluster.removeInstanceDescription(id)) {
                    throw new IllegalStateException("could not remove id: "+id);
                }
                return this;
            }
        }
        throw new IllegalStateException("instance not found: "+slingId);
    }

    public static SimpleTopologyView clone(final SimpleTopologyView view) {
        final SimpleTopologyView result = new SimpleTopologyView(view.id);
        final Iterator<InstanceDescription> it = view.getInstances().iterator();
        Map<String,SimpleClusterView> clusters = new HashMap<String, SimpleClusterView>();
        while(it.hasNext()) {
            InstanceDescription id = it.next();
            SimpleInstanceDescription clone = clone(id);
            String clusterId = id.getClusterView().getId();
            SimpleClusterView cluster = clusters.get(clusterId);
            if (cluster==null) {
                cluster = new SimpleClusterView(clusterId);
                clusters.put(clusterId, cluster);
            }
            clone.setClusterView(cluster);
            result.addInstance(clone);
        }
        if (!view.isCurrent()) {
            result.setNotCurrent();
        }
        return result;
    }
    
    private static SimpleInstanceDescription clone(InstanceDescription id) {
        return new SimpleInstanceDescription(id.isLeader(), id.isLocal(), id.getSlingId(), id.getProperties());
    }

    public SimpleTopologyView clone() {
        return SimpleTopologyView.clone(this);
    }
}
