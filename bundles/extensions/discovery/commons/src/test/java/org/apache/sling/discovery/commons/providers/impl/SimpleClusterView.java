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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

public class SimpleClusterView implements ClusterView {

    private final String id;
    private List<InstanceDescription> instances = new LinkedList<InstanceDescription>();

    public SimpleClusterView(String id) {
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    public void addInstanceDescription(InstanceDescription id) {
        instances.add(id);
    }

    public boolean removeInstanceDescription(InstanceDescription id) {
        return instances.remove(id);
    }

    @Override
    public List<InstanceDescription> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    @Override
    public InstanceDescription getLeader() {
        for (InstanceDescription instanceDescription : instances) {
            if (instanceDescription.isLeader()) {
                return instanceDescription;
            }
        }
        throw new IllegalStateException("no leader");
    }

}
