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

/**
 * The <code>SizeLimitedFileRotator</code> is a {@link FileRotator} which
 * rotates the log file as soon as some configurable log file size has been
 * reached. Further configuration can specify the number of log file generations
 * to keep in case of rotation.
 * <p>
 * This file rotator implements the same functionality as the
 * <code>RollingFileAppender</code> of the Log4J framework.
 */
final class SizeLimitedFileRotator implements FileRotator {

    /**
     * The maximum index number of old rotated log files to keep. If this is
     * zero or negative no old generation log files are kept.
     */
    private final int maxIndex;

    /**
     * The maximum size of the log file after which the current log file is
     * rolled. This setting is ignored if logging to standard output.
     */
    private final long maxSize;

    /**
     * Creates a new instance of the size limited file rotator.
     *
     * @param maxNum The maximum number of generations to keep. If this is zero
     *            or negative, no old log files are kept. Otherwise maxNum old
     *            log files (besides the current log file) are kept.
     * @param maxSize The maximum size of the file after which it is to be
     *            rotated.
     */
    SizeLimitedFileRotator(int maxNum, long maxSize) {
        this.maxIndex = maxNum - 1;
        this.maxSize = maxSize;
    }

    /**
     * NOT PART OF THE API. Returns the maximum index number of old log file
     * generations. If this is negative, no log file generations are kept.
     */
    int getMaxIndex() {
        return maxIndex;
    }

    /**
     * NOT PART OF THE API. Returns the maximum size of the log file at which
     * the {@link #isRotationDue(File)} returns <code>true</code>.
     */
    long getMaxSize() {
        return maxSize;
    }

    /**
     * Returns <code>true</code> if the <code>file</code>'s size is larger than
     * the configured size of this rotator.
     */
    public boolean isRotationDue(File file) {
        return file.length() > maxSize;
    }

    /**
     * Rotates the given <code>file</code> removing the oldest generation log
     * file and aging all other generations. The current log file becomes
     * generation 0.
     */
    public void rotate(final File file) {
        if (maxIndex >= 0) {

            final String baseName = file.getAbsolutePath();

            // remove oldest file
            File dstFile = new File(baseName + "." + maxIndex);
            if (dstFile.exists()) {
                dstFile.delete();
            }

            // rename next files
            for (int i = maxIndex - 1; i >= 0; i--) {
                final File srcFile = new File(baseName + "." + i);
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
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": maxSize=" + getMaxSize()
            + ", generations=" + (getMaxIndex() + 1);
    }
}