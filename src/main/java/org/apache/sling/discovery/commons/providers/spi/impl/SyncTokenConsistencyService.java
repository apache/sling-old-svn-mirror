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
package org.apache.sling.discovery.commons.providers.spi.impl;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.apache.sling.discovery.commons.providers.spi.ConsistencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 'sync-token' part of the ConsistencyService,
 * but not the 'wait while backlog' part (which is left to subclasses
 * if needed).
 */
public class SyncTokenConsistencyService implements ConsistencyService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SYNCTOKEN_PATH = "/var/discovery/commons/synctokens";

    private static final String DEFAULT_RESOURCE_TYPE = "sling:Folder";

    protected static Resource getOrCreateResource(
            final ResourceResolver resourceResolver, final String path)
            throws PersistenceException {
        return ResourceUtil.getOrCreateResource(resourceResolver, path,
                DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, true);
    }

    private final class BackgroundCheckRunnable implements Runnable {
        private final Runnable callback;
        private final BackgroundCheck check;
        private final long timeoutMillis;
        private volatile boolean cancelled;
        private final String threadName;
        
        // for testing only:
        private final Object waitObj = new Object();
        private int waitCnt;
        private volatile boolean done;

        private BackgroundCheckRunnable(Runnable callback, 
                BackgroundCheck check, long timeoutMillis, String threadName) {
            this.callback = callback;
            this.check = check;
            this.timeoutMillis = timeoutMillis;
            this.threadName = threadName;
        }

        @Override
        public void run() {
            logger.debug("backgroundCheck.run: start");
            long start = System.currentTimeMillis();
            try{
                while(!cancelled()) {
                    if (check.check()) {
                        if (callback != null) {
                            callback.run();
                        }
                        return;
                    }
                    if (timeoutMillis != -1 && 
                            (System.currentTimeMillis() > start + timeoutMillis)) {
                        if (callback == null) {
                            logger.info("backgroundCheck.run: timeout hit (no callback to invoke)");
                        } else {
                            logger.info("backgroundCheck.run: timeout hit, invoking callback.");
                            callback.run();
                        }
                        return;
                    }
                    logger.debug("backgroundCheck.run: waiting another sec.");
                    synchronized(waitObj) {
                        waitCnt++;
                        try {
                            waitObj.notify();
                            waitObj.wait(1000);
                        } catch (InterruptedException e) {
                            logger.info("backgroundCheck.run: got interrupted");
                        }
                    }
                }
                logger.debug("backgroundCheck.run: this run got cancelled. {}", check);
            } catch(RuntimeException re) {
                logger.error("backgroundCheck.run: RuntimeException: "+re, re);
                // nevertheless calling runnable.run in this case
                if (callback != null) {
                    logger.info("backgroundCheck.run: RuntimeException -> invoking callback");
                    callback.run();
                }
                throw re;
            } catch(Error er) {
                logger.error("backgroundCheck.run: Error: "+er, er);
                // not calling runnable.run in this case!
                // since Error is typically severe
                logger.info("backgroundCheck.run: NOT invoking callback");
                throw er;
            } finally {
                logger.debug("backgroundCheck.run: end");
                synchronized(waitObj) {
                    done = true;
                    waitObj.notify();
                }
            }
        }
        
        boolean cancelled() {
            return cancelled;
        }

        void cancel() {
            logger.info("cancel: "+threadName);
            cancelled = true;
        }

        public void triggerCheck() {
            synchronized(waitObj) {
                int waitCntAtStart = waitCnt;
                waitObj.notify();
                while(!done && waitCnt<=waitCntAtStart) {
                    try {
                        waitObj.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("got interrupted");
                    }
                }
            }
        }
    }

    interface BackgroundCheck {
        
        boolean check();
        
    }
    
    protected final ResourceResolverFactory resourceResolverFactory;

    protected final String slingId;

    private final long syncTokenTimeoutMillis;

    protected BackgroundCheckRunnable backgroundCheckRunnable;
    
    public SyncTokenConsistencyService(ResourceResolverFactory resourceResolverFactory,
            String slingId, long syncTokenTimeoutMillis) {
        if (resourceResolverFactory == null) {
            throw new IllegalArgumentException("resourceResolverFactory must not be null");
        }
        if (slingId == null || slingId.length() == 0) {
            throw new IllegalArgumentException("slingId must not be null or empty: "+slingId);
        }
        this.slingId = slingId;
        this.resourceResolverFactory = resourceResolverFactory;
        this.syncTokenTimeoutMillis = syncTokenTimeoutMillis;
    }
    
    protected void cancelPreviousBackgroundCheck() {
        BackgroundCheckRunnable current = backgroundCheckRunnable;
        if (current!=null) {
            current.cancel();
            // leave backgroundCheckRunnable field as is
            // as that does not represent a memory leak
            // nor is it a problem to invoke cancel multiple times
            // but properly synchronizing on just setting backgroundCheckRunnable
            // back to null is error-prone and overkill
        }
    }
    
    protected void startBackgroundCheck(String threadName, final BackgroundCheck check, final Runnable callback, final long timeoutMillis) {
        // cancel the current one if it's still running
        cancelPreviousBackgroundCheck();
        
        if (check.check()) {
            // then we're not even going to start the background-thread
            // we're already done
            logger.info("backgroundCheck: already done, backgroundCheck successful, invoking callback");
            callback.run();
            return;
        }
        logger.info("backgroundCheck: spawning background-thread for '"+threadName+"'");
        backgroundCheckRunnable = new BackgroundCheckRunnable(callback, check, timeoutMillis, threadName);
        Thread th = new Thread(backgroundCheckRunnable);
        th.setName(threadName);
        th.setDaemon(true);
        th.start();
    }
    
    /** Get or create a ResourceResolver **/
    protected ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
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
        }, callback, syncTokenTimeoutMillis);
    }

    private void storeMySyncToken(String syncTokenId) throws LoginException, PersistenceException {
        logger.trace("storeMySyncToken: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            final Resource resource = getOrCreateResource(resourceResolver, SYNCTOKEN_PATH);
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

    private boolean seenAllSyncTokens(BaseTopologyView view) {
        logger.trace("seenAllSyncTokens: start");
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            Resource resource = getOrCreateResource(resourceResolver, SYNCTOKEN_PATH);
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

    /** for testing only! **/
    protected void triggerBackgroundCheck() {
        BackgroundCheckRunnable backgroundOp = backgroundCheckRunnable;
        if (backgroundOp!=null) {
            backgroundOp.triggerCheck();
        }
    }
}
