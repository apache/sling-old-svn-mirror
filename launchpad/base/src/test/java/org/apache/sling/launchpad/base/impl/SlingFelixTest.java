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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import junit.framework.AssertionFailedError;

import org.apache.sling.launchpad.base.shared.Notifiable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.packageadmin.PackageAdmin;

public class SlingFelixTest {

    public static final int N_START_STOP = 100;
    public static final long WAIT_FOR_STOP_TIMEOUT_MSEC = 1000;
    public static final long STOPPED_CALLED_TIMEOUT_MSEC = 5000L;
    private final TestNotifiable notifiable = new TestNotifiable();

    private SlingFelix framework;

    @Before
    public void setup() {
        startSling();
    }
    
    @After
    public void tearDown() {
        stopSling();
    }
    
    @Test
    public void testMultipleStop() {
        startSling();
        for(int i=0; i < N_START_STOP; i++) {
            stopSling();
        }
    }

    @Test
    public void testMultipleStartStop() {
        stopSling();
        for(int i=0; i < N_START_STOP; i++) {
            startSling();
            stopSling();
        }
    }

    @Test
    public void test_start_stop() {
        assertNotNull(framework);
        assertEquals(Bundle.ACTIVE, framework.getState());

        stopSling();
        
        // as the notifiable is notified async we wait
        final long start = System.currentTimeMillis();
        while ( !this.notifiable.stoppedCalled ) {
            // timeout on this wait
            if ( System.currentTimeMillis() - start > STOPPED_CALLED_TIMEOUT_MSEC ) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignore) {
            }
        }
        assertTrue("Expect Notifiable.stopped to be called", this.notifiable.stoppedCalled);
        assertFalse("Expect Notifiable.updated to not be called", this.notifiable.updatedCalled);
        assertNull("Expect Notifiable.updated to not be called", this.notifiable.updatedCalledFile);
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

        PackageAdmin pa = getService(framework, PackageAdmin.class);
        assertNotNull(pa);

        assertNull("Integer class provided by the VM not from a bundle", pa.getBundle(Integer.class));
        assertEquals("BundleContext class must come from the framework", framework.getBundle(),
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

        final SlingFelix sf2 = startFramework(new TestNotifiable());

        try {
            InputStream ins = getClass().getResourceAsStream("/test1.jar");
            framework.getBundleContext().installBundle("test1", ins).start();

            Runnable r = getService(framework, Runnable.class);
            r.run();

        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError(e.toString()).initCause(e);
        } finally {
            stopFramework(sf2);
        }
    }

    private void startSling() {
        stopSling();
        framework = startFramework(this.notifiable);
    }

    private static SlingFelix startFramework(final Notifiable notifiable) {
        final String baseDir = System.getProperty("basedir");
        if (baseDir == null) {
            fail("Need the basedir system property to locate the framework folder");
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
            fail("Failed to start OSGi Framework: " + e);
            return null; // to keep the compiler cool
        }
    }
    
    private void stopSling() {
        stopFramework(framework);
    }

    private static void stopFramework(Framework f) {
        if(f == null) {
            return;
        }
        try {
            f.stop();
            if (f.waitForStop(WAIT_FOR_STOP_TIMEOUT_MSEC).getType() == FrameworkEvent.WAIT_TIMEDOUT) {
                fail("Timed out waiting for framework to stop, after " + WAIT_FOR_STOP_TIMEOUT_MSEC + " msec");
            }
        } catch (Exception e) {
            fail("Cannot stop OSGi Framework: " + e);
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

        volatile boolean stoppedCalled = false;

        volatile boolean updatedCalled = false;

        volatile File updatedCalledFile = null;

        public void stopped() {
            this.stoppedCalled = true;
        }

        public void updated(File tmpFile) {
            this.updatedCalled = true;
            this.updatedCalledFile = tmpFile;
        }

    }
}