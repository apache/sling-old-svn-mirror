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

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.base.commons.BaseViewChecker;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.settings.SlingSettingsService;

public class DummyViewChecker extends BaseViewChecker {
    
    protected SlingSettingsService slingSettingsService;

    protected ResourceResolverFactory resourceResolverFactory;

    protected ConnectorRegistry connectorRegistry;

    protected AnnouncementRegistry announcementRegistry;

    protected Scheduler scheduler;

    protected BaseConfig connectorConfig;

    public static DummyViewChecker testConstructor(
            SlingSettingsService slingSettingsService,
            ResourceResolverFactory resourceResolverFactory,
            ConnectorRegistry connectorRegistry,
            AnnouncementRegistry announcementRegistry,
            Scheduler scheduler,
            BaseConfig connectorConfig) {
        DummyViewChecker pinger = new DummyViewChecker();
        pinger.slingSettingsService = slingSettingsService;
        pinger.resourceResolverFactory = resourceResolverFactory;
        pinger.connectorRegistry = connectorRegistry;
        pinger.announcementRegistry = announcementRegistry;
        pinger.scheduler = scheduler;
        pinger.connectorConfig = connectorConfig;
        return pinger;
    }

    @Override
    protected SlingSettingsService getSlingSettingsService() {
        return slingSettingsService;
    }

    @Override
    protected ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    @Override
    protected ConnectorRegistry getConnectorRegistry() {
        return connectorRegistry;
    }

    @Override
    protected AnnouncementRegistry getAnnouncementRegistry() {
        return announcementRegistry;
    }

    @Override
    protected Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected BaseConfig getConnectorConfig() {
        return connectorConfig;
    }

    @Override
    protected void updateProperties() {
        // nothing done for the dummyViewChecker
    }
}
