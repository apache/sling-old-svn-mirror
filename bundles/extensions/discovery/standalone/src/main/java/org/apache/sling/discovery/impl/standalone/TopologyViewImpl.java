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
package org.apache.sling.discovery.impl.standalone;

import java.util.Collections;
import java.util.Set;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyView;

public class TopologyViewImpl implements TopologyView {

    private volatile boolean current = true;

    private final InstanceDescription myInstance;

    public TopologyViewImpl(final InstanceDescription myInstance) {
        this.myInstance = myInstance;
    }

    @Override
    public InstanceDescription getLocalInstance() {
        return myInstance;
    }

    @Override
    public boolean isCurrent() {
        return current;
    }

    public void invalidate() {
        this.current = false;
    }

    @Override
    public Set<InstanceDescription> getInstances() {
        return Collections.singleton(this.myInstance);
    }

    @Override
    public Set<InstanceDescription> findInstances(final InstanceFilter picker) {
        if ( picker.accept(this.myInstance) ) {
            return getInstances();
        }
        return Collections.emptySet();
    }

    @Override
    public Set<ClusterView> getClusterViews() {
        final ClusterView clusterView = new ClusterViewImpl(myInstance);
        return Collections.singleton(clusterView);
    }
}
