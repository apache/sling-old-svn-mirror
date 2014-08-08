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
package org.apache.sling.crankstart.core.commands;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.junit.Test;

/** Verify that factory configs are handled properly
 *  when starting the OSGi framework multiple times
 *  with Crankstart.
 */
public class StartFrameworkTest {

    private static final Dictionary<String, Object> EXIT_OPT= new Hashtable<String, Object>();
    
    {
        EXIT_OPT.put(StartFramework.EXIT_IF_NOT_FIRST, "false");
        EXIT_OPT.put("something else", "should not matter");
    }
    
    private static void assertStopProcessing(CrankstartCommandLine cmd, int nBundles, boolean expected) {
        final boolean actual = new StartFramework().stopProcessing(cmd, nBundles); 
        assertEquals("Expecting stop processing attribute to match", expected, actual);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testZeroStartupNoOption() throws Exception {
        assertStopProcessing(new CrankstartCommandLine(StartFramework.I_START_FRAMEWORK, "", null), 0,  false);
    }
    
    @Test
    public void testFirstStartupNoOption() throws Exception {
        assertStopProcessing(new CrankstartCommandLine(StartFramework.I_START_FRAMEWORK, "", null), 1,  false);
    }
    
    @Test
    public void testFirstStartupExitOption() throws Exception {
        assertStopProcessing(new CrankstartCommandLine(StartFramework.I_START_FRAMEWORK, "", EXIT_OPT), 1,  false);
    }
    
    @Test
    public void testSecondStartupNoOption() throws Exception {
        assertStopProcessing(new CrankstartCommandLine(StartFramework.I_START_FRAMEWORK, "", null), 12,  true);
    }
    
    @Test
    public void testSecondStartupExitOption() throws Exception {
        assertStopProcessing(new CrankstartCommandLine(StartFramework.I_START_FRAMEWORK, "", EXIT_OPT), 42,  false);
    }
    
}
