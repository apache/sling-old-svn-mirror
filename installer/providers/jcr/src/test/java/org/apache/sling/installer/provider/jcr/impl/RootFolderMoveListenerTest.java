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
import static org.junit.Assert.fail;

import java.util.UUID;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the RootFolderMoveListener
 */
public class RootFolderMoveListenerTest {
    private RootFolderMoveListener rmlt;
    private static final long TEST_SCAN_DELAY_MSEC = 5; 
    private final RescanTimer timer = new RescanTimer(TEST_SCAN_DELAY_MSEC);
    private Session contentModificationSession;
    private Session moveListenerSession;

    public static final String ROOT_1 = "/" + uniqueName();
    public static final String ROOT_2 = "/" + uniqueName();
    
    public static final String [] ROOTS = { ROOT_1, ROOT_2 };

    private static String uniqueName() {
        return RootFolderMoveListener.class.getSimpleName() + "_" + UUID.randomUUID().toString();
    }
    
    @Before
    public void setup() throws Exception {
        // Need 2 sessions for this test, as the RootFolderMoveListener is registered with noLocal property set to true.
        contentModificationSession = RepositoryProvider.instance().getRepository().loginAdministrative(null);
        moveListenerSession = RepositoryProvider.instance().getRepository().loginAdministrative(null);
        rmlt = new RootFolderMoveListener(moveListenerSession, ROOTS, timer);
    }
    
    @After
    public void cleanup() throws Exception {
        rmlt.cleanup(moveListenerSession);
        contentModificationSession.logout();
        moveListenerSession.logout();
    }
    
    private void waitForScanDelay() {
        try {
            Thread.sleep(2 * TEST_SCAN_DELAY_MSEC);
        } catch(InterruptedException iex) {
            fail("Sleep failed: " + iex);
        }
    }
    
    @Test
    public void testToString() {
        final String str = rmlt.toString();
        assertTrue("Expecting " + str + " to contain our list of paths", str.contains(ROOT_1 + ", " + ROOT_2));
    }
    
    @Test
    public void testTimer() {
        assertFalse(timer.expired());
        timer.scheduleScan();
        waitForScanDelay();
        assertTrue(timer.expired());
    }
    
    @Test
    public void testMove() throws Exception {
        contentModificationSession.getRootNode().addNode(ROOT_1.substring(1));
        contentModificationSession.getNode(ROOT_1).addNode("one");
        contentModificationSession.save();
        
        waitForScanDelay();
        assertFalse("Expecting no scheduled scan before move", timer.expired());
        
        contentModificationSession.move(ROOT_1 + "/one", ROOT_1 + "/two");
        contentModificationSession.save();
        waitForScanDelay();
        assertTrue("Expecting scan to be triggered after move", timer.expired());
    }
}