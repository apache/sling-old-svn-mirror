/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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
