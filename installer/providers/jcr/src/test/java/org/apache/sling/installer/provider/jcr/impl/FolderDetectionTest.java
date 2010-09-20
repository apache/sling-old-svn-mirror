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


/** Test that changes in folders to watch are correctly detected,
 *  including when root folders are created or deleted
 */
public class FolderDetectionTest extends JcrInstallTestBase {

    protected boolean needsTestContent() {
        return false;
    }

    public void testCreateAndDeleteLibs() throws Exception {
        final String res = "/libs/foo/install/somefile.jar";
        assertRegistered("Before test", res, false);

        assertFalse("/libs must not exist when test starts", session.itemExists("/libs"));
        contentHelper.createFolder("/libs");
        contentHelper.createFolder("/libs/foo");
        contentHelper.createFolder("/libs/foo/install");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        contentHelper.createOrUpdateFile(res);
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertRegistered("After creating libs and test file", res, true);

        contentHelper.delete("/libs");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertRegistered("After deleting libs", res, false);
    }

    public void testMoveLibsToFoo() throws Exception {
        final String res = "/libs/foo/install/somefile.jar";
        assertRegistered("Before test", res, false);

        assertFalse("/libs must not exist when test starts", session.itemExists("/libs"));
        contentHelper.createFolder("/libs");
        contentHelper.createFolder("/libs/foo");
        contentHelper.createFolder("/libs/foo/install");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        assertFalse("/foo must not exist when test starts", session.itemExists("/foo"));
        contentHelper.createFolder("/foo");

        contentHelper.createOrUpdateFile(res);
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        assertRegistered("After creating libs and test file", res, true);

        session.move("/libs/foo", "/foo/bar");
        session.save();
        assertFalse(session.itemExists("/libs/foo/install"));

        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertRegistered("After moving /libs to /foo", res, false);

        contentHelper.delete("/foo");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        contentHelper.delete("/libs");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
    }

    public void testMoveLibsToApps() throws Exception {
        final String res = "/libs/foo/install/somefile.jar";
        final String appsRes = "/apps/foo/install/somefile.jar";
        assertRegistered("Before test", res, false);

        assertFalse("/libs must not exist when test starts", session.itemExists("/libs"));
        contentHelper.createFolder("/libs");
        contentHelper.createFolder("/libs/foo");
        contentHelper.createFolder("/libs/foo/install");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        assertFalse("/apps must not exist when test starts", session.itemExists("/apps"));
        contentHelper.createFolder("/apps");

        contentHelper.createOrUpdateFile(res);
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        assertRegistered("After creating libs and test file", res, true);

        session.move("/libs/foo", "/apps/foo");
        session.save();
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertRegistered("/apps resource must be registered", appsRes, true);
        assertRegistered("/libs resource must be gone", res, false);

        contentHelper.delete("/apps");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);

        contentHelper.delete("/libs");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
    }
}
