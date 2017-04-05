/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.fail;

/**
 * This teleported test checks the presence of multiple services in the Sling launchpad.
 */
public class ServicesPresentTest {

    public static final String[] services = new String[] {
            "org.apache.sling.hc.api.execution.HealthCheckExecutor"
    };

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");

    @Test
    public void testServices() {
        BundleContext bundleContext = teleporter.getService(BundleContext.class);
        StringBuilder stringBuilder = new StringBuilder();
        for (String service : services) {
            ServiceReference sr = bundleContext.getServiceReference(service);
            if (sr == null) {
                stringBuilder.append("    ").append(service).append("\n");
            }
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.insert(0, "Could not obtain a ServiceReference for the following services:\n");
            fail(stringBuilder.toString());
        }
    }

}
