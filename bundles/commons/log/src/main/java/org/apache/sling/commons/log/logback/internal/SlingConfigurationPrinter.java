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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.CachingDateFormatter;

/**
 * The <code>SlingConfigurationPrinter</code> is an Apache Felix Web Console
 * plugin to display the currently configured log files.
 */
@SuppressWarnings("JavadocReference")
public class SlingConfigurationPrinter {
    private static final CachingDateFormatter SDF = new CachingDateFormatter("yyyy-MM-dd HH:mm:ss");
    private static final String MODE_ZIP = "zip";
    private final LogbackManager logbackManager;

    public SlingConfigurationPrinter(LogbackManager logbackManager) {
        this.logbackManager = logbackManager;
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    @SuppressWarnings("UnusedDeclaration")
    public void printConfiguration(PrintWriter printWriter, String mode) {
        LogbackManager.LoggerStateContext ctx = logbackManager.determineLoggerState();

        int numOfLines = getNumOfLines();
        Tailer tailer = new Tailer(printWriter, numOfLines);

        dumpLogFileSummary(printWriter, ctx.getAllAppenders());

        if (!MODE_ZIP.equals(mode)) {
            for (Appender<ILoggingEvent> appender : ctx.getAllAppenders()) {
                if (appender instanceof FileAppender) {
                    final File file = new File(((FileAppender) appender).getFile());
                    if (file.exists()) {
                        printWriter.print("Log file ");
                        printWriter.println(file.getAbsolutePath());
                        printWriter.println("--------------------------------------------------");
                        if (numOfLines < 0) {
                            includeWholeFile(printWriter, file);
                        } else {
                            try {
                                tailer.tail(file);
                            } catch (IOException e) {
                                logbackManager.getLogConfigManager().internalFailure("Error occurred " +
                                        "while processing log file " + file, e);
                            }
                        }
                        printWriter.println();
                    }
                }
            }
        }

        dumpLogbackStatus(logbackManager, printWriter);
    }

    static void includeWholeFile(PrintWriter printWriter, File file) {
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
    }

    private void dumpLogFileSummary(PrintWriter pw, Collection<Appender<ILoggingEvent>> appenders) {
        pw.println("Summary");
        pw.println("=======");
        pw.println();
        int counter = 0;
        final String rootDir = logbackManager.getRootDir();
        for (Appender<ILoggingEvent> appender : appenders) {
            if (appender instanceof FileAppender) {
                File file = new File(((FileAppender) appender).getFile());
                final File dir = file.getParentFile();
                final String baseName = file.getName();
                String absolutePath = dir.getAbsolutePath();
                String displayName = ((FileAppender) appender).getFile();
                if (absolutePath.startsWith(rootDir)) {
                    displayName = baseName;
                }
                pw.printf("%d. %s %n", ++counter, displayName);
                final File[] files = getRotatedFiles((FileAppender) appender, -1);
                for (File f : files) {
                    pw.printf("  - %s, %s, %s %n", f.getName(), humanReadableByteCount(f.length()), getModifiedDate(f));
                }
            }
        }
        pw.println();
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
        if (MODE_ZIP.equals(mode)) {
            final List<URL> urls = new ArrayList<URL>();
            LogbackManager.LoggerStateContext ctx = logbackManager.determineLoggerState();
            for (Appender<ILoggingEvent> appender : ctx.getAllAppenders()) {
                if (appender instanceof FileAppender) {
                    final File[] files = getRotatedFiles((FileAppender) appender, getMaxOldFileCount());
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

    /**
     * @param app appender instance
     * @param maxOldFileCount -1 if all files need to be included. Otherwise max
     *                        old files to include
     * @return sorted array of files generated by passed appender
     */
    private File[] getRotatedFiles(FileAppender app, int maxOldFileCount) {
        final File file = new File(app.getFile());

        //If RollingFileAppender then make an attempt to list files
        //This might not work in all cases if complex rolling patterns
        //are used in Logback
        if (app instanceof RollingFileAppender) {
            final File dir = file.getParentFile();
            final String baseName = file.getName();
            File[] result = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(baseName);
                }
            });

            //Sort the files in reverse
            Arrays.sort(result, Collections.reverseOrder(new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    long o1t = o1.lastModified();
                    long o2t = o2.lastModified();
                    return o1t < o2t ? -1 : (o1t == o2t ? 0 : 1);
                }
            }));

            if (maxOldFileCount > 0) {
                int maxCount = Math.min(getMaxOldFileCount(), result.length);
                if (maxCount < result.length) {
                    File[] resultCopy = new File[maxCount];
                    System.arraycopy(result, 0, resultCopy, 0, maxCount);
                    return resultCopy;
                }
            }
            return result;
        }

        //Not a RollingFileAppender then just return the actual file
        return new File[]{file};
    }

    private int getNumOfLines(){
        return logbackManager.getLogConfigManager().getNumOfLines();
    }

    private int getMaxOldFileCount(){
        return logbackManager.getLogConfigManager().getMaxOldFileCount();
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
                    abbreviatedOrigin(s),
                    s.getMessage());
            if (s.getThrowable() != null) {
                s.getThrowable().printStackTrace(pw);
            }
        }

        pw.println();
    }

    static String abbreviatedOrigin(Status s) {
        Object o = s.getOrigin();
        if (o == null) {
            return null;
        }
        String fqClassName = o.getClass().getName();
        int lastIndex = fqClassName.lastIndexOf(CoreConstants.DOT);
        if (lastIndex != -1) {
            return fqClassName.substring(lastIndex + 1, fqClassName.length());
        } else {
            return fqClassName;
        }
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

    /**
     * Returns a human-readable version of the file size, where the input represents
     * a specific number of bytes. Based on http://stackoverflow.com/a/3758880/1035417
     */
    private static String humanReadableByteCount(long bytes) {
        if (bytes < 0) {
            return "0";
        }
        int unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "kMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static String getModifiedDate(File f){
        long modified = f.lastModified();
        if (modified == 0){
            return "UNKNOWN";
        }
        return SDF.format(modified);
    }
}
