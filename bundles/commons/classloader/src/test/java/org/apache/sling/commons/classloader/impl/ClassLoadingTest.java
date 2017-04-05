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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;
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
            allowing(packageAdmin).getExportedPackages("org.apache.sling.test");
            will(returnValue(new ExportedPackage[] {ep}));
            allowing(ep).getExportingBundle();
            will(returnValue(bundle));
            allowing(ep).isRemovalPending();
            will(returnValue(false));
            allowing(bundle).getBundleId();
            will(returnValue(2L));
            allowing(bundle).getState();
            will(returnValue(Bundle.ACTIVE));
            allowing(bundle).getSymbolicName();
            will(returnValue("org.apache.sling.test"));
            allowing(bundle).getVersion();
            will(returnValue(new Version("1.0.0")));
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

    @Test public void testLoading_SLING_6258() throws Exception {
        final BundleContext bundleContext = this.context.mock(BundleContext.class);
        final PackageAdmin packageAdmin = this.context.mock(PackageAdmin.class);
        final ExportedPackage ep1 = this.context.mock(ExportedPackage.class, "ep1");
        final ExportedPackage ep2 = this.context.mock(ExportedPackage.class, "ep2");
        final ExportedPackage ep3 = this.context.mock(ExportedPackage.class, "ep3");
        final Bundle bundle1 = this.context.mock(Bundle.class, "bundle1");
        final Bundle bundle2 = this.context.mock(Bundle.class, "bundle2");
        final Bundle bundle3 = this.context.mock(Bundle.class, "bundle3");
        final ClassNotFoundException cnfe = new ClassNotFoundException();
        this.context.checking(new Expectations() {{
            allowing(bundleContext).createFilter(with(any(String.class)));
            will(returnValue(null));
            allowing(bundleContext).getServiceReferences(with(any(String.class)), with((String)null));
            will(returnValue(null));
            allowing(bundleContext).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
            allowing(bundleContext).removeServiceListener(with(any(ServiceListener.class)));
            allowing(packageAdmin).getExportedPackages(with("org.apache.sling.test"));
            will(returnValue(new ExportedPackage[] {ep1, ep2}));
            allowing(packageAdmin).getExportedPackages(with("org.apache.sling.test3"));
            will(returnValue(new ExportedPackage[] {ep3}));

            allowing(ep1).getExportingBundle();
            will(returnValue(bundle1));
            allowing(ep1).isRemovalPending();
            will(returnValue(false));

            allowing(ep2).getExportingBundle();
            will(returnValue(bundle2));
            allowing(ep2).isRemovalPending();
            will(returnValue(false));

            allowing(ep3).getExportingBundle();
            will(returnValue(bundle3));
            allowing(ep3).isRemovalPending();
            will(returnValue(false));

            allowing(bundle1).getBundleId();
            will(returnValue(1L));
            allowing(bundle1).getState();
            will(returnValue(Bundle.ACTIVE));
            allowing(bundle1).getVersion();
            will(returnValue(new Version("1.0.0")));
            allowing(bundle1).getSymbolicName();
            will(returnValue("org.apache.sling.test1"));


            allowing(bundle2).getBundleId();
            will(returnValue(2L));
            allowing(bundle2).getState();
            will(returnValue(Bundle.ACTIVE));
            allowing(bundle2).getVersion();
            will(returnValue(new Version("2.0.0")));
            allowing(bundle2).getSymbolicName();
            will(returnValue("org.apache.sling.test2"));

            allowing(bundle3).getBundleId();
            will(returnValue(3L));
            allowing(bundle3).getState();
            will(returnValue(Bundle.ACTIVE));
            allowing(bundle3).getVersion();
            will(returnValue(new Version("1.2.3")));
            allowing(bundle3).getSymbolicName();
            will(returnValue("org.apache.sling.test3"));

            allowing(bundle1).loadClass("org.apache.sling.test.T1");
            will(throwException(cnfe));
            allowing(bundle1).getResource("org/apache/sling/test/T3.class");
            will(returnValue(new URL("jar:file:/ws/org.apache.sling.test1.jar!/org/apache/sling/test/T3.class")));

            allowing(bundle2).loadClass("org.apache.sling.test.T1");
            will(returnValue(ArrayList.class));
            allowing(bundle2).loadClass("org.apache.sling.test.T2");
            will(throwException(new ClassNotFoundException()));
            allowing(bundle2).getResource("org/apache/sling/test/T3.class");
            will(returnValue(null));
            allowing(bundle2).getResources("org/apache/sling/test/T3.class");
            will(returnValue(null));

            allowing(bundle3).getResource("org/apache/sling/test3/T4.class");
            will(returnValue(new URL("jar:file:/ws/org.apache.sling.test3.jar!/org/apache/sling/test3/T4.class")));
            allowing(bundle3).getResources("org/apache/sling/test3/T4.class");
            will(returnValue(Collections.enumeration(new ArrayList<URL>() {{
                add(new URL("jar:file:/ws/org.apache.sling.test3.jar!/org/apache/sling/test3/T4.class"));
            }})));
        }});
        DynamicClassLoaderManagerImpl manager = new DynamicClassLoaderManagerImpl(bundleContext, packageAdmin, null,
                new DynamicClassLoaderManagerFactory(bundleContext, packageAdmin));
        final ClassLoader cl = manager.getDynamicClassLoader();
        Assert.assertEquals(ArrayList.class, cl.loadClass("org.apache.sling.test.T1"));
        try {
            cl.loadClass("org.apache.sling.test.T2");
        } catch (Exception e) {
            Assert.assertEquals(ClassNotFoundException.class, e.getClass());
        }
        Assert.assertNull(cl.getResource("org/apache/sling/test/T3.class"));
        Assert.assertFalse(cl.getResources("org/apache/sling/test/T3.class").hasMoreElements());
        Assert.assertEquals("jar:file:/ws/org.apache.sling.test3.jar!/org/apache/sling/test3/T4.class", cl.getResource
                ("org/apache/sling/test3/T4.class").toString());
        Enumeration<URL> resourceURLs = cl.getResources("org/apache/sling/test3/T4.class");
        int count = 0;
        URL lastURL = new URL("https://sling.apache.org");
        while (resourceURLs.hasMoreElements()) {
            count++;
            lastURL = resourceURLs.nextElement();
        }
        Assert.assertEquals(1, count);
        Assert.assertEquals("jar:file:/ws/org.apache.sling.test3.jar!/org/apache/sling/test3/T4.class", lastURL.toString());
    }
}
