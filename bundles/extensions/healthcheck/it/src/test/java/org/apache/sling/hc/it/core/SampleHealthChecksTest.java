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
package org.apache.sling.hc.it.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;
import javax.management.DynamicMBean;

import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class SampleHealthChecksTest {

    @Inject
    private HealthCheckExecutor executor;
    
    @Inject
    private BundleContext bundleContext;
    
    @Configuration
    public Option[] config() {
        return U.config();
    }
    
    @Test
    public void testAnnotatedHC() {
        final List<HealthCheckExecutionResult> results = executor.execute("annotation","sample");
        assertNotNull("Expecting non-null results");
        assertEquals("Expecting a single result", 1, results.size());
        final HealthCheckExecutionResult r = results.get(0);
        assertTrue("Expecting non-empty HC log", r.getHealthCheckResult().iterator().hasNext());
        final String expected = "All good";
        assertTrue(
                "Expecting first log message to contain " + expected,
                r.getHealthCheckResult().iterator().next().getMessage().contains(expected));
    }

    @Test
    public void testAnnotatedHCMBean() throws InvalidSyntaxException {
        // Verify that we have a DynamicMBean service with the right name, the JMX whiteboard will do the rest
        final String filter = "(jmx.objectname=org.apache.sling.healthcheck:type=HealthCheck,name=annotatedHC)";
        final ServiceReference<?> [] refs = bundleContext.getServiceReferences(DynamicMBean.class.getName(), filter);
        assertNotNull("Expecting non-null ServiceReferences for " + filter, refs);
        assertEquals("Expecting a single ServiceReference for " + filter, 1, refs.length);
    }
}
