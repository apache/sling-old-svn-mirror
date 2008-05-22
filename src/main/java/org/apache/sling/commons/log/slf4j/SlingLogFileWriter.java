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
package org.apache.sling.commons.log.slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SlingLogFileWriter extends SlingLogWriter {

    private static final long FACTOR_KB = 1024;

    private static final long FACTOR_MB = 1024 * FACTOR_KB;

    private static final long FACTOR_GB = 1024 * FACTOR_MB;

    private final File file;

    private final String path;

    private final long maxSize;

    private final int maxNum;

    public SlingLogFileWriter(String logFileName, int fileNum, String fileSize)
            throws IOException {
        this(logFileName, fileNum, convertMaxSizeSpec(fileSize));
    }

    public SlingLogFileWriter(String logFileName, int fileNum, long fileSize)
            throws IOException {

        // make sure the file is absolute and derive the path from there
        File file = new File(logFileName);
        if (!file.isAbsolute()) {
            file = file.getAbsoluteFile();
        }

        this.path = file.getAbsolutePath();
        this.file = file;

        this.maxNum = fileNum;
        this.maxSize = fileSize;

        setDelegatee(createFile());
    }

    @Override
    public void flush() throws IOException {
        super.flush();

        checkRotate();
    }

    private Writer createFile() throws IOException {
        // ensure parent path of the file to create
        file.getParentFile().mkdirs();

        // open the file in append mode to not overwrite an existing
        // log file from a previous instance running
        return new OutputStreamWriter(new FileOutputStream(file, true));
    }

    private void checkRotate() throws IOException {
        if (file.length() > maxSize) {
            synchronized (lock) {

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
                setDelegatee(createFile());
            }
        }
    }

    public String getPath() {
        return path;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public static long convertMaxSizeSpec(String maxSize) {
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

    public int getMaxNum() {
        return maxNum;
    }

}
