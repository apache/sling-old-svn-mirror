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

import java.util.Collection;

import org.apache.sling.commons.testing.jcr.EventHelper;

/** JcrInstall test utilities */
class MiscUtil {

    public static String SEARCH_PATHS [] = { "/libs/", "/apps/" };
    public static String RUN_MODES [] = { "dev", "staging" };

    static private void waitForCycles(JcrInstaller installer, long initialCycleCount, int expectedCycles, long timeoutMsec) throws Exception {
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        long cycles = 0;
        while(System.currentTimeMillis() < endTime) {
            cycles = installer.getCounterValue(JcrInstaller.RUN_LOOP_COUNTER) - initialCycleCount;
            if(cycles >= expectedCycles) {
                return;
            }
        }
        throw new Exception("did not get " + expectedCycles + " installer cycles in " + timeoutMsec + " msec, got " + cycles);
    }

    /** Get the WatchedFolders of supplied JcrInstaller */
    static Collection<WatchedFolder> getWatchedFolders(final JcrInstaller installer) {
        final InstallerConfig cfg = installer.getConfiguration();

        return cfg.cloneWatchedFolders();
    }

    /** Wait long enough for all changes in content to be processed by JcrInstaller */
    static void waitAfterContentChanges(EventHelper eventHelper, JcrInstaller installer) throws Exception {
        final long startCycles = installer.getCounterValue(JcrInstaller.RUN_LOOP_COUNTER);

        // First wait for all JCR events to be delivered
        eventHelper.waitForEvents(5000L);
        // RescanTimer causes a SCAN_DELAY_MSEC wait after JCR events are received
        // This assumes the RescanTimer is using its default delay, as per its javadoc
        Thread.sleep(RescanTimer.DEFAULT_SCAN_DELAY_MSEC * 4);
        // And wait for a few JcrInstaller run cycles
        MiscUtil.waitForCycles(installer, startCycles, 2, 10000L);
    }

    /** Wait until installer thread exits */
    static void waitForInstallerThread(JcrInstaller installer, long timeoutMsec) throws Exception {
        final long endTime = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < endTime) {
            if(installer.getCounterValue(JcrInstaller.RUN_LOOP_COUNTER) == -1) {
                return;
            }
        }
        throw new Exception("JcrInstaller thread did not signal its end (timeout=" + timeoutMsec + " msec)");
    }
}