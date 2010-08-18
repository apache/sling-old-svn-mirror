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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.impl.MockBundleResource;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleTaskCreatorTest {
	public static final String SN = "TestSymbolicName";

	private SortedSet<OsgiInstallerTask> getTasks(RegisteredResource [] resources, BundleTaskCreator btc) throws IOException {
		final SortedSet<RegisteredResource> s = new TreeSet<RegisteredResource>();
		for(RegisteredResource r : resources) {
			s.add(r);
		}

		final SortedSet<OsgiInstallerTask> tasks = new TreeSet<OsgiInstallerTask>();
		btc.createTasks(s, tasks);
		return tasks;
	}

	@Test
	public void testSingleBundleNew() throws IOException {
		final RegisteredResource [] r = {
				new MockBundleResource(SN, "1.0")
		};
        final MockBundleTaskCreator c = new MockBundleTaskCreator();
		final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
		assertEquals("Expected one task", 1, s.size());
		assertTrue("Expected a BundleInstallTask", s.first() instanceof BundleInstallTask);
	}

	@Test
    public void testSingleBundleAlreadyInstalled() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.0")
        };

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected no tasks, same version is active", 0, s.size());
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.RESOLVED);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected no tasks, same version is installed", 0, s.size());
        }
    }

    @Test
    public void testBundleUpgrade() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.1")
        };

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeBothRegistered() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.1"),
                new MockBundleResource(SN, "1.0")
        };

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeBothRegisteredReversed() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.0"),
                new MockBundleResource(SN, "1.1")
        };

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeSnapshot() throws IOException {
        // Need to use OSGi-compliant version number, in bundles
        // bnd and other tools generate correct numbers.
        final String v = "2.0.7.SNAPSHOT";
        final RegisteredResource [] r = {
                new MockBundleResource(SN, v)
        };

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, v, Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleRemoveSingle() throws IOException {
        final String version = "1.0";
        final RegisteredResource [] r = {
                new MockBundleResource(SN, version)
        };
        r[0].setInstallable(false);

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected no tasks, bundle was not installed by us", 0, s.size());
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.saveInstalledBundleInfo(SN, r[0].getDigest(), version);
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task, as we installed that bundle", 1, s.size());
            assertTrue("Expected a BundleRemoveTask", s.first() instanceof BundleRemoveTask);
        }
    }

    @Test
    public void testBundleRemoveMultiple() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.0"),
                new MockBundleResource(SN, "1.1"),
                new MockBundleResource(SN, "2.0")
        };
        for(RegisteredResource x : r) {
            x.setInstallable(false);
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected no tasks, bundle was not installed by us", 0, s.size());
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            c.saveInstalledBundleInfo(SN, r[1].getDigest(), "1.1");
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task, as we installed that bundle", 1, s.size());
            assertTrue("Expected a BundleRemoveTask", s.first() instanceof BundleRemoveTask);
        }
    }

    @Test
    public void testDowngradeOfRemovedResource() throws IOException {
        final RegisteredResource [] r = {
                new MockBundleResource(SN, "1.0.0"),
                new MockBundleResource(SN, "1.1.0"),
        };

        // Simulate V1.1 installed but resource is gone -> downgrade to 1.0
        r[1].setInstallable(false);

       {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.1.0", Bundle.ACTIVE);
            c.saveInstalledBundleInfo(SN, "fakedigest", "1.1.0");
            final SortedSet<OsgiInstallerTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
            final BundleUpdateTask t = (BundleUpdateTask)s.first();
            assertEquals("Update should be to V1.0", r[0], t.getResource());
        }
    }
}