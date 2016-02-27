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

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.annotations.SlingHealthCheck;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample Health Check that uses a cron expression to
 *  schedule regular async execution. 
 */

@SlingHealthCheck(
        configurationFactory=true,
        immediate = true, // to ensure same instance is kept between calls of HC (required to keep state in counter)
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        metatype=true)
@Service
public class AsyncHealthCheckSample implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(AsyncHealthCheckSample.class);
    
    private final AtomicInteger counter = new AtomicInteger();
    
    public static final int PERIOD_SECONDS = 5;
    
    @Override
    public Result execute() {
        
        final long toWait = (long)(Math.random() * 2 * PERIOD_SECONDS);
        log.info("{} - Waiting {} seconds to simulate an expensive operation...", this, toWait);
        try {
            Thread.sleep(toWait * 1000L);
        } catch(InterruptedException iex) {
            log.warn("Sleep interrupted", iex);
        }

        final int value =  counter.incrementAndGet();
        log.info("{} - counter set to {}", this, value);
        
        final FormattingResultLog resultLog = new FormattingResultLog();

        resultLog.info("{} - counter value set to {} at {}", this, value, new Date());
        if(value % 2 != 0) {
            resultLog.warn("Counter value ({}) is not even", value);
        }
        return new Result(resultLog);
    }
 
}