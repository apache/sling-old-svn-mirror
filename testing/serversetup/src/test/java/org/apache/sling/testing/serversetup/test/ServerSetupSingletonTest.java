/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.serversetup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.apache.sling.testing.serversetup.ServerSetup;
import org.apache.sling.testing.serversetup.ServerSetupSingleton;
import org.junit.Before;
import org.junit.Test;

/** Test the ServerSetupSingleton */
public class ServerSetupSingletonTest {
    private ServerSetup serverSetup;
    private Properties props;
    
    @Before
    public void setup() throws Exception {
        props = new Properties();
        props.setProperty(ServerSetupSingleton.CLASS_NAME_PROP, TestServerSetup.class.getName());
        props.setProperty(ServerSetup.PHASES_TO_RUN_PROP, "one, \t\n two, three, four, five  \t");
        serverSetup = ServerSetupSingleton.instance(props);
        serverSetup.setConfig(props);
        TestSetupPhase.clearExecutionLog();
        TestSetupPhase.failingPhases = "";
    }

    @Test
    public void testProperties() {
        assertTrue(serverSetup.getConfig() == props);
    }
    
    @Test
    public void testContext() {
        final String key = "foo";
        assertNull(serverSetup.getContext().get(key));
        serverSetup.getContext().put(key, this);
        assertEquals(serverSetup.getContext().get(key), this);
    }
    
    @Test
    public void testStartup() throws Exception {
        serverSetup.setupTestServer();
        assertEquals("Expecting all startup phases to have run",
                "one,two,three", TestSetupPhase.executionLog.toString());
        
        serverSetup.setupTestServer();
        assertEquals("Expecting second setup call to have no effect",
                "one,two,three", TestSetupPhase.executionLog.toString());
    }
    
    @Test
    public void testShutdown() throws Exception {
        serverSetup.shutdown();
        assertEquals("Expecting all shutdown phases to have run",
                "four,five", TestSetupPhase.executionLog.toString());
        
        serverSetup.shutdown();
        assertEquals("Expecting second shutdown call to be ignored",
                "four,five", TestSetupPhase.executionLog.toString());
    }
    
    @Test
    public void testStartupAndShutdown() throws Exception {
        serverSetup.setupTestServer();
        assertEquals("Expecting all startup phases to have run",
                "one,two,three", TestSetupPhase.executionLog.toString());
        
        serverSetup.shutdown();
        assertEquals("Expecting all phases to have run",
                "one,two,three,four,five", TestSetupPhase.executionLog.toString());
    }
    
    @Test
    public void testStartupSomeOnly() throws Exception {
        props.setProperty(ServerSetup.PHASES_TO_RUN_PROP, "one, three, five");
        serverSetup.setConfig(props);
        
        serverSetup.setupTestServer();
        assertEquals("Expecting only two startup phases to have run",
                "one,three", TestSetupPhase.executionLog.toString());
        
        serverSetup.setupTestServer();
        assertEquals("Expecting second setup call to have no effect",
                "one,three", TestSetupPhase.executionLog.toString());
    }
    
    @Test
    public void testShutdownSomeOnly() throws Exception {
        props.setProperty(ServerSetup.PHASES_TO_RUN_PROP, "four");
        serverSetup.setConfig(props);
        
        serverSetup.shutdown();
        assertEquals("Expecting only one shutdown phase to have run",
                "four", TestSetupPhase.executionLog.toString());
        
        serverSetup.shutdown();
        assertEquals("Expecting second setup call to have no effect",
                "four", TestSetupPhase.executionLog.toString());
    }
    
    @Test
    public void testFailingStartup() {
        TestSetupPhase.failingPhases = "two, five";
        
        // setupTestServer will fail every time it's called
        // after a failure, as that means the server is unusable
        for(int i=0; i < 3; i++) {
            try {
                serverSetup.setupTestServer();
                fail("startup should have failed");
            } catch(Exception ignored) {
            }
            
            assertEquals("Expecting only one startup phase to have run",
                    "one", TestSetupPhase.executionLog.toString());
        }
    }
    
    @Test
    public void testFailingShutdown() throws Exception {
        TestSetupPhase.failingPhases = "two, five";
        
        try {
            serverSetup.shutdown();
            fail("shutdown should have failed");
        } catch(Exception ignored) {
        }
        
        assertEquals("Expecting only one startup phase to have run",
                "four", TestSetupPhase.executionLog.toString());
        
        // Calling shutdown again does not throw an Exception again,
        // it's not really useful at shutdown.
        serverSetup.shutdown();
        
        assertEquals("Still expecting only one startup phase to have run",
                "four", TestSetupPhase.executionLog.toString());
    }
    
    @Test(expected=ServerSetup.SetupException.class)
    public void testDuplicateStartupPhase() throws ServerSetup.SetupException {
        serverSetup.addSetupPhase(new TestSetupPhase("two", true));
    }
    
    @Test(expected=ServerSetup.SetupException.class)
    public void testDuplicateShutdownPhase() throws ServerSetup.SetupException {
        serverSetup.addSetupPhase(new TestSetupPhase("two", false));
    }
    
    @Test
    public void testAddPhasesLater() throws Exception {
        props.setProperty(ServerSetup.PHASES_TO_RUN_PROP, "one, B, five, A, two");
        serverSetup.setConfig(props);
        serverSetup.addSetupPhase(new TestSetupPhase("A", true));
        serverSetup.addSetupPhase(new TestSetupPhase("B", false));
        serverSetup.setupTestServer();
        
        assertEquals("Expecting all startup phases to have run",
                "one,A,two", TestSetupPhase.executionLog.toString());
        
        serverSetup.shutdown();
        assertEquals("Expecting all phases to have run",
                "one,A,two,B,five", TestSetupPhase.executionLog.toString());
    }
}