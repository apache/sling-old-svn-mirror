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
package org.apache.sling.osgi.installer.impl.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.osgi.installer.impl.MockOsgiControllerContext;
import org.osgi.framework.Version;

/** Test ignoring bundle updates based on Versions (SLING-1001) */
public class BundleInstallRemoveIgnoreTest {

	private final String symbolicName = "testbundle";
	private final String uri = "testuri";
	private BundleInstallRemoveTask task;
	
    @org.junit.Before public void setup() {
        task = new BundleInstallRemoveTask(null, null, new MockOsgiControllerContext());
    }

	@org.junit.Test public void testLowerVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.0");
		assertTrue("Lower version must be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testHigherVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.2");
		assertFalse("Higher version must not be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testSameVersion() {
		final Version installedVersion = new Version("1.1");
		final Version newVersion = new Version("1.1");
		assertTrue("Same version must be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testSameVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.5.incubator-SNAPSHOT");
		assertFalse("Same version snapshot must not be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testLowerVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.4.incubator-SNAPSHOT");
		assertTrue("Lower version snapshot must be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}

	@org.junit.Test public void testHigherVersionSnapshot() {
		final Version installedVersion = new Version("2.0.5.incubator-SNAPSHOT");
		final Version newVersion = new Version("2.0.6.incubator-SNAPSHOT");
		assertFalse("Higher version snapshot must not be ignored",
				task.ignoreNewBundle(symbolicName, uri, installedVersion, newVersion));
	}
}
