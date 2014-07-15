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
package org.apache.sling.hc.junitbridge;

import junit.framework.TestCase;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class HealthCheckTest extends TestCase {
    private final HealthCheck hc;
    private final HealthCheckMetadata metadata;
    private final BundleContext bundleContext;
    private final ServiceReference serviceRef;
        
    HealthCheckTest(TestBridgeContext context, ServiceReference ref) {
        super("testHealthCheck");
        bundleContext = context.getBundleContext();
        serviceRef = ref;
        this.hc = (HealthCheck)bundleContext.getService(ref);
        this.metadata = new HealthCheckMetadata(ref);
    }
    
    @Override
    public String getName() {
        return metadata.getName();
    }

    /** Execute our health check and dump its log
     *  messages > INFO if it fails */
    public void testHealthCheck() {
        try {
            final Result r = hc.execute();
            final StringBuilder failMsg = new StringBuilder();
            if(!r.isOk()) {
                failMsg.append(metadata.getName());
                failMsg.append("\n");
                for(ResultLog.Entry log : r) {
                    if(log.getStatus().compareTo(Result.Status.INFO) > 0) {
                        if(failMsg.length() > 0) {
                            failMsg.append("\n");
                        }
                        failMsg.append(log.getStatus().toString());
                        failMsg.append(" - ");
                        failMsg.append(log.getMessage());
                    }
                }
            }
            if(failMsg.length() > 0) {
                fail("Health Check failed: " + failMsg.toString());
            }
        } finally {
            // TODO is that ok? service not used anymore after this?
            bundleContext.ungetService(serviceRef);
        }
    }
}