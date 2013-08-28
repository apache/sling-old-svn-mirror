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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

/**
 * The <code>SlingConfigurationPrinter</code> is an Apache Felix Web Console
 * plugin to display the currently configured log files.
 */
public class SlingConfigurationPrinter {
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
                            } catch (IOException ignoreCloseException) {
                            }
                        }
                    }
                    printWriter.println();
                }
            }
        }
    }

    /**
     * TODO Need to see how to implement this with LogBack as we cannot get
     * information about all rolled over policy
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
                    final File file = new File(((FileAppender) appender).getFile());
                    // TODO With LogBack there is no straightforward way to get
                    // information
                    // about rolled over files
                    // final File[] files =
                    // writer.getFileRotator().getRotatedFiles(writer.getFile());
                    final File[] files = new File[] {
                        file
                    };
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

}
