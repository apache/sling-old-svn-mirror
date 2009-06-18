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
package org.apache.sling.scripting.core.impl.helper;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import javax.servlet.ServletRequest;

class OnDemandReader extends Reader {

    private final ServletRequest request;

    private Reader delegatee;

    OnDemandReader(ServletRequest request) {
        this.request = request;
    }

    @Override
    public void close() throws IOException {
        if (delegatee != null) {
            delegatee.close();
        }
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        getReader().mark(readAheadLimit);
    }

    @Override
    public boolean markSupported() {
        return (delegatee != null) ? delegatee.markSupported() : false;
    }

    @Override
    public int read() throws IOException {
        return getReader().read();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return getReader().read(cbuf, off, len);
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return getReader().read(cbuf);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        return getReader().read(target);
    }

    @Override
    public boolean ready() throws IOException {
        return getReader().ready();
    }

    @Override
    public void reset() throws IOException {
        if (delegatee != null) {
            delegatee.reset();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        return getReader().skip(n);
    }

    private Reader getReader() throws IOException {
        if (delegatee == null) {
            delegatee = request.getReader();
        }
        return delegatee;
    }
}
