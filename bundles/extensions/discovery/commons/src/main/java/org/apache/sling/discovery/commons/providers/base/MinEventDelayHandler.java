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
package org.apache.sling.discovery.commons.providers.base;

import java.util.Date;
import java.util.concurrent.locks.Lock;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.commons.providers.BaseTopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** hooks into the ViewStateManagerImpl and adds a delay between
 * TOPOLOGY_CHANGING and TOPOLOGY_CHANGED - with the idea to avoid
 * bundle multiple TOPOLOGY_CHANGED events should they happen within
 * a very short amount of time.
 */
class MinEventDelayHandler {

    private final static Logger logger = LoggerFactory.getLogger(MinEventDelayHandler.class);

    private boolean isDelaying = false;

    private final Scheduler scheduler;

    private final long minEventDelaySecs;

    private DiscoveryService discoveryService;

    private ViewStateManagerImpl viewStateManager;

    private Lock lock;
    
    private volatile int cancelCnt = 0;
    
    MinEventDelayHandler(ViewStateManagerImpl viewStateManager, Lock lock,
            DiscoveryService discoveryService, Scheduler scheduler,
            long minEventDelaySecs) {
        this.viewStateManager = viewStateManager;
        this.lock = lock;
        if (discoveryService==null) {
            throw new IllegalArgumentException("discoveryService must not be null");
        }
        this.discoveryService = discoveryService;
        if (scheduler==null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }
        this.scheduler = scheduler;
        if (minEventDelaySecs<=0) {
            throw new IllegalArgumentException("minEventDelaySecs must be greater than 0 (is "+minEventDelaySecs+")");
        }
        this.minEventDelaySecs = minEventDelaySecs;
    }

    /**
     * Asks the MinEventDelayHandler to handle the new view
     * and return true if the caller shouldn't worry about any follow-up action -
     * only if the method returns false should the caller do the usual 
     * handleNewView action
     */
    boolean handlesNewView(BaseTopologyView newView) {
        if (isDelaying) {
            // already delaying, so we'll soon ask the DiscoveryServiceImpl for the
            // latest view and go ahead then
            logger.info("handleNewView: already delaying, ignoring new view meanwhile");
            return true;
        }
        
        if (!viewStateManager.hadPreviousView()) {
            logger.info("handlesNewView: never had a previous view, hence no delaying applicable");
            return false;
        }
        
        if (viewStateManager.onlyDiffersInProperties(newView)) {
            logger.info("handlesNewView: only properties differ, hence no delaying applicable");
            return false;
        }
        
        if (viewStateManager.unchanged(newView)) {
            // this will be the most frequent case
            // hence log only with trace
            logger.trace("handlesNewView: view is unchanged, hence no delaying applicable");
            return false;
        }
        
        // thanks to force==true this will always return true
        if (!triggerAsyncDelaying(newView)) {
            logger.info("handleNewView: could not trigger async delaying, sending new view now.");
            viewStateManager.handleNewViewNonDelayed(newView);
        } else {
            // if triggering the async event was successful, then we should also
            // ensure that we sent out a TOPOLOGY_CHANGING *before* that delayed event hits.
            //
            // and, we're still in lock.lock() - so we are safe to do a handleChanging() here
            // even though there is the very unlikely possibility that the async-delay-thread
            // would compete - but even if it would, thanks to the lock.lock() that would be safe.
            // so: we're going to do a handleChanging here:
            viewStateManager.handleChanging();
        }
        return true;
    }
    
    private boolean triggerAsyncDelaying(BaseTopologyView newView) {
        final int validCancelCnt = cancelCnt;
        final boolean triggered = runAfter(minEventDelaySecs /*seconds*/ , new Runnable() {
    
            public void run() {
                lock.lock();
                try{
                    if (cancelCnt!=validCancelCnt) {
                        logger.info("asyncDelay.run: got cancelled (validCancelCnt="+validCancelCnt+", cancelCnt="+cancelCnt+"), quitting.");
                        return;
                    }
                    
                    // unlock the CHANGED event for any subsequent call to handleTopologyChanged()
                    isDelaying = false;

                    // check if the new topology is already ready
                    TopologyView t = discoveryService.getTopology();
                    if (!(t instanceof BaseTopologyView)) {
                        logger.error("asyncDelay.run: done delaying. topology not of type BaseTopologyView: "+t);
                        // cannot continue in this case
                        return;
                    }
                    BaseTopologyView topology = (BaseTopologyView) t;
                    
                    if (topology.isCurrent()) {
                        logger.info("asyncDelay.run: done delaying. got new view: "+ topology.toShortString());
                        viewStateManager.handleNewViewNonDelayed(topology);
                    } else {
                        logger.info("asyncDelay.run: done delaying. new view (still/again) not current, delaying again");
                        triggerAsyncDelaying(topology);
                        // we're actually not interested in the result here
                        // if the async part failed, then we have to rely
                        // on a later handleNewView to come in - we can't
                        // really send a view now cos it is not current.
                        // so we're really stuck to waiting for handleNewView
                        // in this case.
                    }
                } catch(RuntimeException re) {
                    logger.error("RuntimeException: "+re, re);
                    throw re;
                } catch(Error er) {
                    logger.error("Error: "+er, er);
                    throw er;
                } finally {
                    lock.unlock();
                }
            }
        });
            
        logger.info("triggerAsyncDelaying: asynch delaying of "+minEventDelaySecs+" triggered: "+triggered);
        if (triggered) {
            isDelaying = true;
        }
        return triggered;
    }
    
    /**
     * run the runnable after the indicated number of seconds, once.
     * @return true if the scheduling of the runnable worked, false otherwise
     */
    private boolean runAfter(long seconds, final Runnable runnable) {
        final Scheduler theScheduler = scheduler;
        if (theScheduler == null) {
            logger.info("runAfter: no scheduler set");
            return false;
        }
        logger.trace("runAfter: trying with scheduler.fireJob");
        final Date date = new Date(System.currentTimeMillis() + seconds * 1000);
        try {
            theScheduler.fireJobAt(null, runnable, null, date);
            return true;
        } catch (Exception e) {
            logger.info("runAfter: could not schedule a job: "+e);
            return false;
        }
    }

    /** for testing only **/
    boolean isDelaying() {
        return isDelaying;
    }

    public void cancelDelaying() {
        logger.info("cancelDelaying: flagging cancelCnt as invalid: "+cancelCnt);
        cancelCnt++;
        isDelaying = false;
    }

}
