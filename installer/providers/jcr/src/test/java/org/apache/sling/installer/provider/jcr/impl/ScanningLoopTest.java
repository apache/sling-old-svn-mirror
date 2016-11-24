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

import javax.jcr.Node;

import org.junit.Test;

/** Verify that JcrInstaller scans folders only when needed */
public class ScanningLoopTest extends JcrInstallTestBase {

    private void assertCounter(int index, long value) {
        
        String label;
        switch (index ) {
            case JcrInstaller.RUN_LOOP_COUNTER:
                label = "RUN_LOOP_COUNTER";
                break;
            case JcrInstaller.SCAN_FOLDERS_COUNTER:
                label = "SCAN_FOLDERS_COUNTER";
                break;
                
            case JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER:
                label = "UPDATE_FOLDERS_LIST_COUNTER";
                break;
            
            default:
                label = "Unknown (" + index +")";
                break;
        }
        
        assertEquals("Counter " + label, value, installer.getCounterValue(index));
    }

    private void assertIdle() throws Exception {
        final long sf = installer.getCounterValue(JcrInstaller.SCAN_FOLDERS_COUNTER);
        final long uc = installer.getCounterValue(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertCounter(JcrInstaller.SCAN_FOLDERS_COUNTER, sf);
        assertCounter(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER, uc);
    }

    private void assertEvents(String info, long oldCount, int counterIndex) {
        final long newCount = installer.getCounterValue(counterIndex);
        assertTrue(info + " (old=" + oldCount + ", new=" + newCount + ")", newCount > oldCount);
    }

    @Test
    public void testIdleState() throws Exception {
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 4);
        assertIdle();
    }

    @Test
    public void testDefaultScanPauseFalse() throws Exception{
        assertFalse(installer.scanningIsPaused(installer.getConfiguration(), installer.getSession()));
    }

    @Test
    public void testPauseScan() throws Exception{
        assertFalse(installer.scanningIsPaused(installer.getConfiguration(), installer.getSession()));

        Node n = contentHelper.createFolder(JcrInstaller.PAUSE_SCAN_NODE_PATH);
        Node testNode = n.addNode("foo.example.pause");
        session.save();

        eventHelper.waitForEvents(TIMEOUT);

        assertTrue(installer.scanningIsPaused(installer.getConfiguration(), installer.getSession()));
        final long sf = installer.getCounterValue(JcrInstaller.SCAN_FOLDERS_COUNTER);
        final long uc = installer.getCounterValue(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER);

        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);

        //Counters should not have changed as no scanning being performed
        assertEquals(sf, installer.getCounterValue(JcrInstaller.SCAN_FOLDERS_COUNTER));

        //Now lets resume again
        testNode.remove();
        session.save();

        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);

        //Now counters should have changed
        assertIdle();
    }

    @Test
    public void testAddBundle() throws Exception {
        contentHelper.createOrUpdateFile(contentHelper.FAKE_RESOURCES[0]);
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertIdle();
    }

    @Test
    public void testAddContentOutside() throws Exception {
        final long sf = installer.getCounterValue(JcrInstaller.SCAN_FOLDERS_COUNTER);
        final long uc = installer.getCounterValue(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER);

        contentHelper.createOrUpdateFile("/" + System.currentTimeMillis());
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);

        // Adding a file outside /libs or /apps must not "wake up" the scan loop
        assertCounter(JcrInstaller.SCAN_FOLDERS_COUNTER, sf);
        assertCounter(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER, uc);
    }

    @Test
    public void testDeleteFile() throws Exception {
        contentHelper.setupContent();
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertIdle();

        final long sf = installer.getCounterValue(JcrInstaller.SCAN_FOLDERS_COUNTER);
        contentHelper.delete(contentHelper.FAKE_RESOURCES[0]);
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertEvents("Expected at least one folder scan event",
                sf,  JcrInstaller.SCAN_FOLDERS_COUNTER);

        assertIdle();
    }

    @Test
    public void testDeleteLibsFolder() throws Exception {
        contentHelper.setupContent();
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertIdle();

        final long uc = installer.getCounterValue(JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER);
        contentHelper.delete("/libs");
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 2);
        assertEvents("Expected at least one folders list update event",
                uc,  JcrInstaller.UPDATE_FOLDERS_LIST_COUNTER);

        assertIdle();
    }
}