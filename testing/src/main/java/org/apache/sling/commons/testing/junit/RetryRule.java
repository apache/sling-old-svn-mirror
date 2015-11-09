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
package org.apache.sling.commons.testing.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JUnit Rule that implements retries, see tests for usage example */
public class RetryRule implements TestRule {
    
    public static final int DEFAULT_DEFAULT_TIMEOUT_MSEC = 5000;
    public static final int DEFAULT_DEFAULT_INTERVAL_MSEC = 500;
    
    private static final Logger log = LoggerFactory.getLogger(RetryRule.class);
    
    private final long defaultTimeout;
    private final long defaultInterval;

    /** Create a RetryRule with default values for default timeout and interval */
    public RetryRule() {
        this(DEFAULT_DEFAULT_TIMEOUT_MSEC, DEFAULT_DEFAULT_INTERVAL_MSEC);
    }
    
    /** Create a RetryRule with specific values for default timeout and interval */
    public RetryRule(long defaultTimeout, long defaultInterval) {
        this.defaultTimeout = defaultTimeout;
        this.defaultInterval = defaultInterval;
    }
    
    @Override
    public String toString() {
        return new StringBuilder()
        .append(getClass().getSimpleName())
        .append(", default interval=")
        .append(defaultInterval)
        .append(" msec, default timeout=")
        .append(defaultTimeout)
        .append(" msec")
        .toString();
    }
    
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            
            private Throwable eval(Statement stmt) {
                try {
                    stmt.evaluate();
                } catch(Throwable t) {
                    return t;
                }
                return null;
            }
            
            @Override
            public void evaluate() throws Throwable {
                int retries = 0;
                Throwable t = eval(statement);
                if(t != null) {
                    final Retry r = description.getAnnotation(Retry.class);
                    if(r != null) {
                        final long timeout = System.currentTimeMillis() + getTimeout(r.timeoutMsec());
                        while(System.currentTimeMillis() < timeout) {
                            retries++;
                            t = eval(statement);
                            if(t == null) {
                                break;
                            }
                            Thread.sleep(getInterval(r.intervalMsec()));
                        }
                    }
                }
                if(t != null) {
                    if(retries > 0) {
                        log.debug("{} fails after retrying {} time(s)", statement, retries);
                    }
                    throw t;
                }
                if(retries > 0) {
                    log.debug("{} succeeds after retrying {} time(s)", statement, retries);
                }
            }
        };
    }
    
    long getTimeout(long ruleTimeout) {
        return ruleTimeout > 0 ? ruleTimeout : defaultTimeout;
    }
    
    long getInterval(long ruleInterval) {
        return ruleInterval > 0 ? ruleInterval : defaultInterval;
    }
    
}