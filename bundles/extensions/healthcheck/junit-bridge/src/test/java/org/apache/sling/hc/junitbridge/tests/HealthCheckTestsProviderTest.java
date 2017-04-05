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
package org.apache.sling.hc.junitbridge.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.junitbridge.HealthCheckTestsProvider;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.junit.TestsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/** Test the HealthCheckTestsProvider, which 
 *  uses everything else.
 */
public class HealthCheckTestsProviderTest {
    private TestsProvider provider;
    private long setupTimestamp;
    private final Random random = new Random();
    
    static abstract class HcLogSetter {
        abstract FormattingResultLog setLog(FormattingResultLog log);
    };
    
    private static final Map<String, HcLogSetter> LOG_SETTERS = new HashMap<String, HcLogSetter>(); 
    
    static {
        LOG_SETTERS.put("PASS_HC", new HcLogSetter() {
            @Override
            FormattingResultLog setLog(FormattingResultLog log) {
                log.info("pass");
                return log;
            }
        });
        LOG_SETTERS.put("OK_HC", new HcLogSetter() {
            @Override
            FormattingResultLog setLog(FormattingResultLog log) {
                log.debug("ok");
                return log;
            }
        });
        LOG_SETTERS.put("FAIL_HC", new HcLogSetter() {
            @Override
            FormattingResultLog setLog(FormattingResultLog log) {
                log.warn("fail");
                return log;
            }
        });
        LOG_SETTERS.put("BAD_HC", new HcLogSetter() {
            @Override
            FormattingResultLog setLog(FormattingResultLog log) {
                log.warn("bad");
                return log;
            }
        });
    }
    
    // Our fake tags represent a number of
    // passing (P) or failing (F) fake HCs 
    final String [] TAG_GROUPS = {
            "a,b",
            "some,tags",
            "justOne"
    };
    
    private static String testName(String tagGroup) {
        return HealthCheckTestsProvider.TEST_NAME_PREFIX + tagGroup + HealthCheckTestsProvider.TEST_NAME_SUFFIX;
    }
    
    /** Return ServiceReferences that point to our test HealthChecks */
    private ServiceReference [] getMockReferences(BundleContext bc, String OSGiFilter) {
        
        final List<ServiceReference> refs = new ArrayList<ServiceReference>();
        
        for(String key : LOG_SETTERS.keySet()) {
            if(OSGiFilter.contains(key)) {
                final HcLogSetter hls = LOG_SETTERS.get(key);
                final ServiceReference ref = Mockito.mock(ServiceReference.class);
                Mockito.when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(random.nextLong());
                Mockito.when(ref.getProperty(HealthCheck.NAME)).thenReturn("someTest");
                final HealthCheck hc = new HealthCheck() {
                    @Override
                    public Result execute() {
                        final FormattingResultLog log = new FormattingResultLog();
                        return new Result(hls.setLog(log));
                    }
                };
                Mockito.when(bc.getService(ref)).thenReturn(hc);
                
                refs.add(ref);
            }
        }
        
        return refs.toArray(new ServiceReference[]{});
    }
            
    @Before
    public void setup() throws InvalidSyntaxException {
        setupTimestamp = System.currentTimeMillis();
        final ComponentContext ctx = Mockito.mock(ComponentContext.class);

        // context properties
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheckTestsProvider.PROP_TAG_GROUPS, TAG_GROUPS);
        props.put(Constants.SERVICE_PID, getClass().getName());
        Mockito.when(ctx.getProperties()).thenReturn(props);
        
        // bundle context
        final BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(ctx.getBundleContext()).thenReturn(bc);
        
        // HealthCheck ServiceReferences mocks
        Mockito.when(bc.getServiceReferences(Mockito.anyString(), Mockito.anyString())).thenAnswer(
            new Answer<ServiceReference[]> () {
                @Override
                public ServiceReference[] answer(InvocationOnMock invocation) throws Throwable {
                    return getMockReferences(bc, (String)invocation.getArguments()[1]);
                }
            });
        
        provider = new HealthCheckTestsProvider() {
            {
                activate(ctx);
            }
        };
    }
    
    @Test
    public void testGetTestNames() {
        final List<String> names = provider.getTestNames();
        assertEquals(TAG_GROUPS.length, names.size());
        for(String tag : TAG_GROUPS) {
            final String expected = testName(tag);
            assertTrue("Expecting test names to contain " + expected + ", " + names, names.contains(expected));
        }
    }
    
    @Test
    public void testServicePid() {
        assertEquals(getClass().getName(), provider.getServicePid());
    }
    
    @Test
    public void testLastModified() {
        assertTrue(provider.lastModified() >= setupTimestamp);
    }
    
    @Test
    public void testNoFailuresHealthCheck() throws ClassNotFoundException {
        final Class<?> c = provider.createTestClass(testName("PASS_HC"));
        assertNotNull("Expecting non-null test class", c);
        final org.junit.runner.Result r = JUnitCore.runClasses(c); 
        assertEquals(0, r.getFailureCount());
        assertEquals(1, r.getRunCount());
    }
    
    @Test
    public void testFailingHealthCheck() throws ClassNotFoundException {
        final Class<?> c = provider.createTestClass(testName("FAIL_HC and BAD_HC"));
        assertNotNull("Expecting non-null test class", c);
        final org.junit.runner.Result r = JUnitCore.runClasses(c); 
        assertEquals(2, r.getFailureCount());
        assertEquals(2, r.getRunCount());
    }
    
    @Test
    public void testPassAndFailHealthCheck() throws ClassNotFoundException {
        final Class<?> c = provider.createTestClass(testName("FAIL_HC and PASS_HC and OK_HC and BAD_HC"));
        assertNotNull("Expecting non-null test class", c);
        final org.junit.runner.Result r = JUnitCore.runClasses(c); 
        assertEquals(2, r.getFailureCount());
        assertEquals(4, r.getRunCount());
    }

    @Test(expected=RuntimeException.class)
    public void testInvalidTestName() throws ClassNotFoundException {
        provider.createTestClass("foo");
    }
}