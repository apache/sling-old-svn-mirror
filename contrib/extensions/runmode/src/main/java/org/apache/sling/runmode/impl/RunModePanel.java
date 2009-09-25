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

import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.runmode.RunMode;

/**
 * @scr.component metatype="no"
 * @scr.service
 */
public class RunModePanel implements ConfigurationPrinter {
    private static final String TITLE = "Run Modes";

    /** @scr.reference */
    private RunMode runMode;

    // ---------- ConfigurationPrinter

    public void printConfiguration(PrintWriter pw) {
        pw.print("Current Run Modes: ");

        String[] modes = runMode.getCurrentRunModes();
        if (modes == null || modes.length == 0) {
            pw.println("-");
        } else {
            pw.println(Arrays.asList(runMode.getCurrentRunModes()));
        }
    }

    public String getTitle() {
        return TITLE;
    }
}
