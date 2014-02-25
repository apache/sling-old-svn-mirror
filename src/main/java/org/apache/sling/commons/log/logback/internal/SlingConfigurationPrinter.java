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
package org.apache.sling.commons.log.logback.internal;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.CachingDateFormatter;

/**
 * The <code>SlingConfigurationPrinter</code> is an Apache Felix Web Console
 * plugin to display the currently configured log files.
 */
public class SlingConfigurationPrinter {
    private static final CachingDateFormatter SDF = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss");
    private final LogbackManager logbackManager;

    public SlingConfigurationPrinter(LogbackManager logbackManager) {
        this.logbackManager = logbackManager;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    @SuppressWarnings("UnusedDeclaration")
    public void printConfiguration(PrintWriter printWriter) {
        LogbackManager.LoggerStateContext ctx = logbackManager.determineLoggerState();
        dumpLogbackStatus(logbackManager, printWriter);
        for (Appender<ILoggingEvent> appender : ctx.getAllAppenders()) {
            if (appender instanceof FileAppender) {
                final File file = new File(((FileAppender) appender).getFile());
                if (file.exists()) {
                    printWriter.print("Log file ");
                    printWriter.println(file.getAbsolutePath());
                    printWriter.println("--------------------------------------------------");
                    FileReader fr = null;
                    try {
                        fr = new FileReader(file);
                        final char[] buffer = new char[512];
                        int len;
                        while ((len = fr.read(buffer)) != -1) {
                            printWriter.write(buffer, 0, len);
                        }
                    } catch (IOException ignore) {
                        // we just ignore this
                    } finally {
                        if (fr != null) {
                            try {
                                fr.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                    printWriter.println();
                }
            }
        }
    }

    /**
     * Attempts to determine all log files created even via rotation.
     * if some complex rotation logic is used where rotated file get different names
     * or get created in different directory then those files would not be
     * included
     * 
     * @see org.apache.felix.webconsole.AttachmentProvider#getAttachments(String)
     */
    @SuppressWarnings("UnusedDeclaration")
    public URL[] getAttachments(String mode) {
        // we only provide urls for mode zip
        if ("zip".equals(mode)) {
            final List<URL> urls = new ArrayList<URL>();
            LogbackManager.LoggerStateContext ctx = logbackManager.determineLoggerState();
            for (Appender<ILoggingEvent> appender : ctx.getAllAppenders()) {
                if (appender instanceof FileAppender) {
                    final File[] files = getRotatedFiles((FileAppender) appender);
                    for (File f : files) {
                        try {
                            urls.add(f.toURI().toURL());
                        } catch (MalformedURLException mue) {
                            // we just ignore this file then
                        }
                    }
                }
            }
            if (urls.size() > 0) {
                return urls.toArray(new URL[urls.size()]);
            }
        }
        return null;
    }

    private static File[] getRotatedFiles(FileAppender app) {
        final File file = new File(app.getFile());

        //If RollingFileAppender then make an attempt to list files
        //This might not work in all cases if complex rolling patterns
        //are used in Logback
        if (app instanceof RollingFileAppender) {
            final File dir = file.getParentFile();
            final String baseName = file.getName();
            return dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(baseName);
                }
            });
        }

        //Not a RollingFileAppender then just return the actual file
        return new File[]{file};
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private static void dumpLogbackStatus(LogbackManager logbackManager, PrintWriter pw) {
        List<Status> statusList = logbackManager.getStatusManager().getCopyOfStatusList();
        pw.println("Logback Status");
        pw.println("--------------------------------------------------");
        for (Status s : statusList) {
            pw.printf("%s *%s* %s - %s %n",
                    SDF.format(s.getDate()),
                    statusLevelAsString(s),
                    SlingLogPanel.abbreviatedOrigin(s),
                    s.getMessage());
            if (s.getThrowable() != null) {
                s.getThrowable().printStackTrace(pw);
            }
        }

        pw.println();
    }

    private static String statusLevelAsString(Status s) {
        switch (s.getEffectiveLevel()) {
            case Status.INFO:
                return "INFO";
            case Status.WARN:
                return "WARN";
            case Status.ERROR:
                return "ERROR";
        }
        return null;
    }

}
