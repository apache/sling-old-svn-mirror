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

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class ByteBufferBackedInputStream extends InputStream {

    private final ByteBuffer memory;

    public ByteBufferBackedInputStream(ByteBuffer memory) {
        this.memory = memory;
    }

    public int read() throws IOException {
        if (!memory.hasRemaining()) {
            return -1;
        }
        return memory.get() & 0xFF;
    }

    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!memory.hasRemaining()) {
            return -1;
        }

        len = min(len, memory.remaining());
        memory.get(bytes, off, len);
        return len;
    }

}
