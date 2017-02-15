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
package org.apache.sling.installer.it;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.sling.installer.it.BundleInstallUpgradeExceptionRetryTest.BundleExceptionActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

@RunWith(PaxExam.class)
public class BundleInstallUpgradeExceptionRetryTest extends OsgiInstallerTestBase {

    private AtomicInteger ac;
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }

    @Before
    public void setUp() throws IOException, BundleException {
        setupInstaller();
        File cb = createCounterBundle();
        bundleContext.installBundle(cb.toURI().toURL().toExternalForm())
            .start();
        this.ac = getService(AtomicInteger.class);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testUpdateException() throws Exception {
        final String symbolicName = "osgi-installer-testbundle";
        assertNull("Test bundle must be absent before installing", findBundle(symbolicName));

        // install version 1.0
        {
            ac.set(0);
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME,
                getInstallableResource(createTestBundle(symbolicName, Version.parseVersion("1.0.0"))), null);
            this.waitForBundleEvents(symbolicName + " should be installed with version 1.0.0", listener,
                new BundleEvent(symbolicName, "1.0.0", org.osgi.framework.BundleEvent.INSTALLED),
                new BundleEvent(symbolicName, "1.0.0", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After installing", symbolicName, "1.0.0", Bundle.ACTIVE);
        }
        // install version 1.1 but fail activation with exception all attempts
        {
            ac.set(6);
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME,
                getInstallableResource(createTestBundle(symbolicName, Version.parseVersion("1.1.0"))), null);
            this.assertNoBundleEvents("Exception in activate should surpress successfull installation", listener,
                symbolicName);
            assertBundle("After installing", symbolicName, "1.0.0", Bundle.ACTIVE);
        }

        // install version 1.2 but succeeding after 3 attempts
        {
            ac.set(3);
            final Object listener = this.startObservingBundleEvents();
            installer.updateResources(URL_SCHEME,
                getInstallableResource(createTestBundle(symbolicName, Version.parseVersion("1.2.0"))), null);
            this.waitForBundleEvents(symbolicName + " should be installed with version 1.0.0", listener,
                new BundleEvent(symbolicName, "1.0.0", org.osgi.framework.BundleEvent.STOPPED),
                new BundleEvent(symbolicName, "1.2.0", org.osgi.framework.BundleEvent.STARTED));
            assertBundle("After installing", symbolicName, "1.2.0", Bundle.ACTIVE);
        }
    }
    
    private static File createCounterBundle() throws IOException {
        Class<ActivationCounterActivator> activator = ActivationCounterActivator.class;
        String manifest = "Bundle-SymbolicName: test.counter\n"
            + "Bundle-Version: 1.0.0 \n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n"
            + "Manifest-Version: 1.0\n"
            + "Bundle-Activator: " + activator.getName() + "\n\n";
        return createBundle("counterbundle", manifest, activator);
    }

    private static File createTestBundle(String symbolicName, Version version) throws IOException {
        Class<BundleExceptionActivator> activator = BundleExceptionActivator.class;
        String manifest = "Bundle-SymbolicName: " + symbolicName + "\n"
            + "Bundle-Version: " + version.toString() + "\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Import-Package: org.osgi.framework\n"
            + "Manifest-Version: 1.0\n"
            + "Bundle-Activator: " + activator.getName() + "\n\n";
        return createBundle("testbundle", manifest, activator);
    }

    private static File createBundle(String name, String manifest, Class... classes) throws IOException {
        File f = File.createTempFile(name, ".jar");
        f.deleteOnExit();

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);

        for (Class clazz : classes) {
            String path = clazz.getName().replace('.', '/') + ".class";
            os.putNextEntry(new ZipEntry(path));

            InputStream is = clazz.getClassLoader().getResourceAsStream(path);
            byte[] buffer = new byte[8 * 1024];
            for (int i = is.read(buffer); i != -1; i = is.read(buffer)) {
                os.write(buffer, 0, i);
            }
            is.close();
            os.closeEntry();
        }
        os.close();
        return f;
    }

    public static final class BundleExceptionActivator implements BundleActivator, Runnable {

        @Override
        public void start(BundleContext context) throws Exception {
            final ServiceReference<AtomicInteger> ref = context.getServiceReference(AtomicInteger.class);
            AtomicInteger ac = context.getService(ref);
            if (ac.getAndDecrement() >= 0) {
                throw new BundleException("Simulate exception");
            }
        }

        @Override
        public void run() {
            // nothing to do

        }

        @Override
        public void stop(BundleContext context) throws Exception {
            // nothing to do
        }

    }

    public static final class ActivationCounterActivator implements BundleActivator {

        private BundleContext context;

        @Override
        public void start(BundleContext context) throws Exception {
            this.context = context;
            context.registerService(AtomicInteger.class, new AtomicInteger(0), null);
        }

        @Override
        public void stop(BundleContext context) throws Exception {
            // nothing to do
        }

    }
}