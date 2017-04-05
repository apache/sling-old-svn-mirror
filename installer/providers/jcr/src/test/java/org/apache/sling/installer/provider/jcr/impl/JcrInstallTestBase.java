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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.EventHelper;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

/** Base test class with common utilities */
public abstract class JcrInstallTestBase  {
    public static final long TIMEOUT = 5000L;
    
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_JACKRABBIT);

    protected Session session;
    protected EventHelper eventHelper;
    protected ContentHelper contentHelper;
    protected JcrInstaller installer;
    protected MockOsgiInstaller osgiInstaller;

    @Before
    public void setUp() throws Exception {

        session = context.resourceResolver().adaptTo(Session.class);
        eventHelper = new EventHelper(session);
        contentHelper = new ContentHelper(session);
        contentHelper.cleanupContent();
        if(needsTestContent()) {
            contentHelper.setupContent();
        }
        osgiInstaller = new MockOsgiInstaller();
        context.registerService(OsgiInstaller.class, osgiInstaller);
        context.runMode(MiscUtil.RUN_MODES);
        
        installer = new JcrInstaller();
        context.registerInjectActivateService(installer);
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws Exception {

        contentHelper.cleanupContent();
        eventHelper = null;
        contentHelper = null;
        installer.deactivate(context.componentContext());
        MiscUtil.waitForInstallerThread(installer, TIMEOUT);
    }

    protected boolean needsTestContent() {
        return true;
    }

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
