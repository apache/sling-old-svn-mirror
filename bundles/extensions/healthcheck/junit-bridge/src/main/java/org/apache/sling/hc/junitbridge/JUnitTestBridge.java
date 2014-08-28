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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.osgi.framework.ServiceReference;

@RunWith(AllTests.class)
public class JUnitTestBridge {
    private static ThreadLocal<TestBridgeContext> testContext = new ThreadLocal<TestBridgeContext>();
    
    static void setThreadContext(TestBridgeContext c) {
        testContext.set(c);
    }
    
    public static junit.framework.Test suite() {
        final TestBridgeContext context = testContext.get();
        assertNotNull("Expecting non-null TestBridgeContext, via ThreadLocal", context);
        TestSuite suite = new TestSuite();
        for(ServiceReference ref : context.getFilter().getTaggedHealthCheckServiceReferences(context.getTags())) {
            suite.addTest(new HealthCheckTest(context, ref));
        }
        if(suite.countTestCases() == 0) {
            fail("No Health Checks found with tags " + Arrays.asList(context.getTags()));
        }
        return suite;
    }
}