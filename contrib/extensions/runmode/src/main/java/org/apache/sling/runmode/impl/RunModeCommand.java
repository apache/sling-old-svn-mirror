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
package org.apache.sling.runmode.impl;

import java.io.PrintStream;
import java.util.Arrays;

import org.apache.felix.shell.Command;
import org.apache.sling.runmode.RunMode;

/**
 * @scr.component metatype="no"
 * @scr.service
 */
public class RunModeCommand implements Command {

    private static final String CMD_NAME = "runmodes";

    /** @scr.reference */
    private RunMode runMode;

    public String getName() {
        return CMD_NAME;
    }

    public String getShortDescription() {
        return "lists current run modes";
    }

    public String getUsage() {
        return CMD_NAME;
    }

    public void execute(String command, PrintStream out, PrintStream err) {

        out.print("Current Run Modes: ");

        String[] modes = runMode.getCurrentRunModes();
        if (modes == null || modes.length == 0) {
            out.println("-");
        } else {
            out.println(Arrays.asList(runMode.getCurrentRunModes()));
        }
    }
}
