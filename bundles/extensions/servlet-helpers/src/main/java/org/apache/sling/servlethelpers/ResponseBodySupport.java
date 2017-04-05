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
package org.apache.sling.servlethelpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

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
                @Override
                public boolean isReady() {
                    return true;
                }
                @Override
                public void setWriteListener(WriteListener writeListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return servletOutputStream;
    }

    public PrintWriter getWriter(String charset) {
        if (printWriter == null) {
            try {
                printWriter = new PrintWriter(new OutputStreamWriter(getOutputStream(), defaultCharset(charset)));
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Unsupported encoding: " + defaultCharset(charset), ex);
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
            return new String(getOutput(), defaultCharset(charset));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unsupported encoding: " + defaultCharset(charset), ex);
        }
    }
    
    private String defaultCharset(String charset) {
        return StringUtils.defaultString(charset, CharEncoding.UTF_8);
    }

}
