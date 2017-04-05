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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

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
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

@RunWith(PaxExam.class)
public class BundleInstallUpgradeExceptionRetryTest extends OsgiInstallerTestBase {

	@org.ops4j.pax.exam.Configuration
	public Option[] config() {
		return defaultConfiguration();
	}

	@Before
	public void beforeTest() throws Exception {
		setupInstaller();

		final Object listener = this.startObservingBundleEvents();
		installer.updateResources(URL_SCHEME, getInstallableResource(createTestBundle(new Version("1.0.0"))), null);

		this.waitForBundleEvents(symbolicName + " should be installed with version 1.0.0", listener,
				new BundleEvent(symbolicName, "1.0.0", org.osgi.framework.BundleEvent.STARTED));

		assertBundle("After installing", symbolicName, "1.0.0", Bundle.ACTIVE);
	}

	static final String symbolicName = "osgi-installer-testbundle";

	@After
	public void afterTest() throws BundleException {
		super.tearDown();
	}

	@Test
	public void testUpdateFailsWithMoreThanMaxRetrys() throws Exception {
		// install version 1.1 and fail activation with exception all attempts
		final Object listener = this.startObservingBundleEvents();
		final AtomicReference<ServiceRegistration<AtomicInteger>> ref = new AtomicReference<ServiceRegistration<AtomicInteger>>(
				null);
		final AtomicInteger counter = new AtomicInteger(5);
		bundleContext.addBundleListener(new SynchronousBundleListener() {

			@Override
			public void bundleChanged(org.osgi.framework.BundleEvent event) {
				if (event.getType() == org.osgi.framework.BundleEvent.STOPPED
						&& event.getBundle().getSymbolicName().equals(symbolicName)
						&& event.getBundle().getVersion().equals(new Version("1.0.0"))) {
					System.out.println(event.getSource() + " " + event.getType());
					Thread.dumpStack();
					if (ref.get() == null) {
						try {
							event.getBundle().start();
							ref.set(bundleContext.registerService(AtomicInteger.class, counter, null));
						} catch (BundleException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						ref.getAndSet(null).unregister();
						if (counter.get() == 0) {
							bundleContext.removeBundleListener(this);
						}
					}
				}
			}
		});

		installer.updateResources(URL_SCHEME, getInstallableResource(createTestBundle(new Version("2.0.0"))), null);

		try {
			long time = 0;
			while (counter.get() >= 0 && time < 1000) {
				sleep(100);
				time += 100;
			}

			assertBundle("After installing", symbolicName, "1.0.0", Bundle.ACTIVE);
		} finally {
			if (ref.get() != null) {
				ref.get().unregister();
			}
		}
	}

	@Test
	public void testUpdateSuccedsWithLessThanMaxRetrys() throws Exception {
		// install version 1.1 and fail activation with exception all attempts
		final Object listener = this.startObservingBundleEvents();
		final AtomicReference<ServiceRegistration<AtomicInteger>> ref = new AtomicReference<ServiceRegistration<AtomicInteger>>(
				null);
		final AtomicInteger counter = new AtomicInteger(3);
		bundleContext.addBundleListener(new SynchronousBundleListener() {

			@Override
			public void bundleChanged(org.osgi.framework.BundleEvent event) {
				if (event.getType() == org.osgi.framework.BundleEvent.STOPPED
						&& event.getBundle().getSymbolicName().equals(symbolicName)
						&& event.getBundle().getVersion().equals(new Version("1.0.0"))) {
					if (ref.get() == null) {
						try {
							event.getBundle().start();
							ref.set(bundleContext.registerService(AtomicInteger.class, counter, null));
						} catch (BundleException e) {
						}
					} else {
						ref.getAndSet(null).unregister();
						if (counter.get() == 0) {
							bundleContext.removeBundleListener(this);
						}
					}
				}
			}
		});

		installer.updateResources(URL_SCHEME, getInstallableResource(createTestBundle(new Version("2.0.0"))), null);

		try {
			long time = 0;
			while (counter.get() >= 0 && time < 1000) {
				sleep(100);
				time += 100;
			}

			assertBundle("After installing", symbolicName, "2.0.0", Bundle.ACTIVE);
		} finally {
			if (ref.get() != null) {
				ref.get().unregister();
			}
		}
	}

	private static File createTestBundle(Version version) throws IOException {
		String manifest = "Bundle-SymbolicName: " + symbolicName + "\n" + "Bundle-Version: " + version + "\n"
				+ "Bundle-ManifestVersion: 2\n" + "Bundle-Activator: " + ThrowingActivator.class.getName() + "\n"
				+ "Import-Package: org.osgi.framework\n" + "Manifest-Version: 1.0\n\n";
		return createBundle("testbundle", manifest, ThrowingActivator.class);
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

	public static class ThrowingActivator implements BundleActivator {
		@Override
		public void start(BundleContext context) throws Exception {

		}

		@Override
		public void stop(BundleContext context) throws Exception {
			ServiceReference<AtomicInteger> ref = context.getServiceReference(AtomicInteger.class);
			if (ref != null && context.getService(ref).getAndDecrement() >= 0) {
				throw new Exception("Force exception for update");
			}
		}
	}
}
