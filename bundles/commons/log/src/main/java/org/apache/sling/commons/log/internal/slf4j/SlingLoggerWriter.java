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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Regular expression matching a maximum file size specification. This
     * pattern case-insensitively matches a number and an optional factor
     * specifier of the forms k, kb, m, mb, g, or gb.
     */
    private static final Pattern SIZE_SPEC = Pattern.compile(
        "([\\d]+)([kmg]b?)?", Pattern.CASE_INSENSITIVE);

    /**
     * The string to place at the end of a line. This is a platform specific
     * string set from the <code>line.separator</code> system property.
     */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * The PID of the configuration from which this instance has been
     * configured. If this is <code>null</code> this instance is an implicitly
     * created instance which is not tied to any configuration.
     */
    private String configurationPID;

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
	 * The {@link #writerLimitChecker} used to in the {@link #checkRotate()}
	 * method to check whether the log file must be rotated.
	 */
    private FileRotator fileRotator;

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
            if (this.file != null) {
                this.fileRotator = createFileRotator(fileNum, fileSize);
            } else {
                this.fileRotator = FileRotator.DEFAULT;
            }

            // check whether the new values cause different rotation
            checkRotate();
        }
    }

    /**
     * Returns the PID of the configuration configuring this instance. This may
     * be <code>null</code> if this is an implicitly defined log writer
     * instance.
     */
    String getConfigurationPID() {
        return configurationPID;
    }

    /**
     * Sets the PID of the configuration configuring this instance. This may be
     * <code>null</code> if the configuration is removed but the writer is still
     * referred to by any logger configuration.
     */
    void setConfigurationPID(String configurationPID) {
        this.configurationPID = configurationPID;
    }

    String getPath() {
        return path;
    }

    FileRotator getFileRotator() {
        return fileRotator;
    }

    File getFile() {
        return file;
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

    static FileRotator createFileRotator(int fileNum, String maxSizeSpec) {
        if (maxSizeSpec != null && maxSizeSpec.length() > 0) {
            Matcher sizeMatcher = SIZE_SPEC.matcher(maxSizeSpec);
            if (sizeMatcher.matches()) {
                // group 1 is the base size and is an integer number
                final long baseSize = Long.parseLong(sizeMatcher.group(1));

                // this will take the final size value
                final long maxSize;

                // group 2 is optional and is the size spec. If not null it is
                // at least one character long and the first character is enough
                // for use to know (the second is of no use here)
                final String factorString = sizeMatcher.group(2);
                if (factorString == null) {
                    // no factor define, hence no multiplication
                    maxSize = baseSize;
                } else {
                    switch (factorString.charAt(0)) {
                        case 'k':
                        case 'K':
                            maxSize = baseSize * FACTOR_KB;
                            break;
                        case 'm':
                        case 'M':
                            maxSize = baseSize * FACTOR_MB;
                            break;
                        case 'g':
                        case 'G':
                            maxSize = baseSize * FACTOR_GB;
                            break;
                        default:
                            // we don't really expect this according to the pattern
                            maxSize = baseSize;
                    }
                }

                // return a size limited rotator with desired number of generations
                return new SizeLimitedFileRotator(fileNum, maxSize);
            }
        }

        // no file size configuration, check for date form specification
        try {
            return new ScheduledFileRotator(maxSizeSpec);
        } catch (IllegalArgumentException iae) {
            // illegal SimpleDateFormatPattern, fall back to no rotation
            // TODO: log this somehow !!!
            return FileRotator.DEFAULT;
        }
    }

    private void setDelegatee(Writer delegatee) {
        synchronized (lock) {
            this.delegatee = delegatee;
        }
    }

    private Writer getDelegatee() {
        synchronized (lock) {
            return delegatee;
        }
    }

    void checkRotate() throws IOException {
        synchronized (lock) {
            if (fileRotator.isRotationDue(file)) {

                getDelegatee().close();

                fileRotator.rotate(file);

                // create new file
                setDelegatee(createWriter());
            }
        }
    }

    private Writer createWriter() throws IOException {
        if ( file != null ) {
            try {
                // ensure parent path of the file to create
                file.getParentFile().mkdirs();
        
                // open the file in append mode to not overwrite an existing
                // log file from a previous instance running
                return new OutputStreamWriter(new FileOutputStream(file, true));
            } catch ( FileNotFoundException e) {
                System.out.println("Unable to open "+file.getAbsolutePath()+" due to "+e.getMessage());
                System.out.println("Defaulting to stdout");
            } catch ( SecurityException e) {
                System.out.println("Unable to open "+file.getAbsolutePath()+" due to "+e.getMessage());
                System.out.println("Defaulting to stdout");
            }
            file = null;
            path = null;
        }
        
        return new OutputStreamWriter(System.out) {
            @Override
            public void close() {
                // not really !!
            }
        };        
    }
}
