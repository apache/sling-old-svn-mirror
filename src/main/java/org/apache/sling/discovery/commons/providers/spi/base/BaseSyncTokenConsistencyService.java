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

/**
 * Implements the 'sync-token' part of the ConsistencyService,
 * but not the 'wait while backlog' part (which is left to subclasses
 * if needed).
 */
public abstract class BaseSyncTokenConsistencyService extends AbstractServiceWithBackgroundCheck implements ConsistencyService {

    protected String slingId;

    protected abstract DiscoveryLiteConfig getCommonsConfig();

    protected abstract ResourceResolverFactory getResourceResolverFactory();

    protected abstract SlingSettingsService getSettingsService();
    
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
        
        startBackgroundCheck("SyncTokenConsistencyService", new BackgroundCheck() {
            
            @Override
            public boolean check() {
                // 1) first storing my syncToken
                if (!storeMySyncToken(view.getLocalClusterSyncTokenId())) {
                    // if anything goes wrong above, then this will mean for the others
                    // that they will have to wait until the timeout hits
                    
                    // so to try to avoid this, retry storing my sync token later:
                    return false;
                }
                
                
                // 2) then check if all others have done the same already
                return seenAllSyncTokens(view);
            }
        }, callback, getCommonsConfig().getBgTimeoutMillis(), getCommonsConfig().getBgIntervalMillis());
    }

    private boolean storeMySyncToken(String syncTokenId) {
        logger.trace("storeMySyncToken: start");
        if (slingId == null) {
            logger.info("storeMySyncToken: not yet activated (slingId is null)");
            return false;
        }
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            final Resource resource = ResourceHelper.getOrCreateResource(resourceResolver, getSyncTokenPath());
            ModifiableValueMap syncTokens = resource.adaptTo(ModifiableValueMap.class);
            boolean updateToken = false;
            if (!syncTokens.containsKey(slingId)) {
                updateToken = true;
            } else {
                Object existingToken = syncTokens.get(slingId);
                if (existingToken==null || !existingToken.equals(syncTokenId)) {
                    updateToken = true;
                }
            }
            if (updateToken) {
                syncTokens.put(slingId, syncTokenId);
                resourceResolver.commit();
                logger.info("storeMySyncToken: stored syncToken of slingId="+slingId+" as="+syncTokenId);
            } else {
                logger.info("storeMySyncToken: syncToken was left unchanged for slingId="+slingId+" at="+syncTokenId);
            }
            return true;
        } catch (LoginException e) {
            logger.error("storeMySyncToken: could not login for storing my syncToken: "+e, e);
            return false;
        } catch (PersistenceException e) {
            logger.error("storeMySyncToken: got PersistenceException while storing my syncToken: "+e, e);
            return false;
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
            
            boolean success = true;
            for (InstanceDescription instance : view.getLocalInstance().getClusterView().getInstances()) {
                Object currentValue = syncTokens.get(instance.getSlingId());
                if (currentValue == null) {
                    logger.info("seenAllSyncTokens: no syncToken of "+instance.getSlingId());
                    success = false;
                } else if (!syncToken.equals(currentValue)) {
                    logger.info("seenAllSyncTokens: old syncToken of " + instance.getSlingId()
                            + " : expected=" + syncToken + " got="+currentValue);
                    success = false;
                }
            }
            if (!success) {
                logger.info("seenAllSyncTokens: not yet seen all expected syncTokens (see above for details)");
                return false;
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
