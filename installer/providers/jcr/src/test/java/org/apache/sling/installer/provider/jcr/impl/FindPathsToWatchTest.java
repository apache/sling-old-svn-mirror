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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

/** Verify that the JcrInstaller finds all folders that must
 *  be watched, including those created after it starts
 */
public class FindPathsToWatchTest extends JcrInstallTestBase {
    
    private boolean isWatched(String path, Collection<WatchedFolder> wfList) {
        for(WatchedFolder wf : wfList ) {
            if(wf.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testInitialFind() throws Exception {
        final Collection<WatchedFolder> wf = MiscUtil.getWatchedFolders(installer);
        assertEquals("activate() must find all watched folders", contentHelper.WATCHED_FOLDERS.length, wf.size());

        for(String folder : contentHelper.WATCHED_FOLDERS) {
            assertTrue("Folder " + folder + " must be watched (watched=" + wf + ")",
                    isWatched(folder, wf));
        }
    }

    @Test
    public void testNewWatchedFolderDetection() throws Exception {
        final String newPaths [] = {
                "/libs/tnwf/install",
                "/libs/tnwf/install.dev",
                "/apps/tnwf/install",
                "/apps/tnwf/install.staging",
                "/apps/tnwf/install.staging.dev",
                "/NOT/foo/nothing"
        };
        for(String newPath : newPaths) {
            assertFalse(newPath + " must not be watched before test", isWatched(newPath,
                    MiscUtil.getWatchedFolders(installer)));

            // Create folder, wait for observation event and check that
            // it is detected
            contentHelper.createFolder(newPath);
            MiscUtil.waitAfterContentChanges(eventHelper, installer);

            if(newPath.contains("NOT")) {
                assertFalse(newPath + " must not be watched after test", isWatched(newPath,
                        MiscUtil.getWatchedFolders(installer)));
            } else {
                assertTrue(newPath + " must be watched after calling addNewWatchedFolders()", isWatched(newPath,
                        MiscUtil.getWatchedFolders(installer)));
            }
        }

        // Delete a single folder and verify that it's gone
        final String folder = newPaths[1];
        final int nBefore = MiscUtil.getWatchedFolders(installer).size();
        contentHelper.delete(folder);
        eventHelper.waitForEvents(5000L);
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertFalse(folder + " must not be watched anymore after deleting", isWatched(folder,
                MiscUtil.getWatchedFolders(installer)));
        assertEquals("Expecting only one WatchedFolder to be deleted", nBefore - 1,
               MiscUtil.getWatchedFolders(installer).size());
    }

    @Test
    public void testDeleteWatchedFolders() throws Exception {
        assertEquals("activate() must find all watched folders", contentHelper.WATCHED_FOLDERS.length, MiscUtil.getWatchedFolders(installer).size());
        contentHelper.cleanupContent();
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals("After deleting content, no more folders must be watched", 0, MiscUtil.getWatchedFolders(installer).size());
    }
}