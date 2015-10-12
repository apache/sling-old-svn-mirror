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
package org.apache.sling.discovery.impl.topology;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the topology view
 */
public class TopologyViewImpl extends BaseTopologyView {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the instances that are part of this topology **/
    private final Set<InstanceDescription> instances = new HashSet<InstanceDescription>();

    /** Create a new empty topology **/
    public TopologyViewImpl() {
        // nothing to be initialized then
    }

    /** Create a new topology filled with the given list of instances **/
    public TopologyViewImpl(final Collection<InstanceDescription> instances) {
        if (instances != null) {
            this.instances.addAll(instances);
        }
    }

    /**
     * Compare this topology with the given one and determine how they compare
     * @param other the other topology against which to compare
     * @return the type describing how these two compare
     * @see Type
     */
    public Type compareTopology(final TopologyViewImpl other) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        if (this.instances.size() != other.instances.size()) {
        	logger.debug("compareTopology: different number of instances");
            return Type.TOPOLOGY_CHANGED;
        }
        boolean propertiesChanged = false;
        for(final InstanceDescription instance : this.instances) {

            final Iterator<InstanceDescription> it2 = other.instances.iterator();
            InstanceDescription matchingInstance = null;
            while (it2.hasNext()) {
                final InstanceDescription otherInstance = it2.next();
                if (instance.getSlingId().equals(otherInstance.getSlingId())) {
                    matchingInstance = otherInstance;
                    break;
                }
            }
            if (matchingInstance == null) {
            	if (logger.isDebugEnabled()) {
	            	logger.debug("compareTopology: no matching instance found for {}", instance);
            	}
                return Type.TOPOLOGY_CHANGED;
            }
            if (!instance.getClusterView().getId()
                    .equals(matchingInstance.getClusterView().getId())) {
            	logger.debug("compareTopology: cluster view id does not match");
                return Type.TOPOLOGY_CHANGED;
            }
            if (!instance.isLeader()==matchingInstance.isLeader()) {
                logger.debug("compareTopology: leaders differ");
                return Type.TOPOLOGY_CHANGED;
            }
            if (!instance.getProperties().equals(
                    matchingInstance.getProperties())) {
                propertiesChanged = true;
            }
        }
        if (propertiesChanged) {
            return Type.PROPERTIES_CHANGED;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof TopologyViewImpl)) {
            return false;
        }
        TopologyViewImpl other = (TopologyViewImpl) obj;
        if (this.isCurrent() != other.isCurrent()) {
            return false;
        }
        Type diff = compareTopology(other);
        return diff == null;
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            code += instance.hashCode();
        }
        return code;
    }

    /**
     * @see org.apache.sling.discovery.TopologyView#getLocalInstance()
     */
    public InstanceDescription getLocalInstance() {
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            if (instance.isLocal()) {
                return instance;
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.discovery.TopologyView#getInstances()
     */
    public Set<InstanceDescription> getInstances() {
        return Collections.unmodifiableSet(instances);
    }

    public void addInstances(final Collection<InstanceDescription> instances) {
        if (instances == null) {
            return;
        }
        outerLoop: for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instanceDescription = it.next();
            for (Iterator<InstanceDescription> it2 = this.instances.iterator(); it2.hasNext();) {
                InstanceDescription existingInstance = it2.next();
                if (existingInstance.getSlingId().equals(instanceDescription.getSlingId())) {
                    // SLING-3726:
                    // while 'normal duplicate instances' are filtered out here correctly,
                    // 'hidden duplicate instances' that are added via this instanceDescription's
                    // cluster, are not caught.
                    // there is, however, no simple fix for this. Since the reason is 
                    // inconsistent state information in /var/discovery/impl - either
                    // due to stale-announcements (SLING-4139) - or by some manualy
                    // copying of data from one cluster to the next (which will also
                    // be cleaned up by SLING-4139 though)
                    // so the fix for avoiding duplicate instances is really SLING-4139
                    logger.info("addInstance: cannot add same instance twice: "
                            + instanceDescription);
                    continue outerLoop;
                }
            }
            this.instances.add(instanceDescription);
        }
    }

    /**
     * @see org.apache.sling.discovery.TopologyView#findInstances(org.apache.sling.discovery.InstanceFilter)
     */
    public Set<InstanceDescription> findInstances(final InstanceFilter picker) {
        if (picker == null) {
            throw new IllegalArgumentException("picker must not be null");
        }
        Set<InstanceDescription> result = new HashSet<InstanceDescription>();
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            if (picker.accept(instance)) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.discovery.TopologyView#getClusterViews()
     */
    public Set<ClusterView> getClusterViews() {
        Set<ClusterView> result = new HashSet<ClusterView>();
        for (Iterator<InstanceDescription> it = instances.iterator(); it
                .hasNext();) {
            InstanceDescription instance = it.next();
            ClusterView cluster = instance.getClusterView();
            if (cluster != null) {
                result.add(cluster);
            }
        }
        return new HashSet<ClusterView>(result);
    }

    @Override
    public String toString() {
        return "TopologyViewImpl [current=" + isCurrent() + ", num=" + instances.size() + ", instances="
                + instances + "]";
    }

    @Override
    public String getLocalClusterSyncTokenId() {
        throw new IllegalStateException("no syncToken applicable");
    }
}
