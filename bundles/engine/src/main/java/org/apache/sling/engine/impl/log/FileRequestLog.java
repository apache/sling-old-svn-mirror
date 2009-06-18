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
package org.apache.sling.engine.impl.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.engine.RequestLog;

/**
 * The <code>FileRequestLog</code> class is an implementation of the
 * {@link RequestLog} interface writing the log messages to an plain file. This
 * class supports sharing the files for different log formatters, in that an
 * internal map of log files is kept and access to the <code>PrintWriter</code>s
 * to write to the file is synchronized.
 * <p>
 * This class has a defined lifecycle to ensure correct operation: Before using
 * the class to instantiate it, the {@link #init(String)} method should be
 * called mainly to set the root directory for relative log file paths. It is
 * not recommended to call the {@link #init(String)} method multiple times. When
 * the class is not used any more the {@link #dispose()} method should be called
 * to clean up, namely to close all open files. This lifecycle behaviour is
 * forced by the {@link RequestLogger} component, which calls the
 * {@link #init(String)} method on filter component activation and calls the
 * {@link #dispose()} metod on filter component deactivation.
 * <p>
 * Note: Currently, each log file is kept open from the moment the log file is
 * first moment until the {@link #dispose()} method is called. Future
 * development should probably focus on the following issues: (1) Implement an
 * open/write/close cycle when logging a message, (2) close log files when the
 * last user has closed the log, (3) optimize the first strategy by keeping the
 * files open for some time.
 * <p>
 * Note: Currently, the <code>PrintWriter</code> used to log the message is
 * flushed after each log message written. Future development should probably
 * implement better buffering in conjunction with the temporary open/close
 * cycles of the files.
 */
class FileRequestLog implements RequestLog {

    // The file representing the root directory for relative log file paths
    private static File relPathRoot;

    // The map of shared open files (actually PrintWriter instances)
    private static Map<String, PrintWriter> logFiles = new HashMap<String, PrintWriter>();

    // Initialize class with the root directory for relative log file paths
    static void init(String relPathRoot) {
        FileRequestLog.relPathRoot = new File(relPathRoot).getAbsoluteFile();
    }

    // Dispose class by closing all open PrintWeiter instances
    static void dispose() {
        for (final Writer w : logFiles.values()) {
            try {
                w.close();
            } catch (IOException ioe) {
                // don't care
            }
        }
        logFiles.clear();
    }

    // The PrintWriter used by this instance to write the messages
    private PrintWriter output;

    FileRequestLog(String fileName) throws IOException {
        // ensure the path is absolute
        File file = new File(fileName);
        if (!file.isAbsolute()) {
            file = new File(relPathRoot, fileName);
        }

        // get back the raw file name
        fileName = file.getAbsolutePath();

        synchronized (logFiles) {
            this.output = logFiles.get(fileName);
            if (this.output == null) {

                // ensure location of the log file
                file.getParentFile().mkdirs();

                FileWriter fw = new FileWriter(file, true);
                this.output = new PrintWriter(fw);
                logFiles.put(fileName, this.output);
            }
        }
    }

    /**
     * @see org.apache.sling.engine.RequestLog#write(java.lang.String)
     */
    public void write(String message) {
        // use a local copy of the reference to not encounter NPE when this
        // log happens to be closed asynchronously while at the same time not
        // requiring synchronization
        PrintWriter writer = this.output;
        if (writer != null) {
            synchronized (writer) {
                writer.println(message);
                writer.flush();
            }
        }
    }

    public void close() {
        // just drop the reference to the output
        this.output = null;
    }
}
