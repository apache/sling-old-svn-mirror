/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.servlets.get.impl.helpers;


import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import javax.servlet.ServletOutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * The <code>HeadServletResponse</code> is a Sling response wrapper which
 * ensures that nothing will ever be written by return null writers or
 * output streams.
 */

public class HeadServletResponse extends
        SlingHttpServletResponseWrapper {

    private ServletOutputStream stream;

    private PrintWriter writer;

    public HeadServletResponse(SlingHttpServletResponse wrappedResponse) {
        super(wrappedResponse);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("Writer already obtained");
        }

        if (stream == null) {
            stream = new NullServletOutputStream();
        }

        return stream;
    }

    @Override
    public PrintWriter getWriter() {
        if (stream != null) {
            throw new IllegalStateException("OutputStream already obtained");
        }

        if (writer == null) {
            writer = new PrintWriter(new NullWriter());
        }

        return writer;
    }

    /**
     * The <code>NullServletOutputStream</code> is a
     * <code>ServletOutputStream</code> which simply does not write out
     * anything.
     *
     * @see HeadServletResponse#getOutputStream()
     */
    private static class NullServletOutputStream extends ServletOutputStream {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }

    /**
     * The <code>NullWriter</code> is a <code>Writer</code> which simply does
     * not write out anything.
     *
     * @see HeadServletResponse#getWriter()
     */
    private static class NullWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) {
        }

        @Override
        public void write(char[] cbuf) {
        }

        @Override
        public void write(int c) {
        }

        @Override
        public void write(String str) {
        }

        @Override
        public void write(String str, int off, int len) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}