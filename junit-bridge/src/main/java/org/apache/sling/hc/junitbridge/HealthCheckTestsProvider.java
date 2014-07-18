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
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.junit.TestsProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

/** Bridge Health Checks into the Sling JUnit server-side test
 *  framework, based on their tags.
 */
@Component(
    metatype=true,
    label="Apache Sling Health Checks/JUnit bridge",
    description="Makes Health Checks available as server-side JUnit tests"
)
@Service
public class HealthCheckTestsProvider implements TestsProvider {

    private String servicePid;
    private long lastModified;
    private BundleContext bundleContext;
    
    public static final String TEST_NAME_PREFIX = "HealthChecks(";
    public static final String TEST_NAME_SUFFIX = ")";
    
    public static final String [] DEFAULT_TAG_GROUPS = {
        "No tag groups configured"
    };

    @Property(cardinality=2147483647, 
            label="Health Check Tags",
            description="Groups of health check tags to execute as JUnit tests. Use the standard Health Checks 'includeThisTag,-omitThatTag' syntax")
    public static final String PROP_TAG_GROUPS = "health.check.tag.groups";
    
    private String [] tagGroups;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        tagGroups = PropertiesUtil.toStringArray(ctx.getProperties().get(PROP_TAG_GROUPS), DEFAULT_TAG_GROUPS);
        servicePid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
        lastModified = System.currentTimeMillis();
    }
    
    @Deactivate
    protected void deactivate() {
        bundleContext = null;
        servicePid = null;
        lastModified = -1;
    }
    
    @Override
    public Class<?> createTestClass(String testName) throws ClassNotFoundException {
        // The test name is like "Health Checks(foo,bar)" and we need just 'foo,bar'
        String tagString = null;
        try {
            tagString = testName.substring(0, testName.length() - TEST_NAME_SUFFIX.length()).substring(TEST_NAME_PREFIX.length()); 
        } catch(Exception e) {
            throw new RuntimeException("Invalid test name:" + testName);
        }
         
        JUnitTestBridge.setThreadContext(new TestBridgeContext(bundleContext, splitTags(tagString)));
        return JUnitTestBridge.class;
    }
    
    private String [] splitTags(String tags) {
        final List<String> result = new ArrayList<String>();
        for(String tag: tags.split(",")) {
            result.add(tag.trim());
        }
        return result.toArray(new String[]{});
    }

    @Override
    public String getServicePid() {
        return servicePid;
    }

    @Override
    public List<String> getTestNames() {
        final List<String> result = new ArrayList<String>();
        for(String t : tagGroups) {
            result.add(TEST_NAME_PREFIX + t + TEST_NAME_SUFFIX);
        }
        return result;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }
}