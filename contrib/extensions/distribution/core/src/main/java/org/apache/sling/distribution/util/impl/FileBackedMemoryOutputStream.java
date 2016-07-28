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
package org.apache.sling.distribution.util.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.io.File.createTempFile;

/**
 * {@link OutputStream} implementation which writes into a {@code byte[]} until a certain {@link #fileThreshold} is
 * reached and then starts writing into a {@code File} beyond that.
 */
public class FileBackedMemoryOutputStream extends OutputStream {

    private final int fileThreshold;

    private final ByteArrayOutputStream memory;

    private final File tempDirectory;

    private final String fileName;

    private final String fileExtension;

    private FileOutputStream out;

    private File file;

    public FileBackedMemoryOutputStream(int fileThreshold, File tempDirectory, String fileName, String fileExtension) {
        this.fileThreshold = fileThreshold;
        this.memory = new ByteArrayOutputStream(fileThreshold);
        this.tempDirectory = tempDirectory;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    @Override
    public void write(int b) throws IOException {
        OutputStream current;
        if (memory.size() < fileThreshold) {
            current = memory;
        } else {
            if (out == null) {
                file = createTempFile(fileName, fileExtension, tempDirectory);
                out = new FileOutputStream(file);
                memory.writeTo(out);
            }

            current = out;
        }

        current.write(b);
    }

    @Override
    public void flush() throws IOException {
        memory.flush();
        if (out != null) {
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        memory.close();
        if (out != null) {
            out.close();
        }
    }

    // method added just for testing purposes
    protected File getFile() {
        return file;
    }

    public long size() {
        if (file != null) {
            return file.length();
        }
        return memory.size();
    }

    public void clean() {
        if (file != null) {
            file.delete();
        }
    }

    public InputStream openWrittenDataInputStream() throws IOException {
        if (file != null) {
            return new FileInputStream(file);
        }
        return new ByteArrayInputStream(memory.toByteArray());
    }

}
