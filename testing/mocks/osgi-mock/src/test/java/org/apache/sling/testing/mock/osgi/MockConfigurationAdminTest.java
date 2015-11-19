/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest.ServiceWithMetadata;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.common.collect.ImmutableMap;

public class MockConfigurationAdminTest {
    
    private static final String[] TEST_ADAPTABLES = new String[] {
        "adaptable1",
        "adaptable2"
    };

    @Rule
    public OsgiContext context = new OsgiContext();

    @Test
    public void testGetConfigurationString() throws IOException {
        ConfigurationAdmin configAdmin = context.getService(ConfigurationAdmin.class);
        
        Configuration config = configAdmin.getConfiguration("org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata");
        Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put(Constants.SERVICE_RANKING, 3000);
        configProps.put("adaptables", TEST_ADAPTABLES);
        configProps.put("prop2", 2);
        config.update(configProps);
        
        context.registerInjectActivateService(new ServiceWithMetadata(), ImmutableMap.<String, Object>builder()
                .put(Constants.SERVICE_RANKING, 4000)
                .put("prop1", 1)
                .build());
        
        ServiceReference reference = context.bundleContext().getServiceReference(Comparable.class.getName());

        // values passed over when registering service has highest precedence
        assertEquals(4000, reference.getProperty(Constants.SERVICE_RANKING));
        assertEquals(1, reference.getProperty("prop1"));

        // values set in config admin has 2ndmost highest precedence
        assertArrayEquals(TEST_ADAPTABLES, (String[])reference.getProperty("adaptables"));
        assertEquals(2, reference.getProperty("prop2"));

        // values set in OSGi SCR metadata
        assertEquals("The Apache Software Foundation", reference.getProperty(Constants.SERVICE_VENDOR));
        assertEquals("org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata", reference.getProperty(Constants.SERVICE_PID));
    }

}
