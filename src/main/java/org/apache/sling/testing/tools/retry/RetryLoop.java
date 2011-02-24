/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.retry;

import static org.junit.Assert.fail;

/** Convenience class for retrying tests
 *  until timeout or success.
 */
public class RetryLoop {
    
    private final long timeout;
    
    /** Interface for conditions to check, isTrue will be called
     *  repeatedly until success or timeout */
    static public interface Condition {
        /** Used in failure messages to describe what was expected */
        String getDescription();
        
        /** If true we stop retrying. The RetryLoop retries on AssertionError, 
         *  so if tests fail in this method they are not reported as 
         *  failures but retried.
         */
        boolean isTrue() throws Exception;
    }
    
    /** Retry Condition c until it returns true or timeout. See {@link Condition}
     *  for isTrue semantics.
     */
    public RetryLoop(Condition c, int timeoutSeconds, int intervalBetweenTriesMsec) {
        timeout = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while(System.currentTimeMillis() < timeout) {
            try {
                if(c.isTrue()) {
                    return;
                }
            } catch(AssertionError ae) {
                // Retry JUnit tests failing in the condition as well
                reportException(ae);
            } catch(Exception e) {
                reportException(e);
            }
            
            try {
                Thread.sleep(intervalBetweenTriesMsec);
            } catch(InterruptedException ignore) {
            }
        }
    
        onTimeout();
        fail("RetryLoop failed, condition is false after " + timeoutSeconds + " seconds: " 
                + c.getDescription());
    }

    /** Can be overridden to report Exceptions that happen in the retry loop */
    protected void reportException(Throwable t) {
    }
    
    /** Called if the loop times out without success, just before failing */
    protected void onTimeout() {
    }
    
    protected long getRemainingTimeSeconds() {
        return Math.max(0L, (timeout - System.currentTimeMillis()) / 1000L);
    }
}