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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

    class HistoryEntry {
        BaseTopologyView view;
        String msg;
        String fullLine;
    }
    
    /** the date format used in the truncated log of topology events **/
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    protected String slingId;

    protected List<HistoryEntry> history = new LinkedList<HistoryEntry>();
    
    protected abstract DiscoveryLiteConfig getCommonsConfig();

    protected abstract ResourceResolverFactory getResourceResolverFactory();

    protected abstract SlingSettingsService getSettingsService();
    
    /** Get or create a ResourceResolver **/
    protected ResourceResolver getResourceResolver() throws LoginException {
        return getResourceResolverFactory().getAdministrativeResourceResolver(null);
    }
    
    @Override
    public void cancelSync() {
        cancelPreviousBackgroundCheck();
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
                final String localClusterSyncTokenId = view.getLocalClusterSyncTokenId();
                if (!storeMySyncToken(localClusterSyncTokenId)) {
                    // if anything goes wrong above, then this will mean for the others
                    // that they will have to wait until the timeout hits
                    
                    // so to try to avoid this, retry storing my sync token later:
                    addHistoryEntry(view, "storing my syncToken ("+localClusterSyncTokenId+")");
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
            StringBuffer historyEntry = new StringBuffer();
            for (InstanceDescription instance : view.getLocalInstance().getClusterView().getInstances()) {
                Object currentValue = syncTokens.get(instance.getSlingId());
                if (currentValue == null) {
                    String msg = "no syncToken yet of "+instance.getSlingId();
                    logger.info("seenAllSyncTokens: " + msg);
                    if (historyEntry.length() != 0) {
                        historyEntry.append(",");
                    }
                    historyEntry.append(msg);
                    success = false;
                } else if (!syncToken.equals(currentValue)) {
                    String msg = "syncToken of " + instance.getSlingId()
                                                + " is " + currentValue
                                                + " waiting for " + syncToken;
                    logger.info("seenAllSyncTokens: " + msg);
                    if (historyEntry.length() != 0) {
                        historyEntry.append(",");
                    }
                    historyEntry.append(msg);
                    success = false;
                }
            }
            if (!success) {
                logger.info("seenAllSyncTokens: not yet seen all expected syncTokens (see above for details)");
                addHistoryEntry(view, historyEntry.toString());
                return false;
            } else {
                addHistoryEntry(view, "seen all syncTokens");
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
    
    public List<String> getSyncHistory() {
        List<HistoryEntry> snapshot;
        synchronized(history) {
            snapshot = Collections.unmodifiableList(history);
        }
        List<String> result = new ArrayList<String>(snapshot.size());
        for (HistoryEntry historyEntry : snapshot) {
            result.add(historyEntry.fullLine);
        }
        return result;
    }

    protected void addHistoryEntry(BaseTopologyView view, String msg) {
        synchronized(history) {
            for(int i = history.size() - 1; i>=0; i--) {
                HistoryEntry entry = history.get(i);
                if (!entry.view.equals(view)) {
                    // don't filter if the view starts differing,
                    // only filter for the last few entries where
                    // the view is equal
                    break;
                }
                if (entry.msg.equals(msg)) {
                    // if the view is equal and the msg matches
                    // then this is a duplicate entry, so ignore
                    return;
                }
            }
            String fullLine = sdf.format(Calendar.getInstance().getTime()) + ": " + msg;
            HistoryEntry newEntry = new HistoryEntry();
            newEntry.view = view;
            newEntry.fullLine = fullLine;
            newEntry.msg = msg;
            history.add(newEntry);
            while (history.size() > 12) {
                history.remove(0);
            }
        }
    }

}
