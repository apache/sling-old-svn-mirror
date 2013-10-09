/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.installer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class LaunchpadConfigInstallerTest {
    
    private OsgiInstaller installer;
    private Set<String> registered;
    private boolean getChildrenReturnsNull;
    private boolean checkResourceTypes;
    
    final Set<String> NO_RUN_MODES = new HashSet<String>();
    
    final LaunchpadContentProvider provider = new LaunchpadContentProvider() {
        
        private final Map<String, String> CHILDREN = new HashMap<String, String>();
        
        {
            CHILDREN.put("resources", 
                    "config,bundles,install/,install.dev,install.dev.test,install.test.dev,install.dev.test.another,install.another");
            CHILDREN.put("config", "A.cfg,B.config,C.somefile,D.properties,dev,cfgModeB");
            CHILDREN.put("dev", "cfgDev.properties");
            CHILDREN.put("cfgModeB", "cfgB.properties");
            CHILDREN.put("install", "install.cfg,install/5/");
            CHILDREN.put("install/5", "fiveA.cfg,fiveB.properties");
            CHILDREN.put("install.dev", "mars.config,april.properties");
            CHILDREN.put("install.dev.test", "devtest.cfg/");
            CHILDREN.put("install.test.dev", "testdev.cfg");
            CHILDREN.put("install.dev.test.another", "anotherNo.cfg");
            CHILDREN.put("install.another", "anotherYes.cfg");
            CHILDREN.put("bundles", "foo.jar,bar.jar,wii.jar");
        }
        
        public Iterator<String> getChildren(String path) {
            if(getChildrenReturnsNull) {
                // simulate old-style provider
                return null;
            }
            final List<String> result = new ArrayList<String>();
            final String kids = CHILDREN.get(path);
            if(kids != null) {
                for(String r : kids.split(",")) {
                    result.add(r);
                }
            }
            return result.iterator();
        }

        public URL getResource(String path) {
            try {
                return new URL("file://" + path);
            } catch(MalformedURLException mfe) {
                fail("Invalid URL " + mfe);
            }
            return null;
        }

        public InputStream getResourceAsStream(String path) {
            return new ByteArrayInputStream(path.getBytes());
        }
    };
    
    private void assertRegistered(String ...resources) {
        final List<String> expected = Arrays.asList(resources);
        for(String r : expected) {
            assertTrue("Expecting " + r + " to be registered (" + registered + ")", registered.contains(r));
        }
        final int delta = registered.size() - resources.length; 
        if(delta != 0) {
            final List<String> unexpected = new ArrayList<String>();
            for(String r : registered) {
                if(!expected.contains(r)) {
                    unexpected.add(r);
                }
                
            }
            fail("Expected resources don't match registered, unexpected=" + unexpected);
        }
    }
    
    @Before
    public void setup() {
        installer = Mockito.mock(OsgiInstaller.class);
        registered = new HashSet<String>();
        getChildrenReturnsNull = false;
        checkResourceTypes = false;
        
        final Answer<Void> rCollector = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                assertEquals("launchpad", invocation.getArguments()[0]);
                final InstallableResource [] resources = (InstallableResource[])invocation.getArguments()[1];
                for(InstallableResource r : resources) {
                    String value = r.getId();
                    final Object hint = r.getDictionary() == null ? null : r.getDictionary().get(InstallableResource.INSTALLATION_HINT);
                    if(hint != null) {
                        value += "-H" + hint;
                    }
                    value += "-P" + r.getPriority();
                    if(checkResourceTypes) {
                        value += "-T" + r.getType();
                    }
                    registered.add(value);
                }
                return null;
            }
        };
        Mockito.doAnswer(rCollector).when(installer).registerResources(Matchers.anyString(), Matchers.any(InstallableResource[].class));
    }
    
    @Test
    public void testNoRunModes() {
        LaunchpadConfigInstaller.install(installer, provider, NO_RUN_MODES);
        assertRegistered("A.cfg-P50", "B.config-P50", "C.somefile-P50", "D.properties-P50","install.cfg-P50",
                "fiveA.cfg-H5-P50","fiveB.properties-H5-P50");
    }
    
    @Test
    public void testAnotherMode() {
        final Set<String> runModes = new HashSet<String>();
        runModes.add("another");
        LaunchpadConfigInstaller.install(installer, provider, runModes);
        assertRegistered(
                "A.cfg-P50", "B.config-P50", "C.somefile-P50", "D.properties-P50","install.cfg-P50",
                "fiveA.cfg-H5-P50","fiveB.properties-H5-P50",
                "anotherYes.cfg-P55"); 
    }
    
    @Test
    public void testDevRunMode() {
        checkResourceTypes = true;
        final Set<String> runModes = new HashSet<String>();
        runModes.add("dev");
        LaunchpadConfigInstaller.install(installer, provider, runModes);
        assertRegistered(
                "A.cfg-P50-Tproperties", "B.config-P50-Tproperties", "C.somefile-P50-Tproperties", 
                "D.properties-P50-Tproperties","install.cfg-P50-Tfile",
                "fiveA.cfg-H5-P50-Tfile","fiveB.properties-H5-P50-Tfile",
                "cfgDev.properties-P55-Tproperties",
                "mars.config-P55-Tfile", "april.properties-P55-Tfile");
    }
    
    @Test
    public void testDevTestRunModes() {
        final Set<String> runModes = new HashSet<String>();
        runModes.add("dev");
        runModes.add("test");
        LaunchpadConfigInstaller.install(installer, provider, runModes);
        assertRegistered(
                "A.cfg-P50", "B.config-P50", "C.somefile-P50", "D.properties-P50","install.cfg-P50",
                "cfgDev.properties-P55",
                "fiveA.cfg-H5-P50","fiveB.properties-H5-P50",
                "mars.config-P55", "april.properties-P55",
                "devtest.cfg-P60", "testdev.cfg-P60");
    }
    
    @Test
    public void testOldStyle() {
        getChildrenReturnsNull = true;
        LaunchpadConfigInstaller.install(installer, provider, NO_RUN_MODES);
        assertRegistered();
    }
}