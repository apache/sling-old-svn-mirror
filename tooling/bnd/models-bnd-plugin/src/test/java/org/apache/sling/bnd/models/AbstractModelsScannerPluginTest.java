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
package org.apache.sling.bnd.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractModelsScannerPluginTest {
    
    protected Builder builder;

    @Before
    public final void setUp() throws Exception {
        builder = new Builder();
        
        Jar classesDirJar = new Jar("test.jar", new File("target/test-classes"));
        classesDirJar.setManifest(new Manifest());
        builder.setJar(classesDirJar);
        
        builder.setSourcepath(new File[] { new File("src/test/java") } );
        
        Plugin plugin = new ModelsScannerPlugin();
        plugin.setReporter(mock(Reporter.class));
        plugin.setProperties(getProperties());
        builder.addBasicPlugin(plugin);
    }

    @After
    public final void tearDown() throws Exception {
        if (!builder.getErrors().isEmpty()) {
            fail(StringUtils.join(builder.getErrors(), "\n"));
        }
        builder.close();
    }
    
    protected Map<String,String> getProperties() {
        return new HashMap<>();
    }
    
    protected final void assertHeaderMissing(Jar jar, String headerName) throws Exception {
        assertNull(jar.getManifest().getMainAttributes().getValue(headerName));
    }

    protected final void assertHeader(Jar jar, String headerName, String... headerValues) throws Exception {
        Set<String> expectedValues = new HashSet<>(Arrays.asList(headerValues));
        String[] actual = StringUtils.split(jar.getManifest().getMainAttributes().getValue(headerName), ",");
        Set<String> actualValues = new HashSet<>(Arrays.asList(actual));
        assertEquals(expectedValues, actualValues);
    }

}
