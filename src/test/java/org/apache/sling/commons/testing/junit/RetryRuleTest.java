/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.testing.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RetryRuleTest {
    private int setupCounter;
    private int callCount = 0;
    private long callTime = -1;

    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    @Before
    public void setup() {
        setupCounter = 0;
    }
    
    @Retry
    @Test
    public void testDefaultParameters() {
        callCount++;
        setupCounter++;
        
        if(callTime > 0) {
            final long delta = System.currentTimeMillis();
            assertTrue("Expecting at least 500 msec between calls", delta >= 500);
        }
        callTime = System.currentTimeMillis();
        
        assertTrue("Expecting to be called several times before passing", callCount > 3);
        assertEquals("Expecting setup() to be called before every retry", 1, setupCounter);

        // Once we pass, reset counters for other tests
        callTime = -1;
        callCount = 0;
    }
    
    @Retry
    @Test
    public void testRetryOnException() throws Exception {
        callCount++;
        setupCounter++;

        if(callCount <= 3) {
            throw new Exception("Expecting to be called several times before passing");
        }
        
        assertEquals("Expecting setup() to be called before every retry", 1, setupCounter);

        // Once we pass, reset counters for other tests
        callTime = -1;
        callCount = 0;
    }
    
    @Retry(timeoutMsec=500, intervalMsec=1)
    @Test
    public void testCustomParameters() {
        callCount++;
        setupCounter++;
        
        assertTrue("Expecting to be called many times before passing", callCount > 100);
        assertEquals("Expecting setup() to be called before every retry", 1, setupCounter);
        
        // Once we pass, reset callTime for other tests
        callTime = -1;
        callCount = 0;
    }
    
    @Test
    public void testDefaultDefaultTimings() {
        final RetryRule r = new RetryRule();
        assertEquals("Expecting default default timeout", RetryRule.DEFAULT_DEFAULT_TIMEOUT_MSEC, r.getTimeout(-1));
        assertEquals("Expecting default default interval", RetryRule.DEFAULT_DEFAULT_INTERVAL_MSEC, r.getInterval(-1));
    }
    
    @Test
    public void testSpecifiedDefaultTimings() {
        final RetryRule r = new RetryRule(12, 42);
        assertEquals("Expecting specified default timeout", 12, r.getTimeout(-1));
        assertEquals("Expecting specified default interval", 42, r.getInterval(-1));
    }
    
    @Test
    public void testRuleTimings() {
        final RetryRule r = new RetryRule(12, 42);
        assertEquals("Expecting timeout from Rule", 1012, r.getTimeout(1012));
        assertEquals("Expecting interval from Rule", 1024, r.getInterval(1024));
    }
}
