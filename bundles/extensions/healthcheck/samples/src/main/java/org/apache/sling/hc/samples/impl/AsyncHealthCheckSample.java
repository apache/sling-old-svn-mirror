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
        configurationPolicy=ConfigurationPolicy.REQUIRE,
        metatype=true)
@Service
public class AsyncHealthCheckSample implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(AsyncHealthCheckSample.class);
    private static final long start = System.currentTimeMillis();
    
    @Override
    public Result execute() {
        
        final long value = secondsFromStart();
        log.info("{} - counter set to {}", this, value);
        
        final FormattingResultLog resultLog = new FormattingResultLog();

        resultLog.info("{} - counter value set to {} at {}", this, value, new Date());
        if(value % 2 != 0) {
            // Generate various states as examples
            final String template = "Counter value ({}) is not {} at {}";
            if(value % 3 == 0) {
                resultLog.critical(template, value, "a multiple of 3 (critical)", new Date());
            } else if(value % 5 == 0) {
                resultLog.healthCheckError(template, value, "a multiple of 5 (healthCheckError)", new Date());
            } else {
                resultLog.warn(template, value, "even (warn)", new Date());
            }
        }
        return new Result(resultLog);
    }
    
    private long secondsFromStart() {
        return (System.currentTimeMillis() - start) / 1000L;
    }
 
}