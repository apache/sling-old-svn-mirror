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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SlingLogWriter extends Writer {

    private String lineSeparator;

    private Writer delegatee;

    protected final Object lock = new Object();

    public SlingLogWriter() {
        delegatee = new OutputStreamWriter(System.out) {
            @Override
            public void close() {
                // not really !!
            }
        };

        lineSeparator = System.getProperty("line.separator");
    }

    protected void setDelegatee(Writer delegatee) {
        synchronized (lock) {
            this.delegatee = delegatee;
        }
    }

    protected Writer getDelegatee() {
        synchronized (lock) {
            return delegatee;
        }
    }

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
            write(lineSeparator);
            flush();
        }
    }
}
