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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class ReflectionServiceUtilTest {

    private BundleContext bundleContext = MockOsgi.newBundleContext();
    private Service1 service1;
    private Service2 service2;

    @Before
    public void setUp() {
        service1 = new Service1();
        service2 = new Service2();
        bundleContext.registerService(ServiceInterface1.class.getName(), service1, null);
        bundleContext.registerService(ServiceInterface2.class.getName(), service2, null);
    }

    @Test
    public void testService3() {
        Service3 service3 = new Service3();
        assertTrue(MockOsgi.injectServices(service3, bundleContext));

        Dictionary<String, Object> service3Config = new Hashtable<String, Object>();
        service3Config.put("prop1", "value1");
        assertTrue(MockOsgi.activate(service3, bundleContext, service3Config));

        assertNotNull(service3.getComponentContext());
        assertEquals(service3Config, service3.getComponentContext().getProperties());

        assertSame(service1, service3.getReference1());

        List<ServiceInterface2> references2 = service3.getReferences2();
        assertEquals(1, references2.size());
        assertSame(service2, references2.get(0));

        List<ServiceSuperInterface3> references3 = service3.getReferences3();
        assertEquals(1, references3.size());
        assertSame(service2, references3.get(0));

        List<Map<String, Object>> reference3Configs = service3.getReference3Configs();
        assertEquals(1, reference3Configs.size());
        assertEquals(200, reference3Configs.get(0).get(Constants.SERVICE_RANKING));

        assertTrue(MockOsgi.deactivate(service3));
        assertNull(service3.getComponentContext());
    }

    @Test
    public void testService4() {
        Service4 service4 = new Service4();

        assertTrue(MockOsgi.injectServices(service4, bundleContext));
        assertFalse(MockOsgi.activate(service4));

        assertSame(service1, service4.getReference1());
    }

    public interface ServiceInterface1 {
        // no methods
    }

    public interface ServiceInterface2 {
        // no methods
    }

    public interface ServiceInterface3 extends ServiceSuperInterface3 {
        // no methods
    }

    public interface ServiceSuperInterface3 {
        // no methods
    }

    @Component
    @Service(ServiceInterface1.class)
    @Property(name = Constants.SERVICE_RANKING, intValue = 100)
    public static class Service1 implements ServiceInterface1 {
        // dummy interface
    }

    @Component
    @Service({ ServiceInterface2.class, ServiceInterface3.class })
    @Property(name = Constants.SERVICE_RANKING, intValue = 200)
    public static class Service2 implements ServiceInterface2, ServiceInterface3 {
        // dummy interface
    }

    @Component
    @References({ @Reference(name = "reference2", referenceInterface = ServiceInterface2.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE) })
    public static class Service3 {

        @Reference
        private ServiceInterface1 reference1;

        private List<ServiceReference> references2 = new ArrayList<ServiceReference>();

        @Reference(name = "reference3", referenceInterface = ServiceInterface3.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
        private List<ServiceSuperInterface3> references3 = new ArrayList<ServiceSuperInterface3>();
        private List<Map<String, Object>> reference3Configs = new ArrayList<Map<String, Object>>();

        private ComponentContext componentContext;

        @Activate
        private void activate(ComponentContext ctx) {
            this.componentContext = ctx;
        }

        @Deactivate
        private void deactivate(ComponentContext ctx) {
            this.componentContext = null;
        }

        public ServiceInterface1 getReference1() {
            return this.reference1;
        }

        public List<ServiceInterface2> getReferences2() {
            List<ServiceInterface2> services = new ArrayList<ServiceInterface2>();
            for (ServiceReference serviceReference : references2) {
                services.add((ServiceInterface2)componentContext.getBundleContext().getService(serviceReference));
            }
            return services;
        }

        public List<ServiceSuperInterface3> getReferences3() {
            return this.references3;
        }

        public List<Map<String, Object>> getReference3Configs() {
            return this.reference3Configs;
        }

        public ComponentContext getComponentContext() {
            return this.componentContext;
        }

        protected void bindReference1(ServiceInterface1 service) {
            reference1 = service;
        }

        protected void unbindReference1(ServiceInterface1 service) {
            reference1 = null;
        }

        protected void bindReference2(ServiceReference serviceReference) {
            references2.add(serviceReference);
        }

        protected void unbindReference2(ServiceReference serviceReference) {
            references2.remove(serviceReference);
        }

        protected void bindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
            references3.add(service);
            reference3Configs.add(serviceConfig);
        }

        protected void unbindReference3(ServiceSuperInterface3 service, Map<String, Object> serviceConfig) {
            references3.remove(service);
            reference3Configs.remove(serviceConfig);
        }

    }

    @Component
    @Reference(referenceInterface = ServiceInterface1.class, name = "customName", bind = "customBind", unbind = "customUnbind")
    public static class Service4 {

        private ServiceInterface1 reference1;

        public ServiceInterface1 getReference1() {
            return this.reference1;
        }

        protected void customBind(ServiceInterface1 service) {
            reference1 = service;
        }

        protected void customUnbind(ServiceInterface1 service) {
            reference1 = null;
        }

    }

}
