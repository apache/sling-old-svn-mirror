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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class which implements the concept of a 'BackgroundCheck',
 * a thread that periodically executes a check until that one succeeds.
 * <p>
 */
public abstract class AbstractServiceWithBackgroundCheck {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String slingId;

    /**
     * The BackgroundCheckRunnable implements the details of
     * calling BackgroundCheck.check and looping until it 
     * returns true
     */
    final class BackgroundCheckRunnable implements Runnable {
        private final Runnable callback;
        private final BackgroundCheck check;
        private final long timeoutMillis;
        private volatile boolean cancelled;
        private final String threadName;
        
        // for testing only:
        private final Object waitObj = new Object();
        private int waitCnt;
        private volatile boolean done;
        private long waitInterval;

        private BackgroundCheckRunnable(Runnable callback, 
                BackgroundCheck check, long timeoutMillis, long waitInterval, String threadName) {
            this.callback = callback;
            this.check = check;
            this.timeoutMillis = timeoutMillis;
            if (waitInterval <= 0) {
                throw new IllegalArgumentException("waitInterval must be greater than 0: "+waitInterval);
            }
            this.waitInterval = waitInterval;
            this.threadName = threadName;
        }
        
        boolean isDone() {
            synchronized(waitObj) {
                return done;
            }
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
                    logger.trace("backgroundCheck.run: waiting another sec.");
                    synchronized(waitObj) {
                        waitCnt++;
                        try {
                            waitObj.notify();
                            waitObj.wait(waitInterval);
                        } catch (InterruptedException e) {
                            logger.debug("backgroundCheck.run: got interrupted");
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
            if (!done) {
                logger.info("cancel: "+threadName);
            }
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

    /**
     * A BackgroundCheck is anything that can be periodically
     * checked until it eventually returns true.
     */
    interface BackgroundCheck {
        
        boolean check();
        
    }
    
    protected BackgroundCheckRunnable backgroundCheckRunnable;
    
    /**
     * Cancel the currently ongoing background check if
     * there is any ongoing.
     */
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
    
    /**
     * Start a new BackgroundCheck in a separate thread, that
     * periodically calls BackgroundCheck.check and upon completion
     * calls the provided callback.run()
     * @param threadName the name of the thread (to allow identifying the thread)
     * @param check the BackgroundCheck to periodically invoke with check()
     * @param callback the Runnable to invoke upon a successful check()
     * @param timeoutMillis a timeout at which point the BackgroundCheck is
     * terminated and no callback is invoked. Note that this happens unnoticed
     * at the moment, ie there is no feedback about whether a background
     * check was successfully termianted (ie callback was invoked) or
     * whether the timeout has hit (that's left as a TODO if needed).
     */
    protected void startBackgroundCheck(String threadName, final BackgroundCheck check, final Runnable callback, final long timeoutMillis, final long waitMillis) {
        // cancel the current one if it's still running
        cancelPreviousBackgroundCheck();
        
        if (check.check()) {
            // then we're not even going to start the background-thread
            // we're already done
            if (callback!=null) {
                logger.info("backgroundCheck: already done, backgroundCheck successful, invoking callback");
                callback.run();
            } else {
                logger.info("backgroundCheck: already done, backgroundCheck successful. no callback to invoke.");
            }
            return;
        }
        logger.info("backgroundCheck: spawning background-thread for '"+threadName+"'");
        backgroundCheckRunnable = new BackgroundCheckRunnable(callback, check, timeoutMillis, waitMillis, threadName);
        Thread th = new Thread(backgroundCheckRunnable);
        th.setName(threadName);
        th.setDaemon(true);
        th.start();
    }
    

    /** for testing only! **/
    protected void triggerBackgroundCheck() {
        BackgroundCheckRunnable backgroundOp = backgroundCheckRunnable;
        if (backgroundOp!=null) {
            backgroundOp.triggerCheck();
        }
    }
}
