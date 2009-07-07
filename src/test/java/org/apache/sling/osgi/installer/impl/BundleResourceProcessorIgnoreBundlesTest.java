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
package org.apache.sling.osgi.installer.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

/** Test ignoring bundle updates based on Versions (SLING-1001) */
public class BundleResourceProcessorIgnoreBundlesTest {

	private final String symbolicName = "testbundle";
	private final String uri = "testuri";
	private BundleResourceProcessor brp;
	private Mockery mockery;
	
    @org.junit.Before public void setup() {
        mockery = new Mockery();
        final BundleContext bc = mockery.mock(BundleContext.class);
        final PackageAdmin pa = mockery.mock(PackageAdmin.class);
        
        mockery.checking(new Expectations() {{
            allowing(bc).addFrameworkListener(with(any(FrameworkListener.class)));
        }});
        
        brp = new BundleResourceProcessor(bc, pa);
    }

	@org.junit.Test public void testLowerVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.0");
		assertTrue("Lower version must be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testHigherVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.2");
		assertFalse("Higher version must not be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testSameVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.1");
		assertTrue("Same version must be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testSameVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.5.incubator-SNAPSHOT");
		assertFalse("Same version snapshot must not be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testLowerVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.4.incubator-SNAPSHOT");
		assertTrue("Lower version snapshot must be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testHigherVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.6.incubator-SNAPSHOT");
		assertFalse("Higher version snapshot must not be ignored",
				brp.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}
}
