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

import org.apache.felix.scr.annotations.Activate;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.apache.sling.discovery.commons.providers.util.ResourceHelper;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 'sync-token' part of the ConsistencyService,
 * but not the 'wait while backlog' part (which is left to subclasses
 * if needed).
 */
public abstract class BaseSyncTokenConsistencyService extends AbstractServiceWithBackgroundCheck implements ConsistencyService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String slingId;

    protected long syncTokenTimeoutMillis;
    
    protected long syncTokenIntervalMillis;

    protected abstract DiscoveryLiteConfig getCommonsConfig();

    protected abstract ResourceResolverFactory getResourceResolverFactory();

    protected abstract SlingSettingsService getSettingsService();
    
    @Activate
    protected void activate() {
        this.slingId = getSettingsService().getSlingId();
    }
    
    /** Get or create a ResourceResolver **/
    protected ResourceResolver getResourceResolver() throws LoginException {
        return getResourceResolverFactory().getAdministrativeResourceResolver(null);
    }
    
    @Override
    public void sync(BaseTopologyView view, Runnable callback) {
        // cancel the previous background-check if it's still running
        cancelPreviousBackgroundCheck();

        syncToken(view, callback);
        // this implementation doesn't support wait-for-backlog, so
        // the above doSyncTokenPart will already terminate with invoking the callback
    }

    protected void syncToken(final BaseTopologyView view, final Runnable callback) {
        // 1) first storing my syncToken
        try {
            storeMySyncToken(view.getLocalClusterSyncTokenId());
        } catch (LoginException e) {
            logger.error("syncToken: will run into timeout: could not login for storing my syncToken: "+e, e);
        } catch (PersistenceException e) {
            logger.error("syncToken: will run into timeout: got PersistenceException while storing my syncToken: "+e, e);
        }
        // if anything goes wrong above, then this will mean for the others
        // that they will have to wait until the timeout hits
        // which means we should do the same..
        // hence no further action possible on error above
        
        // 2) then check if all others have done the same already
        startBackgroundCheck("SyncTokenConsistencyService", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                return seenAllSyncTokens(view);
            }
        }, callback, syncTokenTimeoutMillis, syncTokenIntervalMillis);
    }

    private void storeMySyncToken(String syncTokenId) throws LoginException, PersistenceException {
        logger.trace("storeMySyncToken: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            final Resource resource = ResourceHelper.getOrCreateResource(resourceResolver, getSyncTokenPath());
            ModifiableValueMap syncTokens = resource.adaptTo(ModifiableValueMap.class);
            Object currentValue = syncTokens.get(slingId);
            if (currentValue == null || !syncTokenId.equals(currentValue)) {
                syncTokens.put(slingId, syncTokenId);
            }
            resourceResolver.commit();
            logger.info("syncToken: stored syncToken of slingId="+slingId+" as="+syncTokenId);
        } finally {
            logger.trace("storeMySyncToken: end");
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }

    private String getSyncTokenPath() {
        return getCommonsConfig().getSyncTokenPath();
    }

    private boolean seenAllSyncTokens(BaseTopologyView view) {
        logger.trace("seenAllSyncTokens: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            Resource resource = ResourceHelper.getOrCreateResource(resourceResolver, getSyncTokenPath());
            ValueMap syncTokens = resource.adaptTo(ValueMap.class);
            String syncToken = view.getLocalClusterSyncTokenId();
            
            for (InstanceDescription instance : view.getLocalInstance().getClusterView().getInstances()) {
                Object currentValue = syncTokens.get(instance.getSlingId());
                if (currentValue == null) {
                    logger.info("seenAllSyncTokens: no syncToken of "+instance);
                    return false;
                }
                if (!syncToken.equals(currentValue)) {
                    logger.info("seenAllSyncTokens: old syncToken of " + instance
                            + " : expected=" + syncToken + " got="+currentValue);
                    return false;
                }
            }
            
            resourceResolver.commit();
            logger.info("seenAllSyncTokens: seen all syncTokens!");
            return true;
        } catch (LoginException e) {
            logger.error("seenAllSyncTokens: could not login: "+e, e);
            return false;
        } catch (PersistenceException e) {
            logger.error("seenAllSyncTokens: got PersistenceException: "+e, e);
            return false;
        } finally {
            logger.trace("seenAllSyncTokens: end");
            if (resourceResolver!=null) {
                resourceResolver.close();
            }
        }
    }
}
