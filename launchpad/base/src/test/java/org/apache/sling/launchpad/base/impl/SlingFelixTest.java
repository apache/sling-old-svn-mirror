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
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.sling.launchpad.base.shared.Notifiable;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.packageadmin.PackageAdmin;

public class SlingFelixTest {

    private final TestNotifiable notifiable = new TestNotifiable();

    private SlingFelix framework;

    @After
    public void tearDown() {
        stopSling();
    }

    @Test
    public void test_start_stop() {
        SlingFelix sf = startSling();
        TestCase.assertNotNull(sf);
        TestCase.assertEquals(Bundle.ACTIVE, sf.getState());

        stopSling();
        TestCase.assertNull("Expect the framework field to be cleared", this.framework);

        TestCase.assertTrue("Expect Notifiable.stopped to be called", this.notifiable.stoppedCalled);
        TestCase.assertFalse("Expect Notifiable.updated to not be called", this.notifiable.updatedCalled);
        TestCase.assertNull("Expect Notifiable.updated to not be called", this.notifiable.updatedCalledFile);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test_Felix_getBundle_Class_from_PackageAdmin() {

        /*
         * Tests whether the Felix.getBundle(Class) call from PackageAdmin
         * is possible. This test should always succeed regardless of the
         * SLING-2554 implementation because it uses regular Java call
         * methodology.
         */

        SlingFelix sf = startSling();

        PackageAdmin pa = getService(sf, PackageAdmin.class);
        TestCase.assertNotNull(pa);

        TestCase.assertNull("Integer class provided by the VM not from a bundle", pa.getBundle(Integer.class));
        TestCase.assertEquals("BundleContext class must come from the framework", sf.getBundle(),
            pa.getBundle(BundleContext.class));
    }

    @Test
    public void test_Felix_getBundle_Class_from_UrlStreamHandler() {

        /*
         * Tests whether the Felix.getBundle(Class) method can be called
         * from the URLHandlers class. This call may fail if SLING-2554
         * does not work because it uses reflection on the class of the
         * framework instance, which happens to be SlingFelix instead of
         * Felix
         */

        SlingFelix sf = startSling();
        SlingFelix sf2 = doStartSling(new TestNotifiable());

        try {
            InputStream ins = getClass().getResourceAsStream("/test1.jar");
            sf.getBundleContext().installBundle("test1", ins).start();

            Runnable r = getService(sf, Runnable.class);
            r.run();

        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError(e.toString()).initCause(e);
        } finally {
            doStopSling(sf2);
        }
    }

    private SlingFelix startSling() {
        if (this.framework == null) {
            this.framework = doStartSling(this.notifiable);
        }

        return this.framework;
    }

    private static SlingFelix doStartSling(final Notifiable notifiable) {
        final String baseDir = System.getProperty("basedir");
        if (baseDir == null) {
            TestCase.fail("Need the basedir system property to locate the framework folder");
        }
        File fwDir = new File(baseDir + "/target/felix." + System.nanoTime());
        fwDir.mkdirs();

        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.FRAMEWORK_STORAGE, fwDir.getAbsolutePath());
        props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        props.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "30");

        try {
            SlingFelix fw = new SlingFelix(notifiable, props);
            fw.start();
            return fw;
        } catch (Exception e) {
            TestCase.fail("Failed to start OSGi Framework: " + e);
            return null; // to keep the compiler cool
        }
    }

    private void stopSling() {
        if (this.framework != null) {
            try {
                doStopSling(this.framework);
            } finally {
                this.framework = null;
            }
        }
    }

    private static void doStopSling(final SlingFelix framework) {
        try {
            framework.stop();
            if (framework.waitForStop(10L).getType() == FrameworkEvent.WAIT_TIMEDOUT) {
                TestCase.fail("Timed out waiting for framework to stop");
            }
        } catch (Exception e) {
            TestCase.fail("Cannot stop OSGi Framework: " + e);
        }
    }

    private <T> T getService(final Framework framework, Class<T> type) {
        ServiceReference<T> ref = framework.getBundleContext().getServiceReference(type);
        if (ref != null) {
            return framework.getBundleContext().getService(ref);
        }
        return null;
    }

    private static class TestNotifiable implements Notifiable {

        boolean stoppedCalled = false;

        boolean updatedCalled = false;

        File updatedCalledFile = null;

        public void stopped() {
            this.stoppedCalled = true;
        }

        public void updated(File tmpFile) {
            this.updatedCalled = true;
            this.updatedCalledFile = tmpFile;
        }

    }
}
