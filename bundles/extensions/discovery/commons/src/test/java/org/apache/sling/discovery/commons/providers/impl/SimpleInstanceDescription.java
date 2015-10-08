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
import java.util.Map;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

public class SimpleInstanceDescription implements InstanceDescription {

    private ClusterView clusterView;
    private final boolean isLeader;
    private final boolean isLocal;
    private final String slingId;
    private Map<String, String> properties;

    public SimpleInstanceDescription(boolean isLeader, boolean isLocal, String slingId,
            Map<String, String> properties) {
        this.isLeader = isLeader;
        this.isLocal = isLocal;
        this.slingId = slingId;
        this.properties = properties;
    }
    
    @Override
    public String toString() {
        return "Instance["+slingId+"]";
    }
    
    @Override
    public int hashCode() {
        return slingId.hashCode() + (isLeader?0:1) + (isLocal?0:1);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleInstanceDescription)) {
            return false;
        }
        SimpleInstanceDescription other = (SimpleInstanceDescription) obj;
        if (!slingId.equals(other.slingId)) {
            return false;
        }
        if (isLeader!=other.isLeader) {
            return false;
        }
        if (isLocal!=other.isLocal) {
            return false;
        }
        Map<String, String> myProperties = getProperties();
        Map<String, String> otherProperties = other.getProperties();
        return (myProperties.equals(otherProperties));
    }

    public void setClusterView(ClusterView clusterView) {
        this.clusterView = clusterView;
    }
    
    @Override
    public ClusterView getClusterView() {
        return clusterView;
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String getSlingId() {
        return slingId;
    }

    @Override
    public String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Map<String, String> getProperties() {
        if (properties==null) {
            return new HashMap<String, String>();
        }
        return new HashMap<String,String>(properties);
    }

    public void setProperty(String key, String value) {
        if (properties==null) {
            properties = new HashMap<String, String>();
        }
        properties.put(key, value);
    }

}
