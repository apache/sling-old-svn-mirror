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
import java.io.Writer;

import javax.servlet.ServletResponse;

class OnDemandWriter extends Writer {

    private final ServletResponse response;

    private Writer delegatee;

    OnDemandWriter(ServletResponse response) {
        this.response = response;
    }

    private Writer getWriter() throws IOException {
        if (delegatee == null) {
            delegatee = response.getWriter();
        }

        return delegatee;
    }

    @Override
    public void write(int c) throws IOException {
        synchronized (lock) {
            getWriter().write(c);
        }
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        synchronized (lock) {
            getWriter().write(cbuf);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            getWriter().write(cbuf, off, len);
        }
    }

    @Override
    public void write(String str) throws IOException {
        synchronized (lock) {
            getWriter().write(str);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            getWriter().write(str, off, len);
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            Writer writer = delegatee;
            if (writer != null) {
                writer.flush();
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            // flush and close the delegatee if existing, otherwise ignore
            Writer writer = delegatee;
            if (writer != null) {
                writer.flush();
                writer.close();

                // drop the delegatee now
                delegatee = null;
            }
        }
    }
}