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
package org.apache.sling.scripting.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Extends the HttpServletResponse to wrap the response and capture the results.
 */
public final class CaptureResponseWrapper extends HttpServletResponseWrapper {

    private boolean isBinaryResponse = false;

    private PrintWriter writer;

    private StringWriter stringWriter;

    /**
     * Construct a new CaptureResponseWrapper.
     *
     * @param response the response to wrap
     */
    public CaptureResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Returns true if the response is binary.
     *
     * @return true if the response is binary, false otherwise
     */
    public boolean isBinaryResponse() {
        return isBinaryResponse;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    @Override
    public void flushBuffer() throws IOException {
        if (isBinaryResponse()) {
            getResponse().getOutputStream().flush();
        } else {
            writer.flush();
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     *
     * @return the output stream from the response
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IOException("'getWriter()' has already been invoked for a character data response.");
        }
        isBinaryResponse = true;
        return getResponse().getOutputStream();
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     *
     * @return the writer
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }
        if (isBinaryResponse) {
            throw new IOException("'getOutputStream()' has already been invoked for a binary data response.");
        }
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        return writer;
    }

    /**
     *
     * @return the captured character response data
     * @throws IOException if no character response data captured
     */
    public String getCapturedCharacterResponse() throws IOException {
        if (stringWriter == null) {
            throw new IOException("no character response data captured");
        }
        writer.flush();
        return stringWriter.toString();
    }

}
