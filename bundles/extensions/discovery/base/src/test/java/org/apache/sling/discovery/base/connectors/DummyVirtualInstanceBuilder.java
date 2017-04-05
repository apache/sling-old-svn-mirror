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
package org.apache.sling.discovery.base.connectors;

import org.apache.sling.discovery.base.commons.BaseDiscoveryService;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.DummyDiscoveryService;
import org.apache.sling.discovery.base.commons.ViewChecker;
import org.apache.sling.discovery.base.its.setup.ModifiableTestBaseConfig;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.DummyViewChecker;
import org.apache.sling.discovery.base.its.setup.mock.MockFactory;
import org.apache.sling.discovery.base.its.setup.mock.SimpleClusterViewService;
import org.apache.sling.discovery.base.its.setup.mock.SimpleConnectorConfig;

public class DummyVirtualInstanceBuilder extends VirtualInstanceBuilder {

    private ModifiableTestBaseConfig connectorConfig;

    public DummyVirtualInstanceBuilder() {
    }

    @Override
    public VirtualInstanceBuilder createNewRepository() throws Exception {
        this.factory = MockFactory.mockResourceResolverFactory();
        return this;
    }

    @Override
    public VirtualInstanceBuilder setPath(String string) {
        // nothing to do now
        return this;
    }
    
    @Override
    public Object[] getAdditionalServices(VirtualInstance instance) throws Exception {
        return null;
    }
    
    protected ClusterViewService createClusterViewService() {
        return new SimpleClusterViewService(getSlingId());
    }

    protected ViewChecker createViewChecker() throws Exception {
        return DummyViewChecker.testConstructor(getSlingSettingsService(), getResourceResolverFactory(), getConnectorRegistry(), getAnnouncementRegistry(), getScheduler(), getConnectorConfig());
    }

    protected BaseDiscoveryService createDiscoveryService() throws Exception {
        return new DummyDiscoveryService(getSlingId(), getClusterViewService(), getAnnouncementRegistry(), getResourceResolverFactory(), getConnectorConfig(), getConnectorRegistry(), getScheduler());
    }

    @Override
    public ModifiableTestBaseConfig getConnectorConfig() {
        if (connectorConfig==null) {
            connectorConfig = createConnectorConfig();
        }
        return connectorConfig;
    }

    private ModifiableTestBaseConfig createConnectorConfig() {
        return new SimpleConnectorConfig();
    }
    
    @Override
    protected void resetRepo() {
        // does nothing
    }
}
