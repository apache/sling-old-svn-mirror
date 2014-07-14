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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.apache.sling.junit.TestsProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

/** Bridge Health Checks into the Sling JUnit server-side test
 *  framework, based on their tags.
 */
@Component
@Service
public class HealthCheckTestsProvider implements TestsProvider {

    private String servicePid;

    // TODO configurable
    private String [] tags = { "script" };
    private HealthCheckFilter filter;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        servicePid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
        filter = new HealthCheckFilter(ctx.getBundleContext());
    }
    
    @Override
    public Class<?> createTestClass(String testName) throws ClassNotFoundException {
        JUnitTestBridge.setContext(new TestBridgeContext(filter, tags));
        return JUnitTestBridge.class;
    }

    @Override
    public String getServicePid() {
        return servicePid;
    }

    @Override
    public List<String> getTestNames() {
        final List<String> result = new ArrayList<String>();
        // TODO use a configurable name?
        result.add("HealthChecks(sling,slow)");
        return result;
    }

    @Override
    public long lastModified() {
        return 0;
    }
}