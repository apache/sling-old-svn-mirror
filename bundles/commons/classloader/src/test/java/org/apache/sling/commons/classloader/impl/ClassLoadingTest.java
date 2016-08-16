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
package org.apache.sling.commons.classloader.impl;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Simple test for the dynamic class loader.
 */
public class ClassLoadingTest {

    protected Mockery context;

    public ClassLoadingTest() {
        this.context = new JUnit4Mockery();
    }

    /**
     * This method tests the dynamic class loading through the package admin.
     * The returned class changes from map to array list.
     */
    @Test public void testLoading() throws Exception {
        final Sequence sequence = this.context.sequence("load-sequence");
        final BundleContext bundleContext = this.context.mock(BundleContext.class);
        final PackageAdmin packageAdmin = this.context.mock(PackageAdmin.class);
        final ExportedPackage ep = this.context.mock(ExportedPackage.class);
        final Bundle bundle = this.context.mock(Bundle.class);
        this.context.checking(new Expectations() {{
            allowing(bundleContext).createFilter(with(any(String.class)));
            will(returnValue(null));
            allowing(bundleContext).getServiceReferences(with(any(String.class)), with((String)null));
            will(returnValue(null));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            allowing(bundleContext).removeServiceListener(with(any(ServiceListener.class)));
            allowing(packageAdmin).getExportedPackage("org.apache.sling.test");
            will(returnValue(ep));
            allowing(ep).getExportingBundle();
            will(returnValue(bundle));
            allowing(ep).isRemovalPending();
            will(returnValue(false));
            allowing(bundle).getBundleId();
            will(returnValue(2L));
            allowing(bundle).getState();
            will(returnValue(Bundle.ACTIVE));
            one(bundle).loadClass("org.apache.sling.test.A"); inSequence(sequence);
            will(returnValue(java.util.Map.class));
            one(bundle).loadClass("org.apache.sling.test.A"); inSequence(sequence);
            will(returnValue(java.util.Map.class));
            one(bundle).loadClass("org.apache.sling.test.A"); inSequence(sequence);
            will(returnValue(java.util.ArrayList.class));
        }});
        DynamicClassLoaderManagerImpl manager = new DynamicClassLoaderManagerImpl(bundleContext, packageAdmin, null,
            new DynamicClassLoaderManagerFactory(bundleContext, packageAdmin));
        final ClassLoader cl = manager.getDynamicClassLoader();
        final Class<?> c1 = cl.loadClass("org.apache.sling.test.A");
        Assert.assertEquals("java.util.Map", c1.getName());
        final Class<?> c2 = cl.loadClass("org.apache.sling.test.A");
        Assert.assertEquals("java.util.Map", c2.getName());
        // as we cache the result, we still get the map!
        final Class<?> c3 = cl.loadClass("org.apache.sling.test.A");
        Assert.assertEquals("java.util.Map", c3.getName());
    }
}
