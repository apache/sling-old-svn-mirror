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

import static java.io.File.createTempFile;
import static java.lang.Math.pow;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.allocateDirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * {@link OutputStream} implementation which writes into a {@code byte[]} until a certain {@link #fileThreshold} is
 * reached and then starts writing into a {@code File} beyond that.
 */
public class FileBackedMemoryOutputStream extends OutputStream {

    private int memorySize = -1;

    public enum MemoryUnit {

        BYTES(1),
        KILO_BYTES(1000),
        MEGA_BYTES((int) pow(10, 6)),
        GIGA_BYTES((int) pow(10, 9));

        private final int memoryFactor;

        private MemoryUnit(int memoryFactor) {
            this.memoryFactor = memoryFactor;
        }

    };

    private final ByteBuffer memory;

    private final File tempDirectory;

    private final String fileName;

    private final String fileExtension;

    private FileOutputStream out;

    private File file;

    public FileBackedMemoryOutputStream(int fileThreshold,
                                        MemoryUnit memoryUnit,
                                        boolean useOffHeapMemory,
                                        File tempDirectory,
                                        String fileName,
                                        String fileExtension) {
        if (fileThreshold < 0) {
            throw new IllegalArgumentException("Negative fileThreshold size has no semantic in this version.");
        }
        int threshold = fileThreshold * memoryUnit.memoryFactor;
        if (useOffHeapMemory) {
            memory = allocateDirect(threshold);
        } else {
            memory = allocate(threshold);
        }
        this.tempDirectory = tempDirectory;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    @Override
    public void write(int b) throws IOException {
        if (memory.hasRemaining()) {
            memory.put((byte) (b & 0xff));
        } else {
            if (out == null) {
                file = createTempFile(fileName, fileExtension, tempDirectory);
                out = new FileOutputStream(file);
            }

            out.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
    }

    // method added just for testing purposes
    protected File getFile() {
        return file;
    }

    public long size() {
        long size = memorySize > 0 ? memorySize : memory.position();
        if (file != null) {
            size += file.length();
        }
        return size;
    }

    public void clean() {
        memory.clear();
        memory.rewind();
        if (file != null) {
            file.delete();
        }
    }

    public InputStream openWrittenDataInputStream() throws IOException {
        memorySize = memory.position(); // save the memory position for size calculation as after flip() position's always 0
        memory.flip();
        return new ByteBufferBackedInputStream(memory, file);
    }

}
