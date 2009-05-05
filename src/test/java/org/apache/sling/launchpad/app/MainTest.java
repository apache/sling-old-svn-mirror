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
package org.apache.sling.launchpad.app;

import java.util.Map;

import junit.framework.TestCase;

public class MainTest extends TestCase {

    public void test_parseCommandLine_null_args() {
        String[] args = null;
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }

    public void test_parseCommandLine_empty_args() {
        String[] args = {};
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }
    
    public void test_parseCommandLine_single_dash() {
        String[] args = { "-" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }

    public void test_parseCommandLine_single_arg_no_par() {
        String[] args = { "-a" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have one entry", 1, commandline.size());
        assertEquals("single argument must be " + args[0].charAt(1), String.valueOf(args[0].charAt(1)), commandline.keySet().iterator().next());
        assertEquals("single argument value must be " + args[0].charAt(1), String.valueOf(args[0].charAt(1)), commandline.values().iterator().next());
    }
    
    public void test_parseCommandLine_single_arg_with_par() {
        String[] args = { "-a", "value" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have one entry", 1, commandline.size());
        assertEquals("single argument must be " + args[0].charAt(1), String.valueOf(args[0].charAt(1)), commandline.keySet().iterator().next());
        assertEquals("single argument value must be " + args[1], args[1], commandline.values().iterator().next());
    }

    public void test_parseCommandLine_two_args_no_par() {
        String[] args = { "-a", "-b" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2, commandline.size());
        assertEquals("argument a must a", "a", commandline.get("a"));
        assertEquals("argument b must b", "b", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_first_par() {
        String[] args = { "-a", "apar", "-b" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2, commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument b must b", "b", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_second_par() {
        String[] args = { "-a", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2, commandline.size());
        assertEquals("argument a must a", "a", commandline.get("a"));
        assertEquals("argument b must bpar", "bpar", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_all_par() {
        String[] args = { "-a", "apar", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2, commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument b must bpar", "bpar", commandline.get("b"));
    }

    public void test_parseCommandLine_three_args_with_dash() {
        String[] args = { "-a", "apar", "-", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have three entries", 3, commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument -b must -b", "-b", commandline.get("-b"));
        assertEquals("argument bpar must bpar", "bpar", commandline.get("bpar"));
    }

    public void test_parseCommandLine_single_arg_with_dash_par() {
        String[] args = { "-a", "-" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have three entries", 1, commandline.size());
        assertEquals("argument a must -", "-", commandline.get("a"));
    }
}
