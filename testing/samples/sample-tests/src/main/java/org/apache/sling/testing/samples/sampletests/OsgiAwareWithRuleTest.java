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
package org.apache.sling.testing.samples.sampletests;

import static org.junit.Assert.assertNotNull;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.sling.junit.rules.Service;

/** Test OSGi services injection */
@RunWith(SlingAnnotationsTestRunner.class)
public class OsgiAwareWithRuleTest {
    
    @Rule
    public Service configAdminService = new Service(ConfigurationAdmin.class);
   
    @Test
    public void testConfigAdmin() throws Exception {
        ConfigurationAdmin configAdmin = configAdminService.getService(ConfigurationAdmin.class);
        assertNotNull(
                "Expecting ConfigurationAdmin to be injected by Sling test runner", 
                configAdmin);
        
        final String name = "TEST_" + getClass().getName() + System.currentTimeMillis();
        assertNotNull("Expecting config " + name + " to be created",
                configAdmin.getConfiguration(name));
    }
}
