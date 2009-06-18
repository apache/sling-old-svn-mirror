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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * The <code>SlingLoggerWriter</code> abstract the output writing functionality
 * for the Sling Logging implementation. This class is able to write to log
 * files and manage log file rotation for these files. Alternatively this class
 * supports writing to the standard output if no log file name is configured.
 */
class SlingLoggerWriter extends Writer {

    private static final long FACTOR_KB = 1024;

    private static final long FACTOR_MB = 1024 * FACTOR_KB;

    private static final long FACTOR_GB = 1024 * FACTOR_MB;

    /**
     * The string to place at the end of a line. This is a platform specific
     * string set from the <code>line.separator</code> system property.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * The PID of the configuration from which this instance has been
     * configured.
     */
    private final String configurationPID;

    /**
     * The actual <code>Writer</code> to which this instance delegates any
     * message writes.
     */
    private Writer delegatee;

    /**
     * The object on which access to this instance are serialized in
     * multi-threading environments.
     */
    protected final Object lock = new Object();

    /**
     * The <code>java.io.File</code> of the log or <code>null</code> if this
     * instance writes to the standard output.
     */
    private File file;

    /**
     * The absolute path to the log file or <code>null</code> if this instance
     * writes to the standard output.
     */
    private String path;

    /**
     * The maximum size of the log file after which the current log file is
     * rolled. This setting is ignored if logging to standard output.
     */
    private long maxSize;

    /**
     * The maximum number of old rotated log files to keep. This setting is
     * ignored if logging to standard output.
     */
    private int maxNum;

    /**
     * Creates a new instance of this class to be configured from the given
     * <code>configurationPID</code>. This new instance is not ready until
     * the {@link #configure(String, int, String)} method is being called.
     */
    SlingLoggerWriter(String configurationPID) {
        this.configurationPID = configurationPID;
    }

    /**
     * (Re)configures this instance to log to the given file.
     * 
     * @param logFileName The name of the file to log to or <code>null</code>
     *            to log to the standard output.
     * @param fileNum The maximum number of old (rotated) files to keep. This is
     *            ignored if <code>logFileName</code> is <code>null</code>.
     * @param fileSize The maximum size of the log file before rotating it. This
     *            is ignored if <code>logFileName</code> is <code>null</code>.
     * @throws IOException May be thrown if the file indicated by
     *             <code>logFileName</code> cannot be opened for writing.
     */
    void configure(String logFileName, int fileNum, String fileSize)
            throws IOException {

        // lock this instance while reconfiguring it
        synchronized (lock) {

            // change the log file name (if reconfigured)
            if (logFileName == null || !logFileName.equals(path)) {

                // close the current file
                close();

                if (logFileName == null) {

                    this.path = null;
                    this.file = null;

                } else {

                    // make sure the file is absolute and derive the path from
                    // there
                    File file = new File(logFileName);
                    if (!file.isAbsolute()) {
                        file = file.getAbsoluteFile();
                    }

                    this.path = file.getAbsolutePath();
                    this.file = file;
                }

                setDelegatee(createWriter());

            } else {

                // make sure, everything is written
                flush();

            }

            // assign new rotation values
            this.maxNum = fileNum;
            this.maxSize = convertMaxSizeSpec(fileSize);

            // check whether the new values cause different rotation
            checkRotate();
        }
    }

    String getConfigurationPID() {
        return configurationPID;
    }

    String getPath() {
        return path;
    }

    long getMaxSize() {
        return maxSize;
    }

    int getMaxNum() {
        return maxNum;
    }

    // ---------- Writer Overwrite ---------------------------------------------

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                flush();

                delegatee.close();
                delegatee = null;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.flush();

                // check whether we have to rotate the log file
                checkRotate();
            }
        }

    }

    @Override
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.write(c);
            }
        }
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.write(cbuf);
            }
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.write(cbuf, off, len);
            }
        }
    }

    @Override
    public void write(String str) throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.write(str);
            }
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            if (delegatee != null) {
                delegatee.write(str, off, len);
            }
        }
    }

    public void writeln() throws IOException {
        synchronized (lock) {
            write(LINE_SEPARATOR);
            flush();
        }
    }

    // ---------- internal -----------------------------------------------------

    static long convertMaxSizeSpec(String maxSize) {
        long factor;
        int len = maxSize.length() - 1;

        maxSize = maxSize.toUpperCase();

        if (maxSize.endsWith("G")) {
            factor = FACTOR_GB;
        } else if (maxSize.endsWith("GB")) {
            factor = FACTOR_GB;
            len--;
        } else if (maxSize.endsWith("M")) {
            factor = FACTOR_MB;
        } else if (maxSize.endsWith("MB")) {
            factor = FACTOR_MB;
            len--;
        } else if (maxSize.endsWith("K")) {
            factor = FACTOR_KB;
        } else if (maxSize.endsWith("KB")) {
            factor = FACTOR_KB;
            len--;
        } else {
            factor = 1;
            len = -1;
        }

        if (len > 0) {
            maxSize = maxSize.substring(0, len);
        }

        try {
            return factor * Long.parseLong(maxSize);
        } catch (NumberFormatException nfe) {
            return 10 * 1024 * 1024;
        }
    }

    void setDelegatee(Writer delegatee) {
        synchronized (lock) {
            this.delegatee = delegatee;
        }
    }

    Writer getDelegatee() {
        synchronized (lock) {
            return delegatee;
        }
    }

    /**
     * Must be called while the lock is held !!
     */
    private void checkRotate() throws IOException {
        if (file != null && file.length() > maxSize) {

            getDelegatee().close();

            if (maxNum >= 0) {

                // remove oldest file
                File dstFile = new File(path + "." + maxNum);
                if (dstFile.exists()) {
                    dstFile.delete();
                }

                // rename next files
                for (int i = maxNum - 1; i >= 0; i--) {
                    File srcFile = new File(path + "." + i);
                    if (srcFile.exists()) {
                        srcFile.renameTo(dstFile);
                    }
                    dstFile = srcFile;
                }

                // rename youngest file
                file.renameTo(dstFile);

            } else {

                // just remove the old file if we don't keep backups
                file.delete();

            }

            // create new file
            setDelegatee(createWriter());
        }
    }

    private Writer createWriter() throws IOException {
        if (file == null) {
            return new OutputStreamWriter(System.out) {
                @Override
                public void close() {
                    // not really !!
                }
            };
        }

        // ensure parent path of the file to create
        file.getParentFile().mkdirs();

        // open the file in append mode to not overwrite an existing
        // log file from a previous instance running
        return new OutputStreamWriter(new FileOutputStream(file, true));
    }

}
