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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Extends the HttpServletResponse to wrap the response and capture the results.
 */
public final class CaptureResponseWrapper extends HttpServletResponseWrapper {
    private final String encoding;
    private final ServletOutputStream ops;
    private boolean isBinaryResponse = false;
    private PrintWriter writer = null;

    /**
     * Construct a new CaptureResponseWrapper.
     *
     * @param response
     *            the response to wrap
     * @param ops
     *            the output stream to write to
     */
    public CaptureResponseWrapper(HttpServletResponse response,
                           ServletOutputStream ops) {
        super(response);
        this.encoding = response.getCharacterEncoding();
        this.ops = ops;
    }

    /**
     * Returns true if the response is binary.
     *
     * @return
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
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }
        if (isBinaryResponse) {
            throw new IOException("'getOutputStream()' has already been invoked for a binary data response.");
        }
        writer = new PrintWriter(new OutputStreamWriter(ops, encoding));
        return writer;
    }

}
