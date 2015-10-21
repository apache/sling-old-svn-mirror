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
package org.apache.sling.discovery.commons.providers.spi.base;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 'sync-token' part of the ConsistencyService,
 * but not the 'wait while backlog' part (which is left to subclasses
 * if needed).
 */
@Component(immediate = false)
@Service(value = { ConsistencyService.class })
public class SyncTokenOnlyConsistencyService extends BaseSyncTokenConsistencyService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    protected DiscoveryLiteConfig commonsConfig;

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    protected SlingSettingsService settingsService;

    protected String slingId;

    protected long syncTokenTimeoutMillis;
    
    protected long syncTokenIntervalMillis;

    public static BaseSyncTokenConsistencyService testConstructorAndActivate(
            DiscoveryLiteConfig commonsConfig,
            ResourceResolverFactory resourceResolverFactory,
            SlingSettingsService settingsService) {
        BaseSyncTokenConsistencyService service = testConstructor(commonsConfig, resourceResolverFactory, settingsService);
        service.activate();
        return service;
    }
    
    public static BaseSyncTokenConsistencyService testConstructor(
            DiscoveryLiteConfig commonsConfig,
            ResourceResolverFactory resourceResolverFactory,
            SlingSettingsService settingsService) {
        SyncTokenOnlyConsistencyService service = new SyncTokenOnlyConsistencyService();
        if (commonsConfig == null) {
            throw new IllegalArgumentException("commonsConfig must not be null");
        }
        if (resourceResolverFactory == null) {
            throw new IllegalArgumentException("resourceResolverFactory must not be null");
        }
        if (settingsService == null) {
            throw new IllegalArgumentException("settingsService must not be null");
        }
        service.commonsConfig = commonsConfig;
        service.resourceResolverFactory = resourceResolverFactory;
        service.syncTokenTimeoutMillis = commonsConfig.getBgTimeoutMillis();
        service.syncTokenIntervalMillis = commonsConfig.getBgIntervalMillis();
        service.settingsService = settingsService;
        return service;
    }

    @Override
    protected DiscoveryLiteConfig getCommonsConfig() {
        return commonsConfig;
    }

    @Override
    protected ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    @Override
    protected SlingSettingsService getSettingsService() {
        return settingsService;
    }
    
}
