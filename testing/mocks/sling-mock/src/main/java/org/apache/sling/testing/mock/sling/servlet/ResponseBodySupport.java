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
package org.apache.sling.testing.mock.sling.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;

/**
 * Manage response body content.
 */
class ResponseBodySupport {

    private ByteArrayOutputStream outputStream;
    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;

    public ResponseBodySupport() {
        reset();
    }

    public void reset() {
        outputStream = new ByteArrayOutputStream();
        servletOutputStream = null;
        printWriter = null;
    }

    public ServletOutputStream getOutputStream() {
        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    outputStream.write(b);
                }
            };
        }
        return servletOutputStream;
    }

    public PrintWriter getWriter(String charset) {
        if (printWriter == null) {
            try {
                printWriter = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Unsupported encoding: " + charset, ex);
            }
        }
        return printWriter;
    }

    public byte[] getOutput() {
        if (servletOutputStream != null) {
            try {
                servletOutputStream.flush();
            } catch (IOException ex) {
                // ignore
            }
        }
        return outputStream.toByteArray();
    }

    public String getOutputAsString(String charset) {
        if (printWriter != null) {
            printWriter.flush();
        }
        try {
            return new String(getOutput(), charset);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unsupported encoding: " + charset, ex);
        }
    }

}
