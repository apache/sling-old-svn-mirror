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
package org.apache.sling.crankstart.core.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.core.CrankstartFileProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/** Verify that factory configs are handled properly
 *  when starting the OSGi framework multiple times
 *  with Crankstart.
 */
public class ConfigFactoryTest {

    private CrankstartContext ctx;
    private BundleContext bundleContext;
    private Object configAdmin;
    public static final String TEST_PATH = "/configfactory-test.txt";
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
        
        ctx = new CrankstartContext();
        final String osgiStoragePath = System.getProperty("test.storage.base") + "/" + UUID.randomUUID();
        System.setProperty("osgi.storage.path", osgiStoragePath);
        
        final InputStream is = getClass().getResourceAsStream(TEST_PATH);
        assertNotNull("Expecting test resource to be found:" + TEST_PATH, is);
        final Reader input = new InputStreamReader(is);
        try {
            new CrankstartFileProcessor(ctx).process(input);
        } finally {
            input.close();
        }
        
        bundleContext = ctx.getOsgiFramework().getBundleContext();
        @SuppressWarnings("rawtypes")
        final ServiceReference configAdminRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        assertNotNull("Expecting ConfigurationAdmin service to be present", configAdminRef);
        configAdmin = bundleContext.getService(configAdminRef);
    }
    
    @After
    public void cleanup() throws BundleException {
        ctx.getOsgiFramework().stop();
    }
    
    @Test
    public void testMultipleConfigureExecutions() throws Exception {
        final Configure cmd = new Configure();
        
        final String verb = "config.factory";
        final String factoryName = UUID.randomUUID().toString();
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Configure.CRANKSTART_CONFIG_ID, UUID.randomUUID().toString());
        
        assertEquals("Expecting no configs initially", 0, count(factoryName));
        
        final CrankstartCommandLine commandLine = new CrankstartCommandLine(verb, factoryName, properties);
        cmd.execute(ctx, commandLine);
        assertEquals("Expecting one config after first excute call", 1, count(factoryName));
        cmd.execute(ctx, commandLine);
        assertEquals("Expecting only one config after second excute call", 1, count(factoryName));
    }
    
    private int count(String factoryPid) throws Exception {
        final String filter = "(service.factoryPid=" + factoryPid + ")";
        final Object [] c = (Object [])configAdmin.getClass()
                .getMethod("listConfigurations", String.class)
                .invoke(configAdmin, filter);
        return c == null ? 0 : c.length;
    }
}
