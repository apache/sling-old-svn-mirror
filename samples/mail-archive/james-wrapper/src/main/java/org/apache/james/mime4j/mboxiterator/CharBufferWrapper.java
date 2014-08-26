/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.mime4j.mboxiterator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Wraps a CharBuffer and exposes some convenience methods to easy parse with Mime4j.
 */
public class CharBufferWrapper {

    private final CharBuffer messageBuffer;

    public CharBufferWrapper(CharBuffer messageBuffer) {
        if (messageBuffer == null) {
            throw new IllegalStateException("The buffer is null");
        }
        this.messageBuffer = messageBuffer;
    }

    public InputStream asInputStream(Charset encoding) {
        return new ByteBufferInputStream(encoding.encode(messageBuffer));
    }

    public char[] asCharArray() {
        return messageBuffer.array();
    }

    @Override
    public String toString() {
        return messageBuffer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharBufferWrapper)) return false;

        CharBufferWrapper that = (CharBufferWrapper) o;

        if (!messageBuffer.equals(that.messageBuffer)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return messageBuffer.hashCode();
    }

    /**
     * Provide an InputStream view over a ByteBuffer.
     */
    private static class ByteBufferInputStream extends InputStream {

        private final ByteBuffer buf;

        private ByteBufferInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            buf.get(bytes, off, Math.min(len, buf.remaining()));
            return len;
        }

    }
}
