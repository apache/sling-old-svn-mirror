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
package org.apache.sling.bgservlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/** Minimal HttpServletResponse for background processing */
public class BackgroundHttpServletResponse implements HttpServletResponse {

    private final ServletOutputStream stream;
    private final PrintWriter writer;

    static class ServletOutputStreamWrapper extends ServletOutputStream {

        private final OutputStream os;

        ServletOutputStreamWrapper(OutputStream os) {
            this.os = os;
        }

        @Override
        public void write(int b) throws IOException {
            os.write(b);
        }

        @Override
        public void close() throws IOException {
            os.close();
        }

        @Override
        public void flush() throws IOException {
            os.flush();
        }

    }

    public BackgroundHttpServletResponse(HttpServletResponse hsr, OutputStream os)
            throws IOException {
        stream = new ServletOutputStreamWrapper(os);
        writer = new PrintWriter(new OutputStreamWriter(stream));
    }

    public void cleanup() throws IOException {
        stream.flush();
        stream.close();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return stream;
    }

    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    public void addCookie(Cookie arg0) {
    }

    public void addDateHeader(String arg0, long arg1) {
    }

    public void addHeader(String arg0, String arg1) {
    }

    public void addIntHeader(String arg0, int arg1) {
    }

    public boolean containsHeader(String arg0) {
        return false;
    }

    public String encodeRedirectUrl(String arg0) {
        return null;
    }

    public String encodeRedirectURL(String arg0) {
        return null;
    }

    public String encodeUrl(String arg0) {
        return null;
    }

    public String encodeURL(String arg0) {
        return null;
    }

    public void sendError(int arg0, String arg1) throws IOException {
        // TODO
    }

    public void sendError(int arg0) throws IOException {
        // TODO
    }

    public void sendRedirect(String arg0) throws IOException {
        // TODO
    }

    public void setDateHeader(String arg0, long arg1) {
    }

    public void setHeader(String arg0, String arg1) {
    }

    public void setIntHeader(String arg0, int arg1) {
    }

    public void setStatus(int arg0, String arg1) {
        // TODO
    }

    public void setStatus(int arg0) {
        // TODO
    }

    public void flushBuffer() throws IOException {
        stream.flush();
    }

    public int getBufferSize() {
        return 0;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public String getContentType() {
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
    }

    public void resetBuffer() {
    }

    public void setBufferSize(int arg0) {
    }

    public void setCharacterEncoding(String arg0) {
    }

    public void setContentLength(int arg0) {
    }

    public void setContentType(String arg0) {
    }

    public void setLocale(Locale arg0) {
    }
}