/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.samples.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample that demonstrates how to implement asynchronous health checks
 *  that run in the background at regular intervals.
 *  The execute() method stays fast as it just reads a pre-computed value. 
 */
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true)
@Properties({
    @Property(name=HealthCheck.NAME),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY),
    @Property(name=HealthCheck.MBEAN_NAME),
    
    // Period *must* be a Long
    @Property(name="scheduler.period", longValue=AsyncHealthCheckSample.PERIOD_SECONDS, propertyPrivate=true),
    // Concurrent=false avoids reentrant calls to run()
    @Property(name="scheduler.concurrent", boolValue=false)
})
@Service(value={HealthCheck.class,Runnable.class})
public class AsyncHealthCheckSample implements HealthCheck, Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AtomicInteger counter = new AtomicInteger();
    
    public static final int PERIOD_SECONDS = 5;
    
    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        final int value = counter.get();
        resultLog.debug("{} - counter value is {}", this, value);
        if(value % 2 != 0) {
            resultLog.warn("Counter value ({}) is not even", value);
        }
        return new Result(resultLog);
    }
 
    /** Called by the Sling scheduler, every {@link #SCHEDULER_PERIOD} seconds, without
     *  reentrant calls, as configured by our scheduler.* service properties.
     *  
     *  Simulates an expensive operation by waiting a random time up to twice that period
     *  before incrementing our counter.
     */
    @Override
    public void run() {
        final long toWait = (long)(Math.random() * 2 * PERIOD_SECONDS);
        log.info("{} - Waiting {} seconds to simulate an expensive operation...", this, toWait);
        try {
            Thread.sleep(toWait * 1000L);
        } catch(InterruptedException iex) {
            log.warn("Sleep interrupted", iex);
        }
        counter.incrementAndGet();
        log.info("{} - counter set to {}", this, counter.get());
    }
}