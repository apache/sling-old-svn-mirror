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
package org.apache.sling.installer.provider.jcr.impl;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.EventHelper;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;

/** Base test class with common utilities */
abstract class JcrInstallTestBase extends RepositoryTestBase {
    public static final long TIMEOUT = 5000L;

    SlingRepository repo;
    Session session;
    protected EventHelper eventHelper;
    protected ContentHelper contentHelper;
    protected JcrInstaller installer;
    protected MockOsgiInstaller osgiInstaller;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = getRepository();
        session = repo.loginAdministrative(repo.getDefaultWorkspace());
        eventHelper = new EventHelper(session);
        contentHelper = new ContentHelper(session);
        contentHelper.cleanupContent();
        if(needsTestContent()) {
            contentHelper.setupContent();
        }
        osgiInstaller = new MockOsgiInstaller();
        installer = MiscUtil.getJcrInstaller(repo, osgiInstaller);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        contentHelper.cleanupContent();
        session.logout();
        eventHelper = null;
        contentHelper = null;
        installer.deactivate(MiscUtil.getMockComponentContext());
        MiscUtil.waitForInstallerThread(installer, TIMEOUT);
    }

    protected abstract boolean needsTestContent();

    protected void assertRegisteredPaths(String [] paths) {
        for(String path : paths) {
            assertRegistered(path, true);
        }
    }

    protected void assertRegistered(String path, boolean registered) {
        assertRegistered(null, path, registered);
    }

    protected void assertRegistered(String info, String path, boolean registered) {
        if(info == null) {
            info = "";
        } else {
            info += ": ";
        }

        if(registered) {
            assertTrue(info + "Expected " + path + " to be registered",
                    osgiInstaller.isRegistered(JcrInstaller.URL_SCHEME, path));
        } else {
            assertFalse(info + "Expected " + path + " to be unregistered",
                    osgiInstaller.isRegistered(JcrInstaller.URL_SCHEME, path));
        }
    }

    protected void assertRecordedCall(String action, String path) {
        final String callStr = action + ":" + JcrInstaller.URL_SCHEME + ":" + path;
        boolean found = false;
        for(String call : osgiInstaller.getRecordedCalls()) {
            if(call.startsWith(callStr)) {
                found = true;
                break;
            }
        }
        assertTrue("Expecting '" + callStr + "' in recorded calls (" + osgiInstaller.getRecordedCalls() + ")", found);
    }
}
