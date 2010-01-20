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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>SlingConfigurationPrinter</code> is an Apache Felix
 * Web Console plugin to display the currently configured log
 * files.
 */
public class SlingConfigurationPrinter implements ConfigurationPrinter {

    /** The registration. */
    private static ServiceRegistration registration;

    public static void registerPrinter(BundleContext ctx) {
        if (registration == null) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();

            SlingConfigurationPrinter printer = new SlingConfigurationPrinter();
            registration = ctx.registerService(ConfigurationPrinter.class.getName(),
                    printer, props);
        }
    }

    public static void unregisterPrinter() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#getTitle()
     */
    public String getTitle() {
        return "Log Files";
    }

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter printWriter) {
        final LogConfigManager logConfigManager = LogConfigManager.getInstance();
        Iterator<SlingLoggerWriter> writers = logConfigManager.getSlingLoggerWriters();
        while (writers.hasNext()) {
            final SlingLoggerWriter writer = writers.next();
            final File file = writer.getFile();
            if ( file != null ) {
                printWriter.print("Log file ");
                printWriter.println(getPath(writer));
                printWriter.println("--------------------------------------------------");
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    final char[] buffer = new char[512];
                    int len;
                    while ((len = fr.read(buffer)) != -1 ) {
                        printWriter.write(buffer, 0, len);
                    }
                } catch (IOException ignore) {
                    // we just ignore this
                    if ( fr != null ) {
                        try {
                            fr.close();
                            fr = null;
                        } catch (IOException ignoreCloseException) {}
                    }
                }
                printWriter.println();
            }
        }
    }

    private static String getPath(SlingLoggerWriter writer) {
        final String path = writer.getPath();
        return (path != null) ? path : "[stdout]";
    }
}
