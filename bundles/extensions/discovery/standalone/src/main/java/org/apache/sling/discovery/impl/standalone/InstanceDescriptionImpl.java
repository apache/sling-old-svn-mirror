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
import java.util.Map;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

public class InstanceDescriptionImpl implements InstanceDescription {

    private final String id;

    private final Map<String, String> properties;

    public InstanceDescriptionImpl(final String id, final Map<String, String> properties) {
        this.id = id;
        this.properties = Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean isLeader() {
        return true;
    }

    @Override
    public String getSlingId() {
        return id;
    }

    @Override
    public String getProperty(final String name) {
        return properties.get(name);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public ClusterView getClusterView() {
        return new ClusterViewImpl(this);
    }
}
