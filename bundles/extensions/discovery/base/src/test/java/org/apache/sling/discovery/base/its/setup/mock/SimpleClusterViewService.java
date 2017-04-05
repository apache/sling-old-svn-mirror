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
package org.apache.sling.discovery.base.its.setup.mock;

import java.util.HashMap;
import java.util.UUID;

import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.spi.LocalClusterView;

public class SimpleClusterViewService implements ClusterViewService {

    private LocalClusterView clusterView;
    
    private final String slingId;

    public SimpleClusterViewService(String slingId) {
        this.slingId = slingId;
        LocalClusterView clusterView = new LocalClusterView(UUID.randomUUID().toString(), null);
        new DefaultInstanceDescription(clusterView, true, true, slingId, new HashMap<String, String>());
        this.clusterView = clusterView;
    }
    
    @Override
    public String getSlingId() {
        return slingId;
    }

    @Override
    public LocalClusterView getLocalClusterView() throws UndefinedClusterViewException {
        if (clusterView==null) {
            throw new IllegalStateException("no clusterView set");
        }
        return clusterView;
    }
    
    public void setClusterView(LocalClusterView clusterView) {
        this.clusterView = clusterView;
    }

}
