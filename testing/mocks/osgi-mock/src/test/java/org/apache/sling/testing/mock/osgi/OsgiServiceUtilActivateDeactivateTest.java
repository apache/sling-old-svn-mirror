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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ImmutableMap;

/**
 * Test different variants of activate/deactivate methods with varying signatures.
 */
public class OsgiServiceUtilActivateDeactivateTest {

    private Map<String,Object> map = ImmutableMap.<String, Object>of("prop1", "value1");
    private BundleContext bundleContext = MockOsgi.newBundleContext();
    
    @Test
    public void testService1() {
        Service1 service = new Service1();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService2() {
        Service2 service = new Service2();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getBundleContext());
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService3() {
        Service3 service = new Service3();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService4() {
        Service4 service = new Service4();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService5() {
        Service5 service = new Service5();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }

    @Test
    public void testService6() {
        Service6 service = new Service6();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }
    
    @Test
    public void testService7() {
        Service7 service = new Service7();
        
        assertTrue(MockOsgi.activate(service, bundleContext, map));
        assertTrue(service.isActivated());
        assertSame(bundleContext, service.getComponentContext().getBundleContext());
        assertSame(bundleContext, service.getBundleContext());
        assertEquals(map, ImmutableMap.copyOf(service.getMap()));
        
        assertTrue(MockOsgi.deactivate(service, bundleContext, map));
        assertFalse(service.isActivated());
    }
    
    
    public @interface ServiceConfig {
        String prop1();
    }

    @Component
    public static class Service1 {
        
        private boolean activated;
        private ComponentContext componentContext;

        @Activate
        private void activate(ComponentContext ctx) {
            this.activated = true;
            this.componentContext = ctx;
        }

        @Deactivate
        private void deactivate(ComponentContext ctx) {
            this.activated = false;
            this.componentContext = null;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public ComponentContext getComponentContext() {
            return componentContext;
        }

    }

    @Component
    public static class Service2 {
        
        private boolean activated;
        private BundleContext bundleContext;

        @Activate
        private void activate(BundleContext ctx) {
            this.activated = true;
            this.bundleContext = ctx;
        }

        @Deactivate
        private void deactivate(BundleContext ctx) {
            this.activated = false;
            this.bundleContext = null;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public BundleContext getBundleContext() {
            return bundleContext;
        }

    }

    @Component
    public static class Service3 {
        
        private boolean activated;
        private Map<String, Object> map;

        @Activate
        private void activate(Map<String,Object> map) {
            this.activated = true;
            this.map = map;
        }

        @Deactivate
        private void deactivate(Map<String,Object> map) {
            this.activated = false;
            this.map = null;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public Map<String, Object> getMap() {
            return map;
        }

    }

    @Component
    public static class Service4 {
        
        private boolean activated;
        private Map<String, Object> map;

        @Activate
        private void activate(ServiceConfig config) {
            this.activated = true;
            map = ImmutableMap.<String, Object>of("prop1", config.prop1());
        }

        @Deactivate
        private void deactivate(int value) {
            this.activated = false;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public Map<String, Object> getMap() {
            return map;
        }
        
    }

    @Component
    public static class Service5 {
        
        private boolean activated;

        @Activate
        private void activate() {
            this.activated = true;
        }

        @Deactivate
        private void deactivate(Integer value) {
            this.activated = false;
        }
        
        public boolean isActivated() {
            return activated;
        }

    }

    @Component
    public static class Service6 {
        
        private boolean activated;
        private ComponentContext componentContext;
        private BundleContext bundleContext;
        private Map<String,Object> map;

        @Activate
        private void activate(ComponentContext componentContext, BundleContext bundleContext, Map<String,Object> map) {
            this.activated = true;
            this.componentContext = componentContext;
            this.bundleContext = bundleContext;
            this.map = map;
        }

        @Deactivate
        private void deactivate(Map<String,Object> map, BundleContext bundleContext, int value1, Integer value2) {
            this.activated = false;
            this.componentContext = null;
            this.bundleContext = null;
            this.map = null;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public ComponentContext getComponentContext() {
            return componentContext;
        }

        public BundleContext getBundleContext() {
            return bundleContext;
        }

        public Map<String, Object> getMap() {
            return map;
        }

    }

    @Component
    public static class Service7 {
        
        private boolean activated;
        private ComponentContext componentContext;
        private BundleContext bundleContext;
        private Map<String,Object> map;

        @Activate
        private void activate(ComponentContext componentContext, ServiceConfig config, BundleContext bundleContext) {
            this.activated = true;
            this.componentContext = componentContext;
            this.bundleContext = bundleContext;
            this.map = ImmutableMap.<String, Object>of("prop1", config.prop1());;
        }

        @Deactivate
        private void deactivate() {
            this.activated = false;
            this.componentContext = null;
            this.bundleContext = null;
            this.map = null;
        }
        
        public boolean isActivated() {
            return activated;
        }

        public ComponentContext getComponentContext() {
            return componentContext;
        }

        public BundleContext getBundleContext() {
            return bundleContext;
        }

        public Map<String, Object> getMap() {
            return map;
        }

    }

}
