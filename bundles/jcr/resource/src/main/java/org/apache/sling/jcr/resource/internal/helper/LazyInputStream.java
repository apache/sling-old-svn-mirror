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
package org.apache.sling.jcr.resource.internal.helper;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Lazily acquired InputStream which only accesses the JCR Value InputStream if
 * data is to be read from the stream.
 */
public class LazyInputStream extends InputStream {

    /** The JCR Value from which the input stream is requested on demand */
    private final Value value;

    /** The inputstream created on demand, null if not used */
    private InputStream delegatee;

    public LazyInputStream(Value value) {
        this.value = value;
    }

    /**
     * Closes the input stream if acquired otherwise does nothing.
     */
    @Override
    public void close() throws IOException {
        if (delegatee != null) {
            delegatee.close();
        }
    }

    @Override
    public int available() throws IOException {
        return getStream().available();
    }

    @Override
    public int read() throws IOException {
        return getStream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return getStream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return getStream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return getStream().skip(n);
    }

    @Override
    public boolean markSupported() {
        try {
            return getStream().markSupported();
        } catch (IOException ioe) {
            // ignore
        }
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
        try {
            getStream().mark(readlimit);
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        getStream().reset();
    }

    /** Actually retrieves the input stream from the underlying JCR Value */
    private InputStream getStream() throws IOException {
        if (delegatee == null) {
            try {
                delegatee = value.getBinary().getStream();
            } catch (RepositoryException re) {
                throw (IOException) new IOException(re.getMessage()).initCause(re);
            }
        }
        return delegatee;
    }

}
