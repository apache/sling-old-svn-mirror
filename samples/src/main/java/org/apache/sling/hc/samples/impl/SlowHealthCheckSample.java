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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sample Health Check that takes N msec to execute,
 *  used to demonstrate execution timeouts and caching.
 */

// Need to make the component immediate to make sure a single 
// instance is used, otherwise the lazy DS activation policy
// might cause a different instance to be used for every
// execution. In this sample this is just done to allow the
// counter to persist as long as this service's bundle is active.
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true,
        immediate=true)
@Properties({
    @Property(name=HealthCheck.NAME),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY),
    @Property(name=HealthCheck.MBEAN_NAME)
})
@Service(value=HealthCheck.class)
public class SlowHealthCheckSample implements HealthCheck{

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final AtomicInteger counter = new AtomicInteger();
    private long minExecutionTime;
    private long maxExecutionTime;
    
    @Property(label="Minimum execution time",
            description="Shortest execution time in milliseconds",
            intValue=SlowHealthCheckSample.DEFAULT_MIN_EXEC_TIME)
    private static final String PROP_MIN_EXEC_TIME = "execution.time.min.msec";
    private static final int DEFAULT_MIN_EXEC_TIME = 1000;
    
    @Property(label="Maximum execution time",
            description="Longest execution time in milliseconds",
            intValue=SlowHealthCheckSample.DEFAULT_MAX_EXEC_TIME)
    private static final String PROP_MAX_EXEC_TIME = "execution.time.max.msec";
    private static final int DEFAULT_MAX_EXEC_TIME = 5000;
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ", execution time=" + minExecutionTime + ".." + maxExecutionTime + " msec";
    }
    
    @Activate
    protected void activate(Map<String, Object> config) {
        minExecutionTime = PropertiesUtil.toInteger(config.get(PROP_MIN_EXEC_TIME), DEFAULT_MIN_EXEC_TIME);
        maxExecutionTime = PropertiesUtil.toInteger(config.get(PROP_MAX_EXEC_TIME), DEFAULT_MAX_EXEC_TIME);
        log.debug("{} activated", this);
    }
    
    @Deactivate
    protected void deactivate(Map<String, Object> config) {
        log.debug("{} deactivated", this);
    }
    
    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        try {
            final long randomDelay = (long)(Math.random() * (maxExecutionTime - minExecutionTime));
            final long toSleep = minExecutionTime + randomDelay;
            resultLog.debug("Executing {} will last {} msec", this, toSleep);
            Thread.sleep(toSleep);
        } catch(InterruptedException iex) {
            resultLog.warn("{} during execution", iex.getClass().getSimpleName());
        }
        final String execMsg = "Done executing, execution counter=" + counter.incrementAndGet(); 
        resultLog.debug("{}:{}", this, execMsg);
        log.debug("{}:{}", this, execMsg);
        return new Result(resultLog);
    }
}