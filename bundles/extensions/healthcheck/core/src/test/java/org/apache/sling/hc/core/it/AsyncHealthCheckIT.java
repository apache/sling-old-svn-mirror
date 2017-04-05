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
package org.apache.sling.hc.core.it;

import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
public class AsyncHealthCheckIT {

    @Inject
    private HealthCheckExecutor executor;
    
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return U.config();
    }

    @Test
    public void testAsyncHealthCheck() throws InterruptedException {
        final String id = UUID.randomUUID().toString();
        final AtomicInteger counter = new AtomicInteger(Integer.MIN_VALUE);
        final HealthCheck hc = new HealthCheck() {
            @Override
            public Result execute() {
                final int v = counter.incrementAndGet();
                return new Result(Result.Status.OK, "counter is now " + v);
            }
            
        };
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, "name_" + id);
        props.put(HealthCheck.TAGS, id);
        props.put(HealthCheck.ASYNC_CRON_EXPRESSION, "*/1 * * * * ?");
        
        @SuppressWarnings("rawtypes")
        final ServiceRegistration reg = bundleContext.registerService(HealthCheck.class.getName(), hc, props);
        
        try {
            // Wait for HC to be registered
            U.expectHealthChecks(1, executor, id);
            
            // Now reset the counter and check that HC increments it even if we don't
            // use the executor
            {
                counter.set(0);
                final long timeout = System.currentTimeMillis() + 5000L;
                while(System.currentTimeMillis() < timeout) {
                    if(counter.get() > 0) {
                        break;
                    }
                    Thread.sleep(100L);
                }
                assertTrue("Expecting counter to be incremented", counter.get() > 0);
            }
            
            // Verify that we get the right log
            final String msg = executor.execute(id).get(0).getHealthCheckResult().iterator().next().getMessage();
            assertTrue("Expecting the right message: " + msg, msg.contains("counter is now"));
            
            // And verify that calling executor lots of times doesn't increment as much
            final int previous = counter.get();
            final int n = 100;
            for(int i=0; i < n; i++) {
                executor.execute(id);
            }
            assertTrue("Expecting counter to increment asynchronously", counter.get() < previous + n);
        } finally {
            reg.unregister();
        }
        
    }

}
