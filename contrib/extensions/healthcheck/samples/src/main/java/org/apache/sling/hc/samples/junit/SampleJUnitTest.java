package org.apache.sling.hc.samples.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Trivial test used to demonstrate the junit Health Check service.
 *  Verify that the BundleContext is injected and that a specific
 *  bundle is active.
 */
@RunWith(SlingAnnotationsTestRunner.class)
public class SampleJUnitTest {
    
    @TestReference
    private BundleContext bundleContext;
    
    @Test
    public void checkBundleContext() {
        assertNotNull(bundleContext);
    }
    
    @Test
    public void checkGroovyBundleActive() {
        final String symbolicName = "org.apache.sling.extensions.groovy";
        for(Bundle b : bundleContext.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                assertEquals("Expecting " + symbolicName + " to be active", Bundle.ACTIVE, b.getState());
            }
        }
    }
}
