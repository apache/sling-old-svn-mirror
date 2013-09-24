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
package org.apache.sling.reqanalyzer.impl;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.reqanalyzer.impl.gui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class RequestAnalyzerWebConsole extends HttpServlet {

    private static final String WINDOW_MARKER = ".showWindow";

    private static final String RAW_FILE_MARKER = ".txt";

    private static final String ZIP_FILE_MARKER = ".txt.zip";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final File logFile;

    private MainFrame frame;

    RequestAnalyzerWebConsole(final File logFile) {
        this.logFile = logFile;
    }

    void dispose() {
        if (this.frame != null) {
            MainFrame frame = this.frame;
            this.frame = null;
            AWTEvent e = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().endsWith(RAW_FILE_MARKER) || req.getRequestURI().endsWith(ZIP_FILE_MARKER)) {

            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(this.logFile);
                output = resp.getOutputStream();

                if (req.getRequestURI().endsWith(ZIP_FILE_MARKER)) {
                    ZipOutputStream zip = new ZipOutputStream(output);
                    zip.setLevel(Deflater.BEST_SPEED);

                    ZipEntry entry = new ZipEntry(this.logFile.getName());
                    entry.setTime(this.logFile.lastModified());
                    entry.setMethod(ZipEntry.DEFLATED);

                    zip.putNextEntry(entry);

                    output = zip;
                    resp.setContentType("application/zip");
                } else {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("UTF-8");
                    resp.setHeader("Content-Length", String.valueOf(this.logFile.length())); // might be bigger than
                }
                resp.setDateHeader("Last-Modified", this.logFile.lastModified());

                IOUtils.copy(input, output);
            } catch (IOException ioe) {
                throw new ServletException("Cannot create copy of log file", ioe);
            } finally {
                IOUtils.closeQuietly(input);

                if (output instanceof ZipOutputStream) {
                    ((ZipOutputStream) output).closeEntry();
                    ((ZipOutputStream) output).finish();
                }
            }

            resp.flushBuffer();

        } else if (req.getRequestURI().endsWith(WINDOW_MARKER)) {

            if (canOpenSwingGui(req)) {
                showWindow();
            }

            String target = req.getRequestURI();
            target = target.substring(0, target.length() - WINDOW_MARKER.length());
            resp.sendRedirect(target);
            resp.flushBuffer();

        } else {

            super.service(req, resp);

        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter pw = resp.getWriter();

        final String fileSize = this.formatByteSize(this.logFile.length());
        pw.printf("<p class='statline ui-state-highlight'>Request Log Size %s</p>%n", fileSize);

        pw.println("<table class='nicetable ui-widget'>");
        pw.println("  <thead>");
        pw.println("    <tr>");
        pw.println("        <th class='ui-widget-header'>Name</th>");
        pw.println("        <th class='ui-widget-header'>Last Update</th>");
        pw.println("        <th class='ui-widget-header'>Size</th>");
        pw.println("        <th class='ui-widget-header'>Action</th>");
        pw.println("    </tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        pw.println("<tr>");

        pw.printf("<td><a href='${pluginRoot}%s'>%s</a> (<a href='${pluginRoot}%s'>zipped</a>)</td>%n", RAW_FILE_MARKER, this.logFile.getName(), ZIP_FILE_MARKER);
        pw.printf("<td>%s</td><td>%s</td>", new Date(this.logFile.lastModified()), fileSize);

        pw.println("<td><ul class='icons ui-widget'>");
        if (canOpenSwingGui(req)) {
            pw.printf(
                    "<li title='Analyze Now' class='dynhover ui-state-default ui-corner-all'><a href='${pluginRoot}%s'><span class='ui-icon ui-icon-wrench'></span></a></li>%n",
                    WINDOW_MARKER);
            // pw.printf(" (<a href='${pluginRoot}%s'>Analyze Now</a>)", WINDOW_MARKER);
        }
        // pw.println("<li title='Remove File' class='dynhover ui-state-default ui-corner-all'><span class='ui-icon ui-icon-trash'></span></li>");
        pw.println("</ul></td>");

        pw.println("</tr></tbody>");
        pw.println("</table>");

    }

    private synchronized void showWindow() throws ServletException, IOException {
        if (this.frame == null) {
            final File toAnalyze = this.getLogFileCopy();

            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            MainFrame frame = new MainFrame(toAnalyze, Integer.MAX_VALUE, screenSize);

            // exit the application if the main frame is closed
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // remove the tmp file we showed
                    if (toAnalyze.exists()) {
                        toAnalyze.delete();
                    }
                    // clear the window reference
                    RequestAnalyzerWebConsole.this.frame = null;
                }
            });

            this.frame = frame;
        }

        // make sure the window is visible, try to bring to the front
        this.frame.setVisible(true);
        this.frame.toFront();
    }

    private boolean canOpenSwingGui(final ServletRequest request) {
        try {
            return !GraphicsEnvironment.isHeadless()
                    && InetAddress.getByName(request.getLocalAddr()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            // unexpected, but still fall back to fail-safe false
            return false;
        }
    }

    private final File getLogFileCopy() throws ServletException {
        File result = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            result = File.createTempFile(getServletName(), ".tmp");
            input = new FileInputStream(this.logFile);
            output = new FileOutputStream(result);
            IOUtils.copy(input, output);
            return result;
        } catch (IOException ioe) {
            throw new ServletException("Cannot create copy of log file", ioe);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    private String formatByteSize(final long value) {
        final String suffix;
        final String suffixedValue;

        if (value >= 0) {
            final BigDecimal KB = new BigDecimal(1000L);
            final BigDecimal MB = new BigDecimal(1000L * 1000);
            final BigDecimal GB = new BigDecimal(1000L * 1000 * 1000);

            BigDecimal bd = new BigDecimal(value);
            if (bd.compareTo(GB) > 0) {
                bd = bd.divide(GB);
                suffix = "GB";
            } else if (bd.compareTo(MB) > 0) {
                bd = bd.divide(MB);
                suffix = "MB";
            } else if (bd.compareTo(KB) > 0) {
                bd = bd.divide(KB);
                suffix = "kB";
            } else {
                suffix = "B";
            }
            suffixedValue = bd.setScale(2, RoundingMode.UP).toString();
        } else {
            suffixedValue = "n/a";
            suffix = "";
        }

        return suffixedValue + suffix;
    }

}
