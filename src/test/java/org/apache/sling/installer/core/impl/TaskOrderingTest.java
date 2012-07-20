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
package org.apache.sling.installer.core.impl;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.installer.core.impl.tasks.BundleInstallTask;
import org.apache.sling.installer.core.impl.tasks.BundleRemoveTask;
import org.apache.sling.installer.core.impl.tasks.BundleStartTask;
import org.apache.sling.installer.core.impl.tasks.BundleUpdateTask;
import org.apache.sling.installer.core.impl.tasks.MockInstallationListener;
import org.apache.sling.installer.core.impl.tasks.RefreshBundlesTask;

/** Test the ordering and duplicates elimination of
 * 	OsgiControllerTasks
 */
public class TaskOrderingTest {

    private Set<InstallTask> taskSet;

	@org.junit.Before public void setUp() {
	    // The data type must be consistent with the "tasks" member
	    // of the {@link OsgiControllerImpl} class.
		taskSet = new TreeSet<InstallTask>();
	}

	private static EntityResourceList getRegisteredResource(String url) throws IOException {
        new FileDataStore(new MockBundleContext());
        final InternalResource internal = InternalResource.create("test",
                new InstallableResource(url, null, new Hashtable<String, Object>(), null, null, null));
        RegisteredResourceImpl rr = RegisteredResourceImpl.create(internal);
        TransformationResult[] tr = new DefaultTransformer().transform(rr);
        if ( tr == null ) {
            final TransformationResult result = new TransformationResult();
            result.setId(url);
            result.setResourceType(InstallableResource.TYPE_CONFIG);
            tr = new TransformationResult[] {
                      result
            };
        }
        rr = (RegisteredResourceImpl)rr.clone(tr[0]);

        final EntityResourceList erl = new EntityResourceList("test", new MockInstallationListener());
	    erl.addOrUpdate(rr);
	    return erl;
	}

	private void assertOrder(int testId, Collection<InstallTask> actual, InstallTask [] expected) {
		int index = 0;
		for(InstallTask t : actual) {
			if(!t.equals(expected[index])) {
				fail("Test " + testId + ": at index " + index + ", expected " + expected[index] + " but got " + t);
			}
			index++;
		}
	}

	@org.junit.Test
	public void testBasicOrdering() throws Exception {
		int testIndex = 1;
		final InstallTask [] tasksInOrder = {
		    new BundleRemoveTask(getRegisteredResource("test:url"), null),
            new BundleInstallTask(getRegisteredResource("test:url"), null),
		    new BundleUpdateTask(getRegisteredResource("test:url"), null),
            new RefreshBundlesTask(null),
			new BundleStartTask(null, 0, null)
		};

		taskSet.clear();
		taskSet.add(tasksInOrder[4]);
		taskSet.add(tasksInOrder[3]);
		taskSet.add(tasksInOrder[2]);
        taskSet.add(tasksInOrder[1]);
        taskSet.add(tasksInOrder[0]);

		assertOrder(testIndex++, taskSet, tasksInOrder);

		taskSet.clear();
        taskSet.add(tasksInOrder[0]);
        taskSet.add(tasksInOrder[1]);
		taskSet.add(tasksInOrder[2]);
		taskSet.add(tasksInOrder[3]);
		taskSet.add(tasksInOrder[4]);

		assertOrder(testIndex++, taskSet, tasksInOrder);

		taskSet.clear();
		taskSet.add(tasksInOrder[3]);
		taskSet.add(tasksInOrder[2]);
        taskSet.add(tasksInOrder[0]);
		taskSet.add(tasksInOrder[4]);
        taskSet.add(tasksInOrder[1]);

		assertOrder(testIndex++, taskSet, tasksInOrder);

		taskSet.clear();
		taskSet.add(tasksInOrder[4]);
        taskSet.add(tasksInOrder[0]);
		taskSet.add(tasksInOrder[2]);
		taskSet.add(tasksInOrder[3]);
        taskSet.add(tasksInOrder[1]);

		assertOrder(testIndex++, taskSet, tasksInOrder);
	}

	@org.junit.Test
	public void testMultipleConfigAndBundles() throws Exception {
		int testIndex = 1;
		final InstallTask [] tasksInOrder = {
			new BundleInstallTask(getRegisteredResource("test:someURIa.nothing"), null),
            new BundleInstallTask(getRegisteredResource("test:someURIb.nothing"), null),
            new RefreshBundlesTask(null),
			new BundleStartTask(null, 0, null)
		};

		taskSet.clear();
		for(int i = tasksInOrder.length -1 ; i >= 0; i--) {
			taskSet.add(tasksInOrder[i]);
		}

		assertOrder(testIndex++, taskSet, tasksInOrder);

        taskSet.clear();
        for(int i = 0 ; i < tasksInOrder.length; i++) {
            taskSet.add(tasksInOrder[i]);
        }

        assertOrder(testIndex++, taskSet, tasksInOrder);
	}

	@org.junit.Test
	public void testMultipleRefreshAndStart() throws Exception {
		int testIndex = 1;
		final InstallTask [] tasksInOrder = {
		    new BundleRemoveTask(getRegisteredResource("test:url"), null),
            new RefreshBundlesTask(null),
			new BundleStartTask(null, 0, null),
			new BundleStartTask(null, 1, null)
		};

		taskSet.clear();
		taskSet.add(tasksInOrder[3]);
		taskSet.add(tasksInOrder[3]);
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(tasksInOrder[2]);
		taskSet.add(tasksInOrder[2]);
		taskSet.add(tasksInOrder[1]);
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(tasksInOrder[0]);
		taskSet.add(tasksInOrder[3]);
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(tasksInOrder[3]);
		taskSet.add(tasksInOrder[2]);
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(tasksInOrder[2]);
		taskSet.add(tasksInOrder[1]);
		taskSet.add(new RefreshBundlesTask(null));
		taskSet.add(tasksInOrder[1]);
		taskSet.add(new RefreshBundlesTask(null));

		assertOrder(testIndex++, taskSet, tasksInOrder);
	}

	@org.junit.Test
	public void testBundleStartOrder() {
		int testIndex = 1;
		final InstallTask [] tasksInOrder = {
			new BundleStartTask(null, 0, null),
			new BundleStartTask(null, 1, null),
			new BundleStartTask(null, 5, null),
			new BundleStartTask(null, 11, null),
			new BundleStartTask(null, 51, null)
		};

		taskSet.clear();
		for(int i = tasksInOrder.length -1 ; i >= 0; i--) {
			taskSet.add(tasksInOrder[i]);
		}
		assertOrder(testIndex++, taskSet, tasksInOrder);

        taskSet.clear();
        for(int i = 0 ; i < tasksInOrder.length; i++) {
            taskSet.add(tasksInOrder[i]);
        }

        assertOrder(testIndex++, taskSet, tasksInOrder);
	}
}
