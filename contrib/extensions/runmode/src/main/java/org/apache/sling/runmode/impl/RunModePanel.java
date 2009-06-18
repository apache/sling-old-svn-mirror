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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.sling.runmode.RunMode;
import org.osgi.service.component.ComponentContext;

/**
 * @scr.component metatype="no"
 * @scr.service
 * @scr.property name="felix.webconsole.label" valueRef="LABEL"
 */
public class RunModePanel extends AbstractWebConsolePlugin implements Servlet,
        ConfigurationPrinter {

    private static final String LABEL = "runmodes";

    private static final String TITLE = "Run Modes";

    /** @scr.reference */
    private RunMode runMode;

    // ---------- AbstractWebConsolePlugin API

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    protected void renderContent(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();

        pw.println("<pre>");
        pw.println("</pre>");

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        pw.println("<tr class='content'>");
        pw.println("<th class='content container' colspan='2'>" + getTitle()
            + "</th>");
        pw.println("</tr>");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Current Run Modes</td>");
        pw.println("<td class='content'>");

        String[] modes = runMode.getCurrentRunModes();
        if (modes == null || modes.length == 0) {
            pw.println("-");
        } else {
            for (String mode : modes) {
                pw.print(mode);
                pw.println("<br>");
            }
        }

        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
    }

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

    // ---------- SCR integration

    protected void activate(ComponentContext context) {
        activate(context.getBundleContext());
    }

    protected void deactivate(ComponentContext context) {
        deactivate();
    }
}
