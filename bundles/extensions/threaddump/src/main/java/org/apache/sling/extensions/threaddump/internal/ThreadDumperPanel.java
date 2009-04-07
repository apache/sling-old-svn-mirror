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
package org.apache.sling.extensions.threaddump.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.ConfigurationPrinter;

public class ThreadDumperPanel extends AbstractWebConsolePlugin implements
        Servlet, ConfigurationPrinter {

    private static final String LABEL = "threads";

    private static final String TITLE = "Threads";

    private BaseThreadDumper baseThreadDumper = new BaseThreadDumper();

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

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content container'>" + getTitle() + "</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>" );
        pw.println( "<pre>" );

        pw.println( "*** Date: "
            + SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG, SimpleDateFormat.LONG, Locale.US ).format(
                new Date() ) );
        pw.println();

        baseThreadDumper.printThreads(pw, true);

        pw.println( "</pre>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );
        pw.println( "</table>" );
    }

    // ---------- ConfigurationPrinter

    public void printConfiguration(PrintWriter pw) {
        pw.println("*** Threads Dumps:");
        baseThreadDumper.printThreads(pw, true);
    }
}
