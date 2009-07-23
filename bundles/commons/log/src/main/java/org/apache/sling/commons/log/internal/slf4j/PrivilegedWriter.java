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

import java.awt.image.ImagingOpException;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class PrivilegedWriter extends FilterWriter {

    PrivilegedWriter(Writer delegatee) {
        super(delegatee);
    }
    
    @Override
    public void close() throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    PrivilegedWriter.super.close();
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            rethrow(e.getCause());
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    PrivilegedWriter.super.flush();
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            rethrow(e.getCause());
        }
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws IOException {
                    PrivilegedWriter.super.write(cbuf, off, len);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            rethrow(e.getCause());
        }
    }

    private void rethrow(Throwable cause) throws IOException {
        if (cause instanceof IOException) {
            throw (IOException) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            throw (IOException) new IOException(cause.getMessage()).initCause(cause);
        }
    }
}
